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
import com.google.auth.oauth2.AccessToken;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.AggregatedHttpMessage;
import com.linecorp.armeria.common.CommonPools;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.unsafe.ByteBufHttpData;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.curioswitch.curiostack.gcloud.core.util.AsyncRefreshingValue;

abstract class AbstractAccessTokenProvider implements AccessTokenProvider {

  private static final String TOKEN_PATH = "/oauth2/v4/token";

  private static final Duration EXPIRATION_SKEW = Duration.ofMinutes(5);

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .findAndRegisterModules();

  enum Type {
    ACCESS_TOKEN,
    ID_TOKEN
  }

  private final HttpClient googleApisClient;
  private final Clock clock;

  private final AsyncRefreshingValue<AccessToken> cachedAccessToken;
  private final AsyncRefreshingValue<AccessToken> cachedIdToken;

  AbstractAccessTokenProvider(HttpClient googleApisClient, Clock clock) {
    this.googleApisClient = googleApisClient;
    this.clock = clock;
    cachedAccessToken =
        new AsyncRefreshingValue<>(
            () -> this.refresh(Type.ACCESS_TOKEN),
            AbstractAccessTokenProvider::extractExpirationTime,
            CommonPools.workerGroup().next(),
            clock);
    cachedIdToken =
        new AsyncRefreshingValue<>(
            () -> this.refresh(Type.ID_TOKEN),
            AbstractAccessTokenProvider::extractExpirationTime,
            CommonPools.workerGroup().next(),
            clock);
  }

  Clock clock() {
    return clock;
  }

  abstract ByteBuf refreshRequestContent(Type type);

  @Override
  public CompletableFuture<String> getAccessToken() {
    return cachedAccessToken.get().thenApply(AccessToken::getTokenValue);
  }

  @Override
  public CompletableFuture<String> getGoogleIdToken() {
    return cachedIdToken.get().thenApply(AccessToken::getTokenValue);
  }

  protected CompletableFuture<AggregatedHttpMessage> fetchToken(Type type) {
    HttpData data = new ByteBufHttpData(refreshRequestContent(type), true);
    return googleApisClient
        .execute(
            HttpHeaders.of(HttpMethod.POST, TOKEN_PATH)
                .set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded"),
            data)
        .aggregate();
  }

  private CompletableFuture<AccessToken> refresh(Type type) {
    return fetchToken(type)
        .handle(
            (msg, t) -> {
              if (t != null) {
                throw new IllegalStateException("Failed to refresh GCP access token.", t);
              }
              final TokenResponse response;
              try {
                response = OBJECT_MAPPER.readValue(msg.content().array(), TokenResponse.class);
              } catch (IOException e) {
                throw new UncheckedIOException("Error parsing token refresh response.", e);
              }
              long expiresAtMilliseconds =
                  clock.millis() + TimeUnit.SECONDS.toMillis(response.expiresIn());
              return new AccessToken(
                  type == Type.ID_TOKEN ? response.idToken() : response.accessToken(),
                  new Date(expiresAtMilliseconds));
            });
  }

  private static Instant extractExpirationTime(AccessToken token) {
    return token.getExpirationTime().toInstant().minus(EXPIRATION_SKEW);
  }
}
