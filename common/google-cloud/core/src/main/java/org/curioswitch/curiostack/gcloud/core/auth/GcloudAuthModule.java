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
import com.google.auth.oauth2.GoogleCredentials;
import com.linecorp.armeria.client.ClientBuilder;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.logging.LoggingClientBuilder;
import com.linecorp.armeria.client.metric.MetricCollectingClient;
import com.linecorp.armeria.client.retry.RetryStrategy;
import com.linecorp.armeria.client.retry.RetryingHttpClient;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.metric.MeterIdPrefixFunction;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Module;
import dagger.Provides;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.inject.Qualifier;
import javax.inject.Singleton;

@Module
public abstract class GcloudAuthModule {

  @Qualifier
  @Retention(RetentionPolicy.CLASS)
  @interface GoogleAccounts {}

  @Provides
  @Singleton
  static GcloudAuthConfig config(Config config) {
    return ConfigBeanFactory.create(
            config.getConfig("gcloud.auth"), ModifiableGcloudAuthConfig.class)
        .toImmutable();
  }

  @Provides
  @Singleton
  static Credentials credentials(GcloudAuthConfig config) {
    final GoogleCredentials credentials;
    try {
      if (config.getServiceAccountBase64().isEmpty()) {
        credentials = GoogleCredentials.getApplicationDefault();
      } else {
        try (InputStream s =
            Base64.getDecoder()
                .wrap(
                    new ByteArrayInputStream(
                        config.getServiceAccountBase64().getBytes(StandardCharsets.UTF_8)))) {
          credentials = GoogleCredentials.fromStream(s);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read credentials.", e);
    }
    if (credentials.createScopedRequired() && !config.getCredentialScopes().isEmpty()) {
      return credentials.createScoped(config.getCredentialScopes());
    }
    return credentials;
  }

  @Provides
  @Singleton
  static AccessTokenProvider accessTokenProvider(
      AccessTokenProvider.Factory factory, Credentials credentials) {
    return factory.create(credentials);
  }

  @Provides
  @Singleton
  @GoogleAccounts
  static HttpClient googleAccountsClient() {
    return new ClientBuilder("none+https://accounts.google.com/")
        .decorator(
            HttpRequest.class,
            HttpResponse.class,
            MetricCollectingClient.newDecorator(MeterIdPrefixFunction.ofDefault("googleaccounts")))
        .decorator(HttpRequest.class, HttpResponse.class, new LoggingClientBuilder().newDecorator())
        .decorator(
            HttpRequest.class,
            HttpResponse.class,
            RetryingHttpClient.newDecorator(RetryStrategy.onServerErrorStatus()))
        .addHttpHeader(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded")
        .build(HttpClient.class);
  }

  private GcloudAuthModule() {}
}
