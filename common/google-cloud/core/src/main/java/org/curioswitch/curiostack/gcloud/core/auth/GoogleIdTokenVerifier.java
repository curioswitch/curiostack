/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GoogleIdTokenVerifier {

  private static final Duration ALLOWED_TIME_SKEW = Duration.ofMinutes(5);

  private final GooglePublicKeysManager publicKeysManager;
  private final Clock clock;

  @Inject
  public GoogleIdTokenVerifier(GooglePublicKeysManager publicKeysManager, Clock clock) {
    this.publicKeysManager = publicKeysManager;
    this.clock = clock;
  }

  public CompletableFuture<Boolean> verify(GoogleIdToken token) {
    Instant currentTime = clock.instant();
    if (currentTime.isAfter(
        Instant.ofEpochSecond(token.getPayload().getExpirationTimeSeconds())
            .plus(ALLOWED_TIME_SKEW))) {
      return completedFuture(false);
    }
    if (currentTime.isBefore(
        Instant.ofEpochMilli(token.getPayload().getIssuedAtTimeSeconds())
            .minus(ALLOWED_TIME_SKEW))) {
      return completedFuture(false);
    }
    return publicKeysManager
        .getKeys()
        .thenApply(
            keys -> {
              for (PublicKey key : keys) {
                try {
                  if (token.verifySignature(key)) {
                    return true;
                  }
                } catch (GeneralSecurityException e) {
                  throw new IllegalArgumentException("Could not verify signature.", e);
                }
              }
              return false;
            });
  }
}
