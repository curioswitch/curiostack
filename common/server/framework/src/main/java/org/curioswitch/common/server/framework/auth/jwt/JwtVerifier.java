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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.spotify.futures.CompletableFuturesExtra;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.CompletableFuture;
import org.curioswitch.common.server.framework.auth.jwt.JwtVerifier.Factory;

@AutoFactory(implementing = Factory.class)
public class JwtVerifier {

  public interface Factory {
    JwtVerifier create(Algorithm algorithm, String publicKeysUrl);
  }

  public enum Algorithm {
    RS256,
    RS384,
    RS512,
    ES256,
    ES384,
    ES512
  }

  private final Algorithm algorithm;
  private final PublicKeysManager publicKeysManager;

  public JwtVerifier(
      @Provided PublicKeysManager.Factory publicKeysManagerFactory,
      Algorithm algorithm,
      String publicKeysUrl) {
    this.algorithm = algorithm;
    publicKeysManager = publicKeysManagerFactory.create(publicKeysUrl);
  }

  public CompletableFuture<DecodedJWT> verify(String token) {
    final DecodedJWT unverifiedJwt;
    try {
      unverifiedJwt = JWT.decode(token);
    } catch (JWTVerificationException e) {
      return CompletableFuturesExtra.exceptionallyCompletedFuture(e);
    }
    return getAlgorithm(unverifiedJwt.getKeyId())
        .thenApply(
            alg -> {
              JWTVerifier verifier = JWT.require(alg).build();
              return verifier.verify(token);
            });
  }

  private CompletableFuture<com.auth0.jwt.algorithms.Algorithm> getAlgorithm(String keyId) {
    return publicKeysManager
        .getById(keyId)
        .thenApply(
            key -> {
              switch (algorithm) {
                case RS256:
                  return com.auth0.jwt.algorithms.Algorithm.RSA256((RSAPublicKey) key, null);
                case RS384:
                  return com.auth0.jwt.algorithms.Algorithm.RSA384((RSAPublicKey) key, null);
                case RS512:
                  return com.auth0.jwt.algorithms.Algorithm.RSA512((RSAPublicKey) key, null);
                case ES256:
                  return com.auth0.jwt.algorithms.Algorithm.ECDSA256((ECPublicKey) key, null);
                case ES384:
                  return com.auth0.jwt.algorithms.Algorithm.ECDSA384((ECPublicKey) key, null);
                case ES512:
                  return com.auth0.jwt.algorithms.Algorithm.ECDSA512((ECPublicKey) key, null);
              }
              throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
            });
  }
}
