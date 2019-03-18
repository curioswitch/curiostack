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
package org.curioswitch.curiostack.gcloud.core;

import com.linecorp.armeria.client.ClientBuilder;
import com.linecorp.armeria.client.ClientDecoration;
import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.ClientFactoryBuilder;
import com.linecorp.armeria.client.ClientOption;
import com.linecorp.armeria.client.Clients;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.logging.LoggingClientBuilder;
import com.linecorp.armeria.client.retry.RetryStrategy;
import com.linecorp.armeria.client.retry.RetryingHttpClient;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.internal.TransportType;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.BindsOptionalOf;
import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.DnsServerAddressStreamProviders;
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
  public static HttpClient googleApisClient(
      Optional<MeterRegistry> meterRegistry, GcloudConfig config) {
    ClientFactory factory =
        meterRegistry
            .map(
                registry -> {
                  ClientFactoryBuilder builder = new ClientFactoryBuilder().meterRegistry(registry);
                  if (config.getDisableEdns()) {
                    builder.addressResolverGroupFactory(
                        eventLoopGroup ->
                            new DnsAddressResolverGroup(
                                new DnsNameResolverBuilder()
                                    .channelType(TransportType.datagramChannelType(eventLoopGroup))
                                    .nameServerProvider(
                                        DnsServerAddressStreamProviders.platformDefault())
                                    .optResourceEnabled(false)));
                  }
                  return builder.build();
                })
            .orElse(ClientFactory.DEFAULT);
    return new ClientBuilder("none+https://www.googleapis.com/")
        .factory(factory)
        .decorator(HttpRequest.class, HttpResponse.class, new LoggingClientBuilder().newDecorator())
        .build(HttpClient.class);
  }

  @Provides
  @Singleton
  @RetryingGoogleApis
  public static HttpClient retryingGoogleApisClient(@GoogleApis HttpClient googleApisClient) {
    return Clients.newDerivedClient(
        googleApisClient,
        ClientOption.DECORATION.newValue(
            ClientDecoration.of(
                HttpRequest.class,
                HttpResponse.class,
                RetryingHttpClient.newDecorator(RetryStrategy.onServerErrorStatus()))));
  }

  private GcloudModule() {}
}
