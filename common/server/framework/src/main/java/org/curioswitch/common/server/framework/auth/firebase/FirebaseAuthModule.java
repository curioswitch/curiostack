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
package org.curioswitch.common.server.framework.auth.firebase;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Module;
import dagger.Provides;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import javax.inject.Singleton;

/** Components for initializing firebase authentication of requests. */
@Module
public class FirebaseAuthModule {

  @Provides
  @Singleton
  static FirebaseAuthConfig firebaseConfig(Config config) {
    return ConfigBeanFactory.create(
        config.getConfig("firebaseAuth"), ModifiableFirebaseAuthConfig.class);
  }

  @Provides
  @Singleton
  static FirebaseApp firebaseApp(FirebaseAuthConfig config) {
    final FirebaseOptions options;
    try {
      options =
          new FirebaseOptions.Builder()
              .setCredentials(
                  GoogleCredentials.fromStream(
                      new ByteArrayInputStream(
                          Base64.getDecoder().decode(config.getServiceAccountBase64()))))
              .setDatabaseUrl("https://" + config.getProjectId() + ".firebaseio.com")
              .build();
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read certificate.", e);
    }
    FirebaseApp.initializeApp(options);
    return checkNotNull(FirebaseApp.getInstance());
  }

  @Provides
  @Singleton
  static FirebaseAuth firebaseAuth(FirebaseApp app) {
    return FirebaseAuth.getInstance(app);
  }

  private FirebaseAuthModule() {}
}
