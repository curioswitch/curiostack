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

import com.google.auth.Credentials;
import com.google.auth.RequestMetadataCallback;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.linecorp.armeria.client.Client;
import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.SimpleDecoratingClient;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javax.inject.Inject;

/**
 * A {@link SimpleDecoratingClient} that annotates requests with Google authentication metadata.
 *
 * <p>TODO(choko): Move this to a shared location, other usages of Google Cloud can benefit from it.
 */
class GoogleCredentialsDecoratingClient extends SimpleDecoratingClient<HttpRequest, HttpResponse> {

  static class Factory {
    private final Credentials credentials;

    @Inject
    Factory(Credentials credentials) {
      this.credentials = credentials;
    }

    Function<Client<HttpRequest, HttpResponse>, GoogleCredentialsDecoratingClient> newDecorator() {
      return client -> new GoogleCredentialsDecoratingClient(client, credentials);
    }
  }

  private final Credentials credentials;
  private final Executor authExecutor;

  /** Creates a new instance that decorates the specified {@link Client}. */
  private GoogleCredentialsDecoratingClient(
      Client<HttpRequest, HttpResponse> delegate, Credentials credentials) {
    super(delegate);
    this.credentials = credentials;
    authExecutor =
        Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("auth-refresh-%d").setDaemon(true).build());
  }

  @Override
  public HttpResponse execute(ClientRequestContext ctx, HttpRequest req) {
    CompletableFuture<HttpResponse> resFuture = new CompletableFuture<>();
    credentials.getRequestMetadata(
        URI.create(req.path()),
        authExecutor,
        new RequestMetadataCallback() {
          @Override
          public void onSuccess(Map<String, List<String>> metadata) {
            metadata.forEach(
                (key, values) -> {
                  for (String value : values) {
                    req.headers().add(HttpHeaderNames.of(key), value);
                  }
                });
            try {
              ctx.contextAwareEventLoop()
                  .submit(() -> resFuture.complete(delegate().execute(ctx, req)));
            } catch (Exception e) {
              resFuture.completeExceptionally(e);
            }
          }

          @Override
          public void onFailure(Throwable t) {
            resFuture.completeExceptionally(t);
          }
        });
    return HttpResponse.from(resFuture);
  }
}
