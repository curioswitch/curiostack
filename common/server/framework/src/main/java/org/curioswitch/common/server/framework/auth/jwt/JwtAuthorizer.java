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

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.auth.Authorizer;
import com.linecorp.armeria.server.auth.OAuth2Token;
import io.netty.util.AttributeKey;
import java.util.concurrent.CompletionStage;
import org.curioswitch.common.server.framework.auth.jwt.JwtAuthorizer.Factory;
import org.curioswitch.common.server.framework.auth.jwt.JwtVerifier.Algorithm;

@AutoFactory(implementing = Factory.class)
public class JwtAuthorizer implements Authorizer<OAuth2Token> {

  public interface Factory {
    JwtAuthorizer create(Algorithm algorithm, String publicKeysUrl);
  }

  public static final AttributeKey<DecodedJWT> DECODED_JWT =
      AttributeKey.valueOf(JwtAuthorizer.class, "DECODED_JWT");

  public static final AttributeKey<String> RAW_JWT =
      AttributeKey.valueOf(JwtAuthorizer.class, "RAW_JWT");

  private final JwtVerifier verifier;

  public JwtAuthorizer(
      @Provided JwtVerifier.Factory verifier, Algorithm algorithm, String publicKeysUrl) {
    this.verifier = verifier.create(algorithm, publicKeysUrl);
  }

  @Override
  public CompletionStage<Boolean> authorize(ServiceRequestContext ctx, OAuth2Token data) {
    return verifier
        .verify(data.accessToken())
        .handle(
            (jwt, t) -> {
              if (t != null) {
                return false;
              }
              ctx.attr(DECODED_JWT).set(jwt);
              ctx.attr(RAW_JWT).set(data.accessToken());
              return true;
            });
  }
}
