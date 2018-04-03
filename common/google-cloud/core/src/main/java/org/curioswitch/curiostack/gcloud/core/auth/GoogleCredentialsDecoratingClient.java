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
package org.curioswitch.curiostack.gcloud.core.auth;

import com.linecorp.armeria.client.Client;
import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.SimpleDecoratingClient;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.util.Exceptions;
import java.util.function.Function;
import javax.inject.Inject;

/** A {@link SimpleDecoratingClient} that annotates requests with Google authentication metadata. */
public class GoogleCredentialsDecoratingClient
    extends SimpleDecoratingClient<HttpRequest, HttpResponse> {

  public static class Factory {
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    Factory(AccessTokenProvider accessTokenProvider) {
      this.accessTokenProvider = accessTokenProvider;
    }

    public Function<Client<HttpRequest, HttpResponse>, GoogleCredentialsDecoratingClient>
        newDecorator() {
      return client -> new GoogleCredentialsDecoratingClient(client, accessTokenProvider);
    }
  }

  private final AccessTokenProvider accessTokenProvider;

  /** Creates a new instance that decorates the specified {@link Client}. */
  private GoogleCredentialsDecoratingClient(
      Client<HttpRequest, HttpResponse> delegate, AccessTokenProvider accessTokenProvider) {
    super(delegate);
    this.accessTokenProvider = accessTokenProvider;
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public HttpResponse execute(ClientRequestContext ctx, HttpRequest req) throws Exception {
    if (ctx.hasAttr(ClientRequestContext.HTTP_HEADERS)
        && ctx.attr(ClientRequestContext.HTTP_HEADERS)
            .get()
            .contains(HttpHeaderNames.AUTHORIZATION)) {
      return delegate().execute(ctx, req);
    }
    return HttpResponse.from(
        accessTokenProvider
            .get()
            .thenApplyAsync(
                (token) -> {
                  req.headers()
                      .add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token.getTokenValue());
                  try {
                    return delegate().execute(ctx, req);
                  } catch (Exception e) {
                    return Exceptions.throwUnsafely(e);
                  }
                },
                ctx.contextAwareEventLoop()));
  }
}
