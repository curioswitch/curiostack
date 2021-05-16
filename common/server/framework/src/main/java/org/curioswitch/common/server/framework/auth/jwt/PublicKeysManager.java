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

package org.curioswitch.common.server.framework.auth.jwt;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.client.retry.RetryRule;
import com.linecorp.armeria.client.retry.RetryingClient;
import com.linecorp.armeria.common.CommonPools;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.curioswitch.common.server.framework.auth.jwt.PublicKeysManager.Factory;
import org.curioswitch.common.server.framework.crypto.KeyUtil;
import org.curioswitch.common.server.framework.immutables.CurioStyle;
import org.curioswitch.curiostack.gcloud.core.util.AsyncRefreshingValue;
import org.immutables.value.Value.Immutable;

@AutoFactory(implementing = Factory.class)
public class PublicKeysManager {

  public interface Factory {
    PublicKeysManager create(String publicKeysUrl);
  }

  private static final Duration EXPIRATION_SKEW = Duration.ofMinutes(5);

  private static final Splitter CACHE_CONTROL_SPLITTER = Splitter.on(',');

  private static final Pattern MAX_AGE_PATTERN = Pattern.compile("\\s*max-age\\s*=\\s*(\\d+)\\s*");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Clock clock;
  private final WebClient httpClient;
  private final String path;
  private final AsyncRefreshingValue<CachedPublicKeys> keysCache;

  @SuppressWarnings("ConstructorLeaksThis")
  public PublicKeysManager(@Provided Clock clock, String publicKeysUrl) {
    this.clock = clock;

    URI uri = URI.create(publicKeysUrl);
    path = uri.getPath();

    httpClient =
        WebClient.builder(uri.getScheme() + "://" + uri.getAuthority())
            .decorator(LoggingClient.builder().newDecorator())
            .decorator(RetryingClient.newDecorator(RetryRule.failsafe()))
            .build();
    keysCache =
        new AsyncRefreshingValue<>(
            this::refresh,
            CachedPublicKeys::expirationTime,
            CommonPools.workerGroup().next(),
            clock);
  }

  public CompletableFuture<PublicKey> getById(String id) {
    return keysCache.get().thenApply(cachedKeys -> cachedKeys.keys().get(id));
  }

  private CompletableFuture<CachedPublicKeys> refresh() {
    return httpClient
        .get(path)
        .aggregate()
        .handle(
            (msg, t) -> {
              if (t != null) {
                throw new IllegalStateException("Failed to refresh Google public keys.", t);
              }
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
                  clock.instant().plusSeconds(cacheTimeSecs).minus(EXPIRATION_SKEW);

              final JsonNode tree;
              try {
                tree = OBJECT_MAPPER.readTree(msg.content().array());
              } catch (IOException e) {
                throw new UncheckedIOException("Could not parse certificates.", e);
              }
              Map<String, PublicKey> keys =
                  Streams.stream(tree.fields())
                      .map(
                          entry -> {
                            byte[] publicKeyPem =
                                entry.getValue().textValue().getBytes(StandardCharsets.UTF_8);
                            PublicKey publicKey = KeyUtil.loadPublicKey(publicKeyPem);
                            return new SimpleImmutableEntry<>(entry.getKey(), publicKey);
                          })
                      .collect(toImmutableMap(Entry::getKey, Entry::getValue));
              return ImmutableCachedPublicKeys.builder()
                  .expirationTime(expirationTime)
                  .putAllKeys(keys)
                  .build();
            });
  }

  @Immutable
  @CurioStyle
  interface CachedPublicKeys {
    Instant expirationTime();

    Map<String, PublicKey> keys();
  }
}
