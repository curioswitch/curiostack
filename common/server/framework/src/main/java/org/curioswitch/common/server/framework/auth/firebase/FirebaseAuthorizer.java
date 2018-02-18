/**
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
package org.curioswitch.common.server.framework.auth.firebase;

import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.auth.Authorizer;
import com.linecorp.armeria.server.auth.OAuth2Token;
import io.netty.util.AttributeKey;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

public class FirebaseAuthorizer implements Authorizer<OAuth2Token> {

  public static final AttributeKey<FirebaseToken> FIREBASE_TOKEN =
      AttributeKey.valueOf(FirebaseAuthorizer.class, "FIREBASE_TOKEN");

  public static final AttributeKey<String> RAW_FIREBASE_TOKEN =
      AttributeKey.valueOf(FirebaseAuthorizer.class, "RAW_FIREBASE_TOKEN");

  private final FirebaseAuth firebaseAuth;
  private final FirebaseAuthConfig config;

  @Inject
  public FirebaseAuthorizer(FirebaseAuth firebaseAuth, FirebaseAuthConfig config) {
    this.firebaseAuth = firebaseAuth;
    this.config = config;
  }

  @Override
  public CompletionStage<Boolean> authorize(ServiceRequestContext ctx, OAuth2Token data) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    ApiFutures.addCallback(
        firebaseAuth.verifyIdTokenAsync(data.accessToken()),
        new ApiFutureCallback<>() {
          @Override
          public void onFailure(Throwable t) {
            result.complete(false);
          }

          @Override
          public void onSuccess(FirebaseToken token) {
            if (!token.isEmailVerified() && !config.isAllowUnverifiedEmail()) {
              result.complete(false);
              return;
            }
            if (!config.getAllowedGoogleDomains().isEmpty()) {
              @SuppressWarnings("unchecked")
              Map<String, Object> firebaseClaims =
                  (Map<String, Object>) token.getClaims().get("firebase");
              if (!firebaseClaims.get("sign_in_provider").equals("google.com")
                  || !config.getAllowedGoogleDomains().contains(getEmailDomain(token.getEmail()))) {
                result.complete(false);
                return;
              }
            }
            ctx.attr(FIREBASE_TOKEN).set(token);
            ctx.attr(RAW_FIREBASE_TOKEN).set(data.accessToken());
            result.complete(true);
          }
        });
    return result;
  }

  private static String getEmailDomain(String email) {
    return email.substring(email.indexOf('@') + 1);
  }
}
