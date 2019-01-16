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
package org.curioswitch.common.server.framework.auth.googleid;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.auth.Authorizer;
import com.linecorp.armeria.server.auth.OAuth2Token;
import java.io.IOException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.common.server.framework.auth.ssl.SslCommonNamesProvider;
import org.curioswitch.curiostack.gcloud.core.auth.GoogleIdTokenVerifier;

public class GoogleIdAuthorizer implements Authorizer<OAuth2Token> {

  public static class Factory {
    private final GoogleIdTokenVerifier verifier;

    @Inject
    public Factory(GoogleIdTokenVerifier verifier) {
      this.verifier = verifier;
    }

    public GoogleIdAuthorizer create(SslCommonNamesProvider commonNamesProvider) {
      return new GoogleIdAuthorizer(commonNamesProvider, verifier);
    }
  }

  private static final Logger logger = LogManager.getLogger();

  // TODO(choko): Clean up the common names provider instead of sharing like this.
  private final SslCommonNamesProvider commonNamesProvider;
  private final GoogleIdTokenVerifier verifier;

  private GoogleIdAuthorizer(
      SslCommonNamesProvider commonNamesProvider, GoogleIdTokenVerifier verifier) {
    this.commonNamesProvider = commonNamesProvider;
    this.verifier = verifier;
  }

  @Override
  public CompletionStage<Boolean> authorize(ServiceRequestContext ctx, OAuth2Token data) {
    final GoogleIdToken token;
    try {
      token = GoogleIdToken.parse(JacksonFactory.getDefaultInstance(), data.accessToken());
    } catch (IOException e) {
      logger.info("Could not parse id token {}", data.accessToken());
      return completedFuture(false);
    }
    return verifier
        .verify(token)
        .thenApply(
            result -> {
              if (!result) {
                logger.info("Invalid signature.");
                return false;
              }
              if (!commonNamesProvider.get().contains(token.getPayload().getEmail())) {
                logger.info("Rejecting client: {}", token.getPayload().getEmail());
                return false;
              }
              return true;
            });
  }
}
