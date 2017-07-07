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

package org.curioswitch.common.server.framework.armeria;

import com.linecorp.armeria.client.AllInOneClientFactory;
import com.linecorp.armeria.client.ClientBuilder;
import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.SessionOption;
import com.linecorp.armeria.client.SessionOptions;
import com.linecorp.armeria.client.metric.PrometheusMetricCollectingClient;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.prometheus.client.CollectorRegistry;
import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.common.server.framework.config.ServerConfig;
import org.curioswitch.common.server.framework.monitoring.RpcMetricLabels;
import org.curioswitch.common.server.framework.monitoring.RpcMetricLabels.RpcMetricLabel;

/**
 * A convenience factory that sets up a {@link ClientBuilder} with appropriate default parameters.
 * Currently only sets up the client's SSL context but in the future will set up monitoring, etc.
 */
@Singleton
public class ClientBuilderFactory {

  private static final Logger logger = LogManager.getLogger();

  private final ClientFactory clientFactory;
  private final Set<RpcMetricLabel> labels;
  private final CollectorRegistry collectorRegistry;

  @Inject
  public ClientBuilderFactory(
      CollectorRegistry collectorRegistry,
      Set<RpcMetricLabel> labels,
      Optional<SelfSignedCertificate> selfSignedCertificate,
      Optional<TrustManagerFactory> caTrustManager,
      ServerConfig serverConfig) {
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
                  new File(serverConfig.getTlsCertificatePath()),
                  new File(serverConfig.getTlsPrivateKeyPath()));
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
        new AllInOneClientFactory(
            SessionOptions.of(SessionOption.SSL_CONTEXT_CUSTOMIZER.newValue(clientTlsCustomizer)),
            true);
    this.labels = labels;
    this.collectorRegistry = collectorRegistry;
  }

  public ClientBuilder create(String url) {
    return new ClientBuilder(url)
        .factory(clientFactory)
        .decorator(
            HttpRequest.class,
            HttpResponse.class,
            PrometheusMetricCollectingClient.newDecorator(
                collectorRegistry, labels, RpcMetricLabels.grpcRequestLabeler()));
  }
}
