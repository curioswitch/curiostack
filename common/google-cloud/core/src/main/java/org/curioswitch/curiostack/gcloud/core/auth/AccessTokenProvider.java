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

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.linecorp.armeria.client.HttpClient;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import org.curioswitch.curiostack.gcloud.core.auth.GoogleAuthModule.GoogleAccounts;

public interface AccessTokenProvider {

  class Factory {
    private final HttpClient googleAccountsClient;
    private final Clock clock;

    @Inject
    public Factory(@GoogleAccounts HttpClient googleAccountsClient, Clock clock) {
      this.googleAccountsClient = googleAccountsClient;
      this.clock = clock;
    }

    public AccessTokenProvider create(Credentials credentials) {
      if (credentials instanceof UserCredentials) {
        return new UserCredentialsAccessTokenProvider(
            googleAccountsClient, clock, (UserCredentials) credentials);
      } else if (credentials instanceof ServiceAccountCredentials) {
        return new ServiceAccountAccessTokenProvider(
            googleAccountsClient, clock, (ServiceAccountCredentials) credentials);
      }
      throw new IllegalArgumentException("Unsupported credentials type: " + credentials);
    }
  }

  CompletableFuture<AccessToken> get();
}
