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

package org.curioswitch.common.server.framework;

import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.common.http.HttpSessionProtocols;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.docs.DocServiceBuilder;
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder;
import com.linecorp.armeria.server.http.healthcheck.HttpHealthCheckService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.Multibinds;
import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.File;
import java.security.cert.CertificateException;
import java.util.Set;
import javax.net.ssl.SSLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.common.server.framework.ServerModule.InternalMultibindsModule;
import org.curioswitch.common.server.framework.config.ModifiableServerConfig;
import org.curioswitch.common.server.framework.config.ServerConfig;
import org.curioswitch.common.server.framework.monitoring.MetricsHttpService;
import org.curioswitch.common.server.framework.monitoring.MonitoringModule;
import org.curioswitch.common.server.framework.staticsite.StaticSiteService;
import org.curioswitch.common.server.framework.staticsite.StaticSiteServiceDefinition;

/**
 * A {@link Module} which bootstraps a server, finding and registering GRPC services to expose. All
 * servers should include this {@link Module} from an application-specific {@link Module} that binds
 * services to be exposed to {@link BindableService} and add it to a {@link dagger.Component} which
 * returns the initialized {@link Server}.
 *
 * <p>For example,
 *
 * <pre>{@code
 * {@literal @}Module(includes = ServerModule.class)
 * abstract class MyAppServerModule {
 *   {@literal @}Bind @IntoSet abstract BindableService myAppService(AppService service);
 * }
 *
 * {@literal @}Component(modules = MyAppServerModule.class)
 * interface MyAppComponent {
 *   Server server();
 * }
 * }</pre>
 */
@Module(
  includes = {ApplicationModule.class, MonitoringModule.class, InternalMultibindsModule.class}
)
public class ServerModule {

  private static final Logger logger = LogManager.getLogger();

  /**
   * An abstract {@link Module} to declare {@link Multibinds} to allow empty {@link Set}s. Not for
   * use outside this class.
   */
  @Module
  public abstract static class InternalMultibindsModule {

    @Multibinds
    abstract Set<BindableService> grpcServices();

    @Multibinds
    abstract Set<StaticSiteServiceDefinition> staticSites();
  }

  @Provides
  ServerConfig serverConfig(Config config) {
    return ConfigBeanFactory.create(config.getConfig("server"), ModifiableServerConfig.class)
        .toImmutable();
  }

  @Provides
  Server armeriaServer(
      Set<BindableService> grpcServices,
      Set<StaticSiteServiceDefinition> staticSites,
      MetricsHttpService metricsHttpService,
      ServerConfig serverConfig) {
    ServerBuilder sb = new ServerBuilder().port(8080, HttpSessionProtocols.HTTPS);

    if (serverConfig.isGenerateSelfSignedCertificate()) {
      logger.warn("Generating self-signed certificate. This should only happen on local!!!");
      try {
        SelfSignedCertificate certificate = new SelfSignedCertificate();
        sb.sslContext(
            HttpSessionProtocols.HTTPS, certificate.certificate(), certificate.privateKey());
      } catch (CertificateException | SSLException e) {
        // Can't happen.
        throw new IllegalStateException(e);
      }
    } else if (serverConfig.getTlsCertificatePath().isEmpty()
        || serverConfig.getTlsPrivateKeyPath().isEmpty()) {
      throw new IllegalStateException(
          "No TLS configuration provided, Curiostack does not support non-TLS servers. "
              + "Use gradle-curio-cluster-plugin to set up a namespace and TLS.");
    } else {
      try {
        sb.sslContext(
            HttpSessionProtocols.HTTPS,
            new File(serverConfig.getTlsCertificatePath()),
            new File(serverConfig.getTlsPrivateKeyPath()));
      } catch (SSLException e) {
        throw new IllegalStateException("Could not load TLS certificate.", e);
      }
    }

    if (!serverConfig.isDisableDocService()) {
      sb.serviceUnder("/internal/docs", new DocServiceBuilder().build());
    }
    sb.serviceAt("/internal/health", new HttpHealthCheckService());
    sb.serviceAt("/internal/metrics", metricsHttpService);

    if (!grpcServices.isEmpty()) {
      GrpcServiceBuilder serviceBuilder =
          new GrpcServiceBuilder()
              .supportedSerializationFormats(GrpcSerializationFormats.values())
              .enableUnframedRequests(true);
      grpcServices.forEach(serviceBuilder::addService);
      if (!serverConfig.isDisableGrpcServiceDiscovery()) {
        serviceBuilder.addService(ProtoReflectionService.newInstance());
      }
      sb.serviceUnder("/api", serviceBuilder.build());
    }

    for (StaticSiteServiceDefinition staticSite : staticSites) {
      sb.serviceUnder(
          staticSite.urlRoot(),
          StaticSiteService.of(staticSite.staticPath(), staticSite.classpathRoot()));
    }

    Server server = sb.build();
    server
        .start()
        .whenComplete(
            (unused, t) -> {
              if (t != null) {
                logger.error("Error starting server.", t);
              } else {
                logger.info("Server started on ports: " + server.activePorts());
              }
            });
    return server;
  }
}
