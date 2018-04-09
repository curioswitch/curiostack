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
/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.curioswitch.common.server.framework.auth.googleid;

import static java.util.Objects.requireNonNull;

import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.Service;
import com.linecorp.armeria.server.auth.AuthTokenExtractors;
import com.linecorp.armeria.server.auth.Authorizer;
import com.linecorp.armeria.server.auth.HttpAuthService;
import com.linecorp.armeria.server.auth.OAuth2Token;
import io.netty.util.AsciiString;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Temporary builder with forked code to allow authorizing custom headers. */
public class GoogleIdAuthServiceBuilder {
  private final List<Authorizer<HttpRequest>> authorizers = new ArrayList<>();

  /** Adds an {@link Authorizer}. */
  public GoogleIdAuthServiceBuilder add(Authorizer<HttpRequest> authorizer) {
    authorizers.add(requireNonNull(authorizer, "authorizer"));
    return this;
  }

  /** Adds an OAuth2 {@link Authorizer}. */
  public GoogleIdAuthServiceBuilder addOAuth2(Authorizer<? super OAuth2Token> authorizer) {
    return addTokenAuthorizer(AuthTokenExtractors.OAUTH2, requireNonNull(authorizer, "authorizer"));
  }

  /** Adds an OAuth2 {@link Authorizer} for the given {@code header}. */
  public GoogleIdAuthServiceBuilder addOAuth2(
      Authorizer<? super OAuth2Token> authorizer, AsciiString header) {
    return addTokenAuthorizer(
        new OAuth2TokenExtractor(requireNonNull(header, "header")),
        requireNonNull(authorizer, "authorizer"));
  }

  /** Adds a token-based {@link Authorizer}. */
  public <T> GoogleIdAuthServiceBuilder addTokenAuthorizer(
      Function<HttpHeaders, T> tokenExtractor, Authorizer<? super T> authorizer) {
    requireNonNull(tokenExtractor, "tokenExtractor");
    requireNonNull(authorizer, "authorizer");
    final Authorizer<HttpRequest> requestAuthorizer =
        (ctx, req) -> {
          T token = tokenExtractor.apply(req.headers());
          if (token == null) {
            return CompletableFuture.completedFuture(false);
          }
          return authorizer.authorize(ctx, token);
        };
    authorizers.add(requestAuthorizer);
    return this;
  }

  /**
   * Returns a newly-created decorator that decorates a {@link Service} with a new {@link
   * HttpAuthService} based on the {@link Authorizer}s added to this builder.
   */
  public Function<Service<HttpRequest, HttpResponse>, HttpAuthService> newDecorator() {
    return HttpAuthService.newDecorator(authorizers);
  }
}
