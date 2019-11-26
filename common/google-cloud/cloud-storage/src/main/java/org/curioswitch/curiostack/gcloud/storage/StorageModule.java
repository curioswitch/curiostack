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
package org.curioswitch.curiostack.gcloud.storage;

import com.linecorp.armeria.client.ClientDecoration;
import com.linecorp.armeria.client.ClientOption;
import com.linecorp.armeria.client.Clients;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.metric.MetricCollectingClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Module;
import dagger.Provides;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import org.curioswitch.curiostack.gcloud.core.auth.RetryingAuthenticatedGoogleApis;

/** A {@link Module} to setup a {@link StorageClient}. */
@Module
public abstract class StorageModule {

  @Qualifier
  @interface ForStorage {}

  @Provides
  @Singleton
  static StorageConfig config(Config config) {
    return ConfigBeanFactory.create(config.getConfig("storage"), ModifiableStorageConfig.class)
        .toImmutable();
  }

  @Provides
  @ForStorage
  @Singleton
  static WebClient metricClient(@RetryingAuthenticatedGoogleApis WebClient httpClient) {
    return Clients.newDerivedClient(
        httpClient,
        ClientOption.DECORATION.newValue(
            ClientDecoration.of(
                MetricCollectingClient.newDecorator(MetricLabels.storageRequestLabeler()))));
  }

  private StorageModule() {}
}
