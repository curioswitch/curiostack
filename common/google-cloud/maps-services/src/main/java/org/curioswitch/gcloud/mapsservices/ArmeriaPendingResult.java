/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

package org.curioswitch.gcloud.mapsservices;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.maps.ImageResult;
import com.google.maps.PendingResult;
import com.google.maps.errors.ApiException;
import com.google.maps.internal.ApiResponse;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.HttpStatusClass;
import java.io.IOException;
import javax.annotation.Nullable;

class ArmeriaPendingResult<T, R extends ApiResponse<T>> implements PendingResult<T> {

  private final HttpClient client;
  private final HttpRequest request;
  private final Class<R> responseClass;
  private final Gson gson;

  ArmeriaPendingResult(HttpClient client, HttpRequest request, Class<R> responseClass, Gson gson) {
    this.client = client;
    this.request = request;
    this.responseClass = responseClass;
    this.gson = gson;
  }

  @Override
  public void setCallback(Callback<T> callback) {
    client
        .execute(request)
        .aggregate()
        .handle(
            (msg, t) -> {
              if (t != null) {
                callback.onFailure(t);
              } else {
                try {
                  callback.onResult(parseResponse(msg));
                } catch (Throwable parseError) {
                  callback.onFailure(parseError);
                }
              }
              return null;
            });
  }

  @Override
  public T await() throws ApiException, InterruptedException, IOException {
    return parseResponse(client.execute(request).aggregate().join());
  }

  @Override
  @Nullable
  public T awaitIgnoreError() {
    try {
      return await();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public void cancel() {}

  private T parseResponse(AggregatedHttpResponse message) throws ApiException, IOException {
    HttpStatus status = message.status();

    String contentType = message.headers().get(HttpHeaderNames.CONTENT_TYPE);
    if (contentType != null
        && contentType.startsWith("image")
        && responseClass == ImageResult.Response.class
        && status.equals(HttpStatus.OK)) {
      var result = new ImageResult(contentType, message.content().array());
      @SuppressWarnings("unchecked")
      T castResult = (T) result;
      return castResult;
    }

    final R resp;
    try {
      resp = gson.fromJson(message.contentUtf8(), responseClass);
    } catch (JsonSyntaxException e) {
      if (!status.codeClass().equals(HttpStatusClass.SUCCESS)) {
        // Some of the APIs return 200 even when the API request fails, as long as the transport
        // mechanism succeeds. In these cases, INVALID_RESPONSE, etc are handled by the Gson
        // parsing.
        throw new IOException(
            String.format("Server Error: %d %s", status.code(), status.reasonPhrase()));
      }

      // Otherwise just cough up the syntax exception.
      throw e;
    }

    if (resp.successful()) {
      return resp.getResult();
    } else {
      // TODO(choko): Retry on retryable exceptions ideally without implementing retry business
      // logic itself.
      throw resp.getError();
    }
  }
}
