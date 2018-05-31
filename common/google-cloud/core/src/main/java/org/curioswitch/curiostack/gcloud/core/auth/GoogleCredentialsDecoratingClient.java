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
import io.netty.util.AsciiString;
import java.util.function.Function;
import javax.inject.Inject;

/** A {@link SimpleDecoratingClient} that annotates requests with Google authentication metadata. */
public class GoogleCredentialsDecoratingClient
    extends SimpleDecoratingClient<HttpRequest, HttpResponse> {

  public static class Factory {
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public Factory(AccessTokenProvider accessTokenProvider) {
      this.accessTokenProvider = accessTokenProvider;
    }

    public Function<Client<HttpRequest, HttpResponse>, GoogleCredentialsDecoratingClient>
        newAccessTokenDecorator() {
      return newAccessTokenDecorator(HttpHeaderNames.AUTHORIZATION);
    }

    public Function<Client<HttpRequest, HttpResponse>, GoogleCredentialsDecoratingClient>
        newAccessTokenDecorator(AsciiString header) {
      return client ->
          new GoogleCredentialsDecoratingClient(
              client, accessTokenProvider, TokenType.ACCESS_TOKEN, header);
    }

    public Function<Client<HttpRequest, HttpResponse>, GoogleCredentialsDecoratingClient>
        newIdTokenDecorator() {
      return newIdTokenDecorator(HttpHeaderNames.AUTHORIZATION);
    }

    public Function<Client<HttpRequest, HttpResponse>, GoogleCredentialsDecoratingClient>
        newIdTokenDecorator(AsciiString header) {
      return client ->
          new GoogleCredentialsDecoratingClient(
              client, accessTokenProvider, TokenType.ID_TOKEN, header);
    }
  }

  private enum TokenType {
    ACCESS_TOKEN,
    ID_TOKEN,
  }

  private final AccessTokenProvider accessTokenProvider;
  private final TokenType type;
  private final AsciiString header;

  /** Creates a new instance that decorates the specified {@link Client}. */
  private GoogleCredentialsDecoratingClient(
      Client<HttpRequest, HttpResponse> delegate,
      AccessTokenProvider accessTokenProvider,
      TokenType type,
      AsciiString header) {
    super(delegate);
    this.accessTokenProvider = accessTokenProvider;
    this.type = type;
    this.header = header;
  }

  @Override
  public HttpResponse execute(ClientRequestContext ctx, HttpRequest req) throws Exception {
    if (ctx.additionalRequestHeaders().contains(header) || req.headers().contains(header)) {
      return delegate().execute(ctx, req);
    }
    return HttpResponse.from(
        (type == TokenType.ACCESS_TOKEN
                ? accessTokenProvider.getAccessToken()
                : accessTokenProvider.getGoogleIdToken())
            .thenApplyAsync(
                (token) -> {
                  req.headers().add(header, "Bearer " + token);
                  try {
                    return delegate().execute(ctx, req);
                  } catch (Exception e) {
                    return Exceptions.throwUnsafely(e);
                  }
                },
                ctx.contextAwareEventLoop()));
  }
}
