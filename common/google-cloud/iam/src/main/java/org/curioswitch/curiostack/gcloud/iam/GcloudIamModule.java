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
package org.curioswitch.curiostack.gcloud.iam;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.client.retrofit2.ArmeriaRetrofitBuilder;
import dagger.Module;
import dagger.Provides;
import org.curioswitch.curiostack.gcloud.core.GcloudConfig;
import org.curioswitch.curiostack.gcloud.core.GcloudModule;
import org.curioswitch.curiostack.gcloud.core.auth.GoogleCredentialsDecoratingClient;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Module(includes = GcloudModule.class)
public class GcloudIamModule {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .findAndRegisterModules();

  @Provides
  static ServiceAccountsClient serviceAccountsClient(
      GcloudConfig config, GoogleCredentialsDecoratingClient.Factory credentialsDecorator) {
    return new ArmeriaRetrofitBuilder()
        .baseUrl("https://iam.googleapis.com/v1/projects/" + config.getProject() + "/")
        .addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER))
        .withClientOptions(
            (unused, options) ->
                options
                    .decorator(LoggingClient.builder().newDecorator())
                    .decorator(credentialsDecorator.newAccessTokenDecorator()))
        .build()
        .create(ServiceAccountsClient.class);
  }

  private GcloudIamModule() {}
}
