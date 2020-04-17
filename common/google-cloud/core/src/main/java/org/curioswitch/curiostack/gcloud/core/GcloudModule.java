/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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
package org.curioswitch.curiostack.gcloud.core;

import com.linecorp.armeria.client.ClientDecoration;
import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.ClientFactoryBuilder;
import com.linecorp.armeria.client.ClientOption;
import com.linecorp.armeria.client.Clients;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.client.retry.RetryStrategy;
import com.linecorp.armeria.client.retry.RetryingClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.BindsOptionalOf;
import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import javax.inject.Singleton;
import org.curioswitch.curiostack.gcloud.core.auth.GcloudAuthModule;

@Module(includes = GcloudAuthModule.class)
public abstract class GcloudModule {

  @BindsOptionalOf
  abstract MeterRegistry meterRegistry();

  @Provides
  @Singleton
  public static GcloudConfig config(Config config) {
    return ConfigBeanFactory.create(config.getConfig("gcloud"), ModifiableGcloudConfig.class)
        .toImmutable();
  }

  @Provides
  @Singleton
  @GoogleApis
  public static WebClient googleApisClient(
      Optional<MeterRegistry> meterRegistry, GcloudConfig config) {
    ClientFactory factory =
        meterRegistry
            .map(
                registry -> {
                  ClientFactoryBuilder builder = ClientFactory.builder().meterRegistry(registry);
                  if (config.getDisableEdns()) {
                    builder.domainNameResolverCustomizer(
                        dnsNameResolverBuilder -> dnsNameResolverBuilder.optResourceEnabled(false));
                  }
                  return builder.build();
                })
            .orElse(ClientFactory.ofDefault());
    return WebClient.builder("https://www.googleapis.com/")
        .factory(factory)
        .decorator(LoggingClient.builder().newDecorator())
        .build();
  }

  @Provides
  @Singleton
  @RetryingGoogleApis
  public static WebClient retryingGoogleApisClient(@GoogleApis WebClient googleApisClient) {
    return Clients.newDerivedClient(
        googleApisClient,
        ClientOption.DECORATION.newValue(
            ClientDecoration.of(RetryingClient.newDecorator(RetryStrategy.onServerErrorStatus()))));
  }

  private GcloudModule() {}
}
