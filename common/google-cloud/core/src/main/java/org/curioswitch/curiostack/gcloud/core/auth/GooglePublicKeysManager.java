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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.time.temporal.ChronoUnit.NANOS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.curioswitch.curiostack.gcloud.core.RetryingGoogleApis;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Singleton
public class GooglePublicKeysManager {

  private static final String CERTS_PATH = "/oauth2/v1/certs";

  private static final Duration REFRESH_SKEW = Duration.ofMinutes(5);

  private static final Splitter CACHE_CONTROL_SPLITTER = Splitter.on(',');

  private static final Pattern MAX_AGE_PATTERN = Pattern.compile("\\s*max-age\\s*=\\s*(\\d+)\\s*");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final CertificateFactory CERTIFICATE_FACTORY;

  static {
    try {
      CERTIFICATE_FACTORY = CertificateFactory.getInstance("X.509");
    } catch (CertificateException e) {
      throw new Error("Could not get certificate factory.", e);
    }
  }

  private enum CacheKey {
    INSTANCE
  }

  private final HttpClient googleApisClient;
  private final Clock clock;
  private final AsyncLoadingCache<CacheKey, CachedPublicKeys> keysCache;

  @Inject
  @SuppressWarnings("CanonicalDuration") // We want to use seconds like the HTTP response.
  public GooglePublicKeysManager(@RetryingGoogleApis HttpClient googleApisClient, Clock clock) {
    this.googleApisClient = googleApisClient;
    this.clock = clock;
    keysCache =
        Caffeine.newBuilder()
            .expireAfter(
                new Expiry<>() {
                  @Override
                  public long expireAfterCreate(
                      @Nonnull Object key, @Nonnull Object value, long currentTime) {
                    CachedPublicKeys keys = (CachedPublicKeys) value;
                    return currentTime + NANOS.between(clock.instant(), keys.expirationTime());
                  }

                  @Override
                  public long expireAfterUpdate(
                      @Nonnull Object key,
                      @Nonnull Object value,
                      long currentTime,
                      long currentDuration) {
                    return expireAfterCreate(key, value, currentTime);
                  }

                  @Override
                  public long expireAfterRead(
                      @Nonnull Object key,
                      @Nonnull Object value,
                      long currentTime,
                      long currentDuration) {
                    return currentDuration;
                  }
                })
            // Currently the max-age is 19560 sec, so optimistically refresh at 18000 (expiration
            // will
            // be 19260). If expiration happens to become sooner than refresh, it's fine it just
            // means
            // the keys will be refreshed on request instead of in the background.
            .refreshAfterWrite(Duration.ofSeconds(18000))
            .buildAsync((unused, executor) -> refresh());
  }

  public CompletableFuture<List<PublicKey>> getKeys() {
    return keysCache.get(CacheKey.INSTANCE).thenApply(CachedPublicKeys::keys);
  }

  private CompletableFuture<CachedPublicKeys> refresh() {
    return googleApisClient
        .get(CERTS_PATH)
        .aggregate()
        .thenApply(
            msg -> {
              if (!msg.status().equals(HttpStatus.OK)) {
                throw new IllegalStateException("Non-200 status code when fetching certificates.");
              }
              // Do the same simple header parsing as the upstream library.
              HttpHeaders headers = msg.headers();
              String cacheControl = headers.get(HttpHeaderNames.CACHE_CONTROL);
              long cacheTimeSecs = 0;
              if (cacheControl != null) {
                for (String arg : CACHE_CONTROL_SPLITTER.split(cacheControl)) {
                  Matcher m = MAX_AGE_PATTERN.matcher(arg);
                  if (m.matches()) {
                    cacheTimeSecs = Long.valueOf(m.group(1));
                    break;
                  }
                }
              }
              cacheTimeSecs -= headers.getInt(HttpHeaderNames.AGE, 0);
              cacheTimeSecs = Math.max(0, cacheTimeSecs);

              Instant expirationTime =
                  clock.instant().plusSeconds(cacheTimeSecs).minus(REFRESH_SKEW);

              final JsonNode tree;
              try {
                tree = OBJECT_MAPPER.readTree(msg.content().array());
              } catch (IOException e) {
                throw new UncheckedIOException("Could not parse certificates.", e);
              }
              List<PublicKey> keys =
                  Streams.stream(tree.elements())
                      .map(
                          valueNode -> {
                            try {
                              return (CERTIFICATE_FACTORY.generateCertificate(
                                      new ByteArrayInputStream(
                                          valueNode.textValue().getBytes(StandardCharsets.UTF_8))))
                                  .getPublicKey();
                            } catch (CertificateException e) {
                              throw new IllegalArgumentException(
                                  "Could not decode certificate.", e);
                            }
                          })
                      .collect(toImmutableList());
              return ImmutableCachedPublicKeys.builder()
                  .expirationTime(expirationTime)
                  .addAllKeys(keys)
                  .build();
            });
  }

  @Immutable
  @Style(
    deepImmutablesDetection = true,
    defaultAsDefault = true,
    builderVisibility = BuilderVisibility.PACKAGE,
    visibility = ImplementationVisibility.PACKAGE
  )
  interface CachedPublicKeys {
    Instant expirationTime();

    List<PublicKey> keys();
  }
}
