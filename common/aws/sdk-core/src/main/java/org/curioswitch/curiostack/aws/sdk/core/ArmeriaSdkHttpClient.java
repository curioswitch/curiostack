/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.curioswitch.curiostack.aws.sdk.core;

import com.linecorp.armeria.client.ClientOptions;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpObject;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.RequestHeadersBuilder;
import com.linecorp.armeria.common.ResponseHeaders;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.async.DelegatingSubscriber;

public class ArmeriaSdkHttpClient implements SdkAsyncHttpClient {

  private final WebClient client;

  ArmeriaSdkHttpClient(WebClient client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<Void> execute(AsyncExecuteRequest executeRequest) {
    SdkHttpRequest httpRequest = executeRequest.request();
    SdkAsyncHttpResponseHandler handler = executeRequest.responseHandler();

    RequestHeadersBuilder headersBuilder =
        RequestHeaders.builder(convert(httpRequest.method()), httpRequest.getUri().toString());
    executeRequest
        .requestContentPublisher()
        .contentLength()
        .ifPresent(
            contentLength ->
                headersBuilder.add(HttpHeaderNames.CONTENT_LENGTH, contentLength.toString()));
    for (Map.Entry<String, List<String>> header : httpRequest.headers().entrySet()) {
      headersBuilder.add(header.getKey(), header.getValue());
    }

    Publisher<HttpData> requestStream =
        delegate ->
            executeRequest
                .requestContentPublisher()
                .subscribe(new SdkToHttpDataSubscriber(delegate));
    HttpRequest request = HttpRequest.of(headersBuilder.build(), requestStream);

    HttpResponse response = client.execute(request);
    response.subscribe(new ResponseSubscriber(handler));

    CompletableFuture<Void> completionFuture = response.whenComplete();
    completionFuture.whenComplete(
        (unused, t) -> {
          if (t != null) {
            // Subscriber.onError, SdkAsyncHttpResponseHandler.onError, the returned future, and any
            // thrown exception are all ways of communicating errors to the SDK. This seems like two
            // too many but cover all the bases just in case.
            handler.onError(t);
          }
        });

    return completionFuture;
  }

  @Override
  public String clientName() {
    return "ArmeriaAsync";
  }

  @Override
  public void close() {}

  // TODO(choko): Implement
  public static class Builder implements SdkAsyncHttpClient.Builder<ArmeriaSdkHttpClient.Builder> {

    @Nullable private ClientOptions options;

    @Override
    public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
      return new ArmeriaSdkHttpClient(WebClient.builder().build());
    }
  }

  private static HttpMethod convert(SdkHttpMethod method) {
    switch (method) {
      case GET:
        return HttpMethod.GET;
      case POST:
        return HttpMethod.POST;
      case PUT:
        return HttpMethod.PUT;
      case DELETE:
        return HttpMethod.DELETE;
      case HEAD:
        return HttpMethod.HEAD;
      case PATCH:
        return HttpMethod.PATCH;
      case OPTIONS:
        return HttpMethod.OPTIONS;
      default:
        try {
          return HttpMethod.valueOf(method.name());
        } catch (IllegalArgumentException unused) {
          throw new IllegalArgumentException(
              "Unknown SdkHttpMethod: "
                  + method
                  + ". Cannot convert to an Armeria request. This could only practically happen if "
                  + "the HTTP standard has new methods added and is very unlikely.");
        }
    }
  }

  private static SdkHttpResponse convert(ResponseHeaders headers) {
    SdkHttpResponse.Builder builder =
        SdkHttpResponse.builder()
            .statusCode(headers.status().code())
            .statusText(headers.status().reasonPhrase());
    fillHeaders(headers, builder);
    return builder.build();
  }

  private static void fillHeaders(HttpHeaders headers, SdkHttpResponse.Builder builder) {
    headers.forEach((name, value) -> builder.appendHeader(name.toString(), value));
  }

  private static class SdkToHttpDataSubscriber extends DelegatingSubscriber<ByteBuffer, HttpData> {
    private SdkToHttpDataSubscriber(Subscriber<? super HttpData> delegate) {
      super(delegate);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
      subscriber.onNext(HttpData.wrap(Unpooled.wrappedBuffer(byteBuffer)));
    }
  }

  private static class ResponseSubscriber implements Subscriber<HttpObject>, Publisher<ByteBuffer> {

    private final SdkAsyncHttpResponseHandler handler;

    private boolean startedStream;

    @MonotonicNonNull private Subscription subscription;

    @MonotonicNonNull private Subscriber<? super ByteBuffer> sdkSubscriber;

    private ResponseSubscriber(SdkAsyncHttpResponseHandler handler) {
      this.handler = handler;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
      subscription.request(1);
    }

    @Override
    public void onNext(HttpObject obj) {
      if (!startedStream) {
        assert obj instanceof ResponseHeaders;
        startedStream = true;

        handler.onHeaders(convert((ResponseHeaders) obj));

        // We've only requested one object, the headers so far. No more objects will be signaled
        // until handler subscribes and requests more objects so we don't have to worry about
        // buffering.
        handler.onStream(this);
      } else {
        notifyObject(obj);
      }
    }

    @Override
    public void onError(Throwable t) {
      sdkSubscriber.onError(t);
    }

    @Override
    public void onComplete() {
      sdkSubscriber.onComplete();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
      this.sdkSubscriber = subscriber;
      // The SDK subscriber will request objects, controlling the stream from here.
      subscriber.onSubscribe(subscription);
    }

    private void notifyObject(HttpObject obj) {
      if (obj instanceof HttpData) {
        HttpData data = (HttpData) obj;
        // We can't subscribe with pooled objects since there is no SDK callback that would let us
        // release them so can just wrap the array here.
        sdkSubscriber.onNext(ByteBuffer.wrap(data.array()));
      } else {
        // Trailers. Documentation doesn't make clear whether the SDK actually can handle trailers
        // but it also doesn't say the callback can only be called once so just try calling it again
        // with the trailers.
        assert obj instanceof HttpHeaders;
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder();
        fillHeaders((HttpHeaders) obj, builder);
        handler.onHeaders(builder.build());
      }
    }
  }
}
