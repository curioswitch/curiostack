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

import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.stream.StreamMessage;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

class ArmeriaSdkHttpClientTest {

  @Mock private WebClient webClient;

  private ArmeriaSdkHttpClient client;

  @BeforeEach
  void setUp() {
    client = new ArmeriaSdkHttpClient(webClient);
  }

  @Test
  void converts_success() {
    StreamMessage<ByteBuffer> requestStream =
        StreamMessage.of(
            ByteBuffer.wrap("meow".getBytes(StandardCharsets.UTF_8)),
            ByteBuffer.wrap("woof".getBytes(StandardCharsets.UTF_8)));

    AtomicReference<SdkHttpResponse> responseHeaders = new AtomicReference<>();
    AtomicReference<Throwable> responseError = new AtomicReference<>();
    AtomicReference<Throwable> responseSubscriberError = new AtomicReference<>();
    AtomicBoolean responseComplete = new AtomicBoolean();
    List<ByteBuffer> responsePayloads = new ArrayList<>();

    AsyncExecuteRequest request =
        AsyncExecuteRequest.builder()
            .request(
                SdkHttpRequest.builder()
                    .protocol("https")
                    .method(SdkHttpMethod.POST)
                    .host("github.com")
                    .appendHeader("X-Animals", "cat")
                    .appendHeader("X-Animals", "dog")
                    .appendHeader("Content-Type", "application/json")
                    .encodedPath("/foo")
                    .appendRawQueryParameter("bar", "baz")
                    .build())
            .responseHandler(
                new SdkAsyncHttpResponseHandler() {
                  @Override
                  public void onHeaders(SdkHttpResponse headers) {
                    responseHeaders.set(headers);
                  }

                  @Override
                  public void onStream(Publisher<ByteBuffer> stream) {
                    stream.subscribe(
                        new Subscriber<ByteBuffer>() {
                          @Override
                          public void onSubscribe(Subscription s) {
                            s.request(Long.MAX_VALUE);
                          }

                          @Override
                          public void onNext(ByteBuffer byteBuffer) {
                            responsePayloads.add(byteBuffer);
                          }

                          @Override
                          public void onError(Throwable t) {
                            responseSubscriberError.set(t);
                          }

                          @Override
                          public void onComplete() {
                            responseComplete.set(true);
                          }
                        });
                  }

                  @Override
                  public void onError(Throwable error) {
                    responseError.set(error);
                  }
                })
            .requestContentPublisher(
                new SdkHttpContentPublisher() {
                  @Override
                  public Optional<Long> contentLength() {
                    return Optional.of((long) ("body1" + "body2").length());
                  }

                  @Override
                  public void subscribe(Subscriber<? super ByteBuffer> s) {
                    requestStream.subscribe(s);
                  }
                })
            .fullDuplex(false)
            .build();

    AtomicReference<AggregatedHttpRequest> aggregatedRequest = new AtomicReference<>();

    when(webClient.execute(any(HttpRequest.class)))
        .thenAnswer(
            (Answer<HttpResponse>)
                invocation -> {
                  HttpRequest httpRequest = invocation.getArgument(0);
                  aggregatedRequest.set(httpRequest.aggregate().join());
                  return HttpResponse.of(
                      ResponseHeaders.of(
                          HttpStatus.OK,
                          HttpHeaderNames.CONTENT_TYPE,
                          "application/json",
                          HttpHeaderNames.of("X-Countries"),
                          "Japan",
                          HttpHeaderNames.of("X-Countries"),
                          "US"),
                      HttpData.ofUtf8("purr"),
                      HttpData.ofUtf8("growl"));
                });

    CompletableFuture<Void> responseFuture = client.execute(request);

    assertThat(responseFuture.join()).isNull();
    assertThat(responseHeaders.get())
        .satisfies(
            headers -> {
              assertThat(headers.statusCode()).isEqualTo(HttpStatus.OK.code());
              assertThat(headers.statusText()).hasValue(HttpStatus.OK.reasonPhrase());
              assertThat(headers.headers())
                  .containsEntry("content-type", ImmutableList.of("application/json"))
                  .containsEntry("x-countries", ImmutableList.of("Japan", "US"));
            });
    assertThat(responsePayloads)
        .extracting(buf -> Unpooled.wrappedBuffer(buf).toString(StandardCharsets.UTF_8))
        .containsExactly("purr", "growl");
    assertThat(responseError.get()).isNull();
    assertThat(responseSubscriberError.get()).isNull();
  }

  @Test
  void clientName() {
    assertThat(client.clientName()).isEqualTo("ArmeriaAsync");
  }
}
