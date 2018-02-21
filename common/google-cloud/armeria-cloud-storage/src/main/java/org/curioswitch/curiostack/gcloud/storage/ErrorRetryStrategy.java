/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

package org.curioswitch.curiostack.gcloud.storage;

import com.linecorp.armeria.client.retry.Backoff;
import com.linecorp.armeria.client.retry.RetryStrategy;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.HttpStatusClass;
import com.linecorp.armeria.internal.HttpHeaderSubscriber;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

enum ErrorRetryStrategy implements RetryStrategy<HttpRequest, HttpResponse> {
  INSTANCE;

  private static final Optional<Backoff> DEFAULT_BACKOFF =
      Optional.of(RetryStrategy.defaultBackoff);

  @Override
  public CompletableFuture<Optional<Backoff>> shouldRetry(
      HttpRequest request, HttpResponse response) {
    final CompletableFuture<HttpHeaders> future = new CompletableFuture<>();
    final HttpHeaderSubscriber subscriber = new HttpHeaderSubscriber(future);
    response.completionFuture().whenComplete(subscriber);
    response.subscribe(subscriber);

    return future.handle(
        (headers, t) -> {
          if (t != null) {
            return DEFAULT_BACKOFF;
          }
          if (headers != null) {
            final HttpStatus resStatus = headers.status();
            if (resStatus.codeClass().equals(HttpStatusClass.SERVER_ERROR)) {
              return DEFAULT_BACKOFF;
            }
          }
          return Optional.empty();
        });
  }
}
