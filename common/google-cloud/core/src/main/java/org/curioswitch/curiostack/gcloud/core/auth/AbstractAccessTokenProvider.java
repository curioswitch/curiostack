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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.auth.oauth2.AccessToken;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.unsafe.ByteBufHttpData;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

abstract class AbstractAccessTokenProvider implements AccessTokenProvider {

  private static final long MINIMUM_TOKEN_MILLISECONDS = 60000L * 5L;

  private static final String TOKEN_PATH = "/o/oauth2/token";

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .findAndRegisterModules();

  private enum CacheKey {
    INSTANCE
  }

  private final HttpClient googleAccountsClient;
  private final Clock clock;
  private final AsyncLoadingCache<CacheKey, AccessToken> cachedAccessToken;

  AbstractAccessTokenProvider(HttpClient googleAccountsClient, Clock clock) {
    this.googleAccountsClient = googleAccountsClient;
    this.clock = clock;
    cachedAccessToken =
        Caffeine.newBuilder()
            // Google access tokens seem to last for an hour.
            .refreshAfterWrite(Duration.ofMinutes(30))
            .buildAsync((unused, executor) -> refresh());
  }

  Clock clock() {
    return clock;
  }

  abstract ByteBuf refreshRequestContent();

  @Override
  public CompletableFuture<AccessToken> get() {
    return cachedAccessToken
        .get(CacheKey.INSTANCE)
        .thenComposeAsync(
            token -> {
              if (token.getExpirationTime().getTime() - clock.millis()
                  <= MINIMUM_TOKEN_MILLISECONDS) {
                // Since we optimistically refresh, this should never happen but issue a manual
                // refresh
                // just in case.
                cachedAccessToken.synchronous().invalidate(CacheKey.INSTANCE);
                return cachedAccessToken.get(CacheKey.INSTANCE);
              }
              return CompletableFuture.completedFuture(token);
            });
  }

  private CompletableFuture<AccessToken> refresh() {
    HttpData data = new ByteBufHttpData(refreshRequestContent(), true);
    return googleAccountsClient
        .post(TOKEN_PATH, data)
        .aggregate()
        .thenApply(
            msg -> {
              final TokenResponse response;
              try {
                response = OBJECT_MAPPER.readValue(msg.content().array(), TokenResponse.class);
              } catch (IOException e) {
                throw new UncheckedIOException("Error parsing token refresh response.", e);
              }
              long expiresAtMilliseconds =
                  clock.millis() + TimeUnit.SECONDS.toMillis(response.expiresIn());
              return new AccessToken(response.accessToken(), new Date(expiresAtMilliseconds));
            });
  }
}
