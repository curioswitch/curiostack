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

package org.curioswitch.common.server.framework.gcloud;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Module;
import dagger.Provides;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.inject.Singleton;
import org.curioswitch.common.server.framework.config.GcloudConfig;
import org.curioswitch.common.server.framework.config.ModifiableGcloudConfig;

@Module
public abstract class GcloudModule {

  @Provides
  @Singleton
  static GcloudConfig config(Config config) {
    return ConfigBeanFactory.create(config.getConfig("gcloud"), ModifiableGcloudConfig.class)
        .toImmutable();
  }

  @Provides
  @Singleton
  static Credentials gcloudCredentials(GcloudConfig config) {
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
    if (credentials.createScopedRequired()) {
      return credentials.createScoped(config.getCredentialScopes());
    }
    return credentials;
  }

  private GcloudModule() {}
}
