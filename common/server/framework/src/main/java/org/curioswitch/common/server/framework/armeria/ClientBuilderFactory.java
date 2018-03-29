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
package org.curioswitch.common.server.framework.armeria;

import brave.Tracing;
import com.google.common.base.Strings;
import com.linecorp.armeria.client.ClientBuilder;
import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.ClientFactoryBuilder;
import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.endpoint.EndpointGroupRegistry;
import com.linecorp.armeria.client.endpoint.EndpointSelectionStrategy;
import com.linecorp.armeria.client.endpoint.dns.DnsAddressEndpointGroup;
import com.linecorp.armeria.client.endpoint.healthcheck.HttpHealthCheckedEndpointGroup;
import com.linecorp.armeria.client.endpoint.healthcheck.HttpHealthCheckedEndpointGroupBuilder;
import com.linecorp.armeria.client.metric.MetricCollectingClient;
import com.linecorp.armeria.client.tracing.HttpTracingClient;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.SessionProtocol;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.common.server.framework.config.ServerConfig;
import org.curioswitch.common.server.framework.monitoring.RpcMetricLabels;
import org.curioswitch.common.server.framework.util.ResourceUtil;

/**
 * A convenience factory that sets up a {@link ClientBuilder} with appropriate default parameters.
 * Currently only sets up the client's SSL context but in the future will set up monitoring, etc.
 */
@Singleton
public class ClientBuilderFactory {

  private static final Logger logger = LogManager.getLogger();

  private final ClientFactory clientFactory;
  private final Tracing tracing;
  private final MeterRegistry meterRegistry;

  @Inject
  public ClientBuilderFactory(
      MeterRegistry meterRegistry,
      Tracing tracing,
      Optional<SelfSignedCertificate> selfSignedCertificate,
      Optional<TrustManagerFactory> caTrustManager,
      ServerConfig serverConfig) {
    this.tracing = tracing;
    this.meterRegistry = meterRegistry;
    final TrustManagerFactory trustManagerFactory;
    if (serverConfig.isDisableClientCertificateVerification()) {
      logger.warn("Disabling client SSL verification. This should only happen on local!");
      trustManagerFactory = InsecureTrustManagerFactory.INSTANCE;
    } else if (caTrustManager.isPresent()) {
      trustManagerFactory = caTrustManager.get();
    } else {
      trustManagerFactory = null;
    }

    final Consumer<SslContextBuilder> clientCertificateCustomizer;
    if (selfSignedCertificate.isPresent()) {
      SelfSignedCertificate certificate = selfSignedCertificate.get();
      clientCertificateCustomizer =
          sslContext -> sslContext.keyManager(certificate.certificate(), certificate.privateKey());
    } else if (serverConfig.getTlsCertificatePath().isEmpty()
        || serverConfig.getTlsPrivateKeyPath().isEmpty()) {
      throw new IllegalStateException(
          "No TLS configuration provided, Curiostack does not support clients without TLS "
              + "certificates. Use gradle-curio-cluster-plugin to set up a namespace and TLS.");
    } else {
      clientCertificateCustomizer =
          sslContext ->
              sslContext.keyManager(
                  ResourceUtil.openStream(serverConfig.getTlsCertificatePath()),
                  ResourceUtil.openStream(serverConfig.getTlsPrivateKeyPath()));
    }

    final Consumer<SslContextBuilder> clientTlsCustomizer;
    if (trustManagerFactory != null) {
      clientTlsCustomizer =
          sslContext -> {
            clientCertificateCustomizer.accept(sslContext);
            sslContext.trustManager(trustManagerFactory);
          };
    } else {
      clientTlsCustomizer = clientCertificateCustomizer;
    }
    clientFactory =
        new ClientFactoryBuilder()
            .sslContextCustomizer(clientTlsCustomizer)
            .meterRegistry(meterRegistry)
            .build();
  }

  public ClientBuilder create(String name, String url) {
    URI uri = URI.create(url);
    DnsAddressEndpointGroup dnsEndpointGroup =
        DnsAddressEndpointGroup.of(uri.getHost(), uri.getPort());
    dnsEndpointGroup.addListener(
        endpoints ->
            logger.info(
                "Resolved new endpoints for {} : [ {} ]",
                name,
                endpoints.stream().map(Endpoint::authority).collect(Collectors.joining(","))));
    dnsEndpointGroup.start();
    HttpHealthCheckedEndpointGroup endpointGroup =
        new HttpHealthCheckedEndpointGroupBuilder(dnsEndpointGroup, "/internal/health")
            .clientFactory(clientFactory)
            .protocol(SessionProtocol.HTTPS)
            .retryInterval(Duration.ofSeconds(3))
            .build();
    EndpointGroupRegistry.register(name, endpointGroup, EndpointSelectionStrategy.ROUND_ROBIN);
    endpointGroup.newMeterBinder(name).bindTo(meterRegistry);

    return new ClientBuilder(
            uri.getScheme() + "://group:" + name + Strings.nullToEmpty(uri.getPath()))
        .factory(clientFactory)
        .decorator(
            HttpRequest.class,
            HttpResponse.class,
            MetricCollectingClient.newDecorator(RpcMetricLabels.grpcRequestLabeler("grpc_clients")))
        .decorator(HttpRequest.class, HttpResponse.class, HttpTracingClient.newDecorator(tracing));
  }
}
