/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.plugins.gcloud.buildcache;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.linecorp.armeria.client.HttpClient;
import java.io.IOException;
import java.time.Clock;
import org.curioswitch.curiostack.gcloud.core.GcloudModule;
import org.curioswitch.curiostack.gcloud.core.auth.AccessTokenProvider;
import org.curioswitch.curiostack.gcloud.core.auth.GcloudAuthModule;
import org.curioswitch.curiostack.gcloud.core.auth.GoogleCredentialsDecoratingClient;
import org.curioswitch.curiostack.gcloud.storage.StorageClient;
import org.curioswitch.curiostack.gcloud.storage.StorageConfig;
import org.gradle.caching.BuildCacheService;
import org.gradle.caching.BuildCacheServiceFactory;
import org.gradle.caching.internal.NoOpBuildCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudStorageBuildCacheServiceFactory
    implements BuildCacheServiceFactory<CloudStorageBuildCache> {

  private static final Logger logger =
      LoggerFactory.getLogger(CloudStorageBuildCacheServiceFactory.class);

  @Override
  public BuildCacheService createBuildCacheService(
      CloudStorageBuildCache buildCache, Describer describer) {
    checkNotNull(buildCache.getBucket(), "buildCache.bucket");

    describer.type("Google Cloud Storage Build Cache").config("bucket", buildCache.getBucket());

    final Credentials credentials;
    try {
      credentials = GoogleCredentials.getApplicationDefault();
    } catch (IOException e) {
      logger.warn(
          "Could not load Google credentials - did you run "
              + "./gradlew :gcloud_auth_application-default_login? Disabling build cache.");
      return new NoOpBuildCacheService();
    }
    HttpClient googleApis = GcloudModule.googleApisClient();
    AccessTokenProvider.Factory accessTokenProviderFactory =
        new AccessTokenProvider.Factory(googleApis, Clock.systemUTC());
    AccessTokenProvider accessTokenProvider = accessTokenProviderFactory.create(credentials);
    GoogleCredentialsDecoratingClient.Factory credentialsDecoratorFactory =
        new GoogleCredentialsDecoratingClient.Factory(accessTokenProvider);
    HttpClient authenticatedGoogleApis =
        GcloudAuthModule.authenticatedGoogleApisClient(googleApis, credentialsDecoratorFactory);

    StorageClient storageClient =
        new StorageClient(
            authenticatedGoogleApis,
            new StorageConfig.Builder().bucket(buildCache.getBucket()).build());
    return new CloudStorageBuildCacheService(storageClient);
  }
}
