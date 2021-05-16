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

package org.curioswitch.common.server.framework;

import static com.google.common.collect.ImmutableList.toImmutableList;

import brave.Tracing;
import brave.propagation.TraceContext;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.auth.OAuth2Token;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.HttpServiceWithRoutes;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServerListener;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.auth.AuthService;
import com.linecorp.armeria.server.auth.AuthServiceBuilder;
import com.linecorp.armeria.server.brave.BraveService;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.docs.DocServiceBuilder;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder;
import com.linecorp.armeria.server.healthcheck.HealthCheckService;
import com.linecorp.armeria.server.healthcheck.HealthChecker;
import com.linecorp.armeria.server.healthcheck.SettableHealthChecker;
import com.linecorp.armeria.server.logging.LoggingService;
import com.linecorp.armeria.server.metric.MetricCollectingService;
import com.linecorp.armeria.server.metric.PrometheusExpositionService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.BindsOptionalOf;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.Multibinds;
import dagger.producers.Production;
import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.prometheus.client.CollectorRegistry;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Singleton;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.common.server.framework.armeria.SslContextKeyConverter;
import org.curioswitch.common.server.framework.auth.firebase.FirebaseAuthConfig;
import org.curioswitch.common.server.framework.auth.firebase.FirebaseAuthModule;
import org.curioswitch.common.server.framework.auth.firebase.FirebaseAuthorizer;
import org.curioswitch.common.server.framework.auth.jwt.JwtAuthorizer;
import org.curioswitch.common.server.framework.auth.jwt.JwtModule;
import org.curioswitch.common.server.framework.auth.jwt.JwtVerifier.Algorithm;
import org.curioswitch.common.server.framework.auth.ssl.RpcAclsCommonNamesProvider;
import org.curioswitch.common.server.framework.auth.ssl.SslAuthorizer;
import org.curioswitch.common.server.framework.auth.ssl.SslCommonNamesProvider;
import org.curioswitch.common.server.framework.config.JavascriptStaticConfig;
import org.curioswitch.common.server.framework.config.ModifiableJavascriptStaticConfig;
import org.curioswitch.common.server.framework.config.ModifiableServerConfig;
import org.curioswitch.common.server.framework.config.MonitoringConfig;
import org.curioswitch.common.server.framework.config.SecurityConfig;
import org.curioswitch.common.server.framework.config.ServerConfig;
import org.curioswitch.common.server.framework.files.FileWatcher;
import org.curioswitch.common.server.framework.files.WatchedPath;
import org.curioswitch.common.server.framework.filter.IpFilteringService;
import org.curioswitch.common.server.framework.grpc.GrpcServiceDefinition;
import org.curioswitch.common.server.framework.inject.CloseOnStop;
import org.curioswitch.common.server.framework.inject.EagerInit;
import org.curioswitch.common.server.framework.logging.LoggingModule;
import org.curioswitch.common.server.framework.logging.RequestLoggingContext;
import org.curioswitch.common.server.framework.monitoring.MetricsHttpService;
import org.curioswitch.common.server.framework.monitoring.MonitoringModule;
import org.curioswitch.common.server.framework.monitoring.RpcMetricLabels;
import org.curioswitch.common.server.framework.monitoring.StackdriverReporter;
import org.curioswitch.common.server.framework.security.HttpsOnlyService;
import org.curioswitch.common.server.framework.security.SecurityModule;
import org.curioswitch.common.server.framework.server.HttpServiceDefinition;
import org.curioswitch.common.server.framework.server.PostServerCustomizer;
import org.curioswitch.common.server.framework.server.ServerShutDownDelayer;
import org.curioswitch.common.server.framework.staticsite.JavascriptStaticService;
import org.curioswitch.common.server.framework.staticsite.StaticSiteService;
import org.curioswitch.common.server.framework.staticsite.StaticSiteServiceDefinition;
import org.curioswitch.common.server.framework.util.ResourceUtil;
import org.curioswitch.curiostack.gcloud.core.auth.GcloudAuthModule;
import org.curioswitch.curiostack.gcloud.iam.GcloudIamModule;
import org.jooq.DSLContext;

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
    includes = {
      ApplicationModule.class,
      FirebaseAuthModule.class,
      GcloudAuthModule.class,
      GcloudIamModule.class,
      MonitoringModule.class,
      JwtModule.class,
      LoggingModule.class,
      SecurityModule.class
    })
public abstract class ServerModule {

  private static final Logger logger = LogManager.getLogger();

  private static final ApplicationProtocolConfig HTTPS_ALPN_CFG =
      new ApplicationProtocolConfig(
          Protocol.ALPN,
          // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
          SelectorFailureBehavior.NO_ADVERTISE,
          // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
          SelectedListenerFailureBehavior.ACCEPT,
          ApplicationProtocolNames.HTTP_2,
          ApplicationProtocolNames.HTTP_1_1);

  @Multibinds
  abstract Set<BindableService> grpcServices();

  @Multibinds
  abstract Set<GrpcServiceDefinition> grpcServiceDefinitions();

  @Multibinds
  abstract Set<HttpServiceDefinition> httpServiceDefinitions();

  @Multibinds
  abstract Set<StaticSiteServiceDefinition> staticSites();

  @Multibinds
  abstract Set<Consumer<ServerBuilder>> serverCustomizers();

  @Multibinds
  abstract Set<PostServerCustomizer> postServerCustomizers();

  @Multibinds
  abstract Set<WatchedPath> watchedPaths();

  @Multibinds
  @EagerInit
  abstract Set<Object> eagerInitializedDependencies();

  @Multibinds
  @CloseOnStop
  abstract Set<Closeable> closeOnStopDependencies();

  @Multibinds
  abstract Set<ServerShutDownDelayer> serverShutDownDelayers();

  @BindsOptionalOf
  abstract SslCommonNamesProvider sslCommonNamesProvider();

  @BindsOptionalOf
  abstract DSLContext db();

  @Provides
  @Singleton
  static ServerConfig serverConfig(Config config) {
    return ConfigBeanFactory.create(config.getConfig("server"), ModifiableServerConfig.class)
        .toImmutable();
  }

  @Provides
  @Singleton
  static JavascriptStaticConfig javascriptStaticConfig(Config config) {
    return ConfigBeanFactory.create(
            config.getConfig("javascriptConfig"), ModifiableJavascriptStaticConfig.class)
        .toImmutable();
  }

  @Provides
  static ServiceRequestContext context() {
    return RequestContext.current();
  }

  @Provides
  @Production
  static Executor executor(ServiceRequestContext ctx) {
    return ctx.eventLoop();
  }

  @Provides
  @Singleton
  static Optional<SelfSignedCertificate> selfSignedCertificate(ServerConfig serverConfig) {
    if (!serverConfig.isGenerateSelfSignedCertificate()) {
      return Optional.empty();
    }
    logger.warn("Generating self-signed certificate. This should only happen on local!!!");
    try {
      return Optional.of(new SelfSignedCertificate());
    } catch (CertificateException e) {
      // Can't happen.
      throw new IllegalStateException(e);
    }
  }

  @Provides
  static Optional<TrustManagerFactory> caTrustManager(ServerConfig serverConfig) {
    if (serverConfig.isDisableServerCertificateVerification()) {
      return Optional.of(InsecureTrustManagerFactory.INSTANCE);
    }
    if (serverConfig.getCaCertificatePath().isEmpty()) {
      return Optional.empty();
    }
    try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(null);

      keystore.setCertificateEntry("caCert", readCertificate(serverConfig.getCaCertificatePath()));
      if (!serverConfig.getAdditionalCaCertificatePath().isEmpty()) {
        keystore.setCertificateEntry(
            "additionalCaCert", readCertificate(serverConfig.getAdditionalCaCertificatePath()));
      }

      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(keystore);

      return Optional.of(trustManagerFactory);
    } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Could not load CA certificate.", e);
    }
  }

  // TODO(choko): Understand this rule better.
  @SuppressWarnings("FutureReturnValueIgnored")
  @Provides
  @Singleton
  static Server armeriaServer(
      Set<BindableService> grpcServices,
      Set<GrpcServiceDefinition> grpcServiceDefinitions,
      Set<HttpServiceDefinition> httpServiceDefinitions,
      Set<StaticSiteServiceDefinition> staticSites,
      Set<Consumer<ServerBuilder>> serverCustomizers,
      Set<PostServerCustomizer> postServerCustomizers,
      Set<WatchedPath> watchedPaths,
      Function<HttpService, LoggingService> loggingService,
      MetricsHttpService metricsHttpService,
      CollectorRegistry collectorRegistry,
      MeterRegistry meterRegistry,
      Tracing tracing,
      Lazy<FirebaseAuthorizer> firebaseAuthorizer,
      Lazy<JwtAuthorizer.Factory> jwtAuthorizer,
      Lazy<JavascriptStaticService> javascriptStaticService,
      Optional<SelfSignedCertificate> selfSignedCertificate,
      Optional<TrustManagerFactory> caTrustManager,
      Optional<SslCommonNamesProvider> sslCommonNamesProvider,
      FileWatcher.Builder fileWatcherBuilder,
      Lazy<StackdriverReporter> stackdriverReporter,
      ServerConfig serverConfig,
      FirebaseAuthConfig authConfig,
      HttpsOnlyService.Factory httpsOnlyServiceFactory,
      JavascriptStaticConfig javascriptStaticConfig,
      MonitoringConfig monitoringConfig,
      SecurityConfig securityConfig,
      Set<ServerShutDownDelayer> serverShutDownDelayers,
      @CloseOnStop Set<Closeable> closeOnStopDependencies,
      // Eagerly trigger bindings that are present, not actually used here.
      @EagerInit Set<Object> eagerInitializedDependencies) {
    for (WatchedPath watched : watchedPaths) {
      fileWatcherBuilder.registerPath(watched.getPath(), watched.getHandler());
    }

    if (!sslCommonNamesProvider.isPresent()
        && !serverConfig.getRpcAclsPath().isEmpty()
        && !serverConfig.isDisableSslAuthorization()) {
      Path path = Paths.get(serverConfig.getRpcAclsPath()).toAbsolutePath();
      RpcAclsCommonNamesProvider commonNamesProvider = new RpcAclsCommonNamesProvider(path);
      fileWatcherBuilder.registerPath(path, commonNamesProvider::processFile);
      if (path.toFile().exists()) {
        commonNamesProvider.processFile(path);
      }
      sslCommonNamesProvider = Optional.of(commonNamesProvider);
    }

    ServerBuilder sb = Server.builder().https(serverConfig.getPort());

    if (selfSignedCertificate.isPresent()) {
      SelfSignedCertificate certificate = selfSignedCertificate.get();
      SslContextKeyConverter.execute(
          ResourceUtil.openStream(certificate.certificate().getAbsolutePath()),
          ResourceUtil.openStream(certificate.privateKey().getAbsolutePath()),
          sb::tls);
      sb.tlsCustomizer(
          tls -> {
            tls.trustManager(InsecureTrustManagerFactory.INSTANCE);
            if (!serverConfig.isDisableSslAuthorization()) {
              tls.clientAuth(ClientAuth.OPTIONAL);
            }
          });
    } else if (serverConfig.getTlsCertificatePath().isEmpty()
        || serverConfig.getTlsPrivateKeyPath().isEmpty()
        || !caTrustManager.isPresent()) {
      throw new IllegalStateException(
          "No TLS configuration provided, Curiostack does not support non-TLS servers. "
              + "Use gradle-curio-cluster-plugin to set up a namespace and TLS.");
    } else {
      SslContextKeyConverter.execute(
          ResourceUtil.openStream(serverConfig.getTlsCertificatePath()),
          ResourceUtil.openStream(serverConfig.getTlsPrivateKeyPath()),
          sb::tls);
      sb.tlsCustomizer(
          tls -> {
            tls.trustManager(caTrustManager.get());
            if (!serverConfig.isDisableSslAuthorization()) {
              tls.clientAuth(ClientAuth.OPTIONAL);
            }
          });
    }

    serverCustomizers.forEach(c -> c.accept(sb));

    Optional<Function<HttpService, IpFilteringService>> ipFilter = Optional.empty();
    if (!serverConfig.getIpFilterRules().isEmpty()) {
      ipFilter = Optional.of(IpFilteringService.newDecorator(serverConfig.getIpFilterRules()));
    }

    if (!serverConfig.isDisableDocService()) {
      DocServiceBuilder docService = DocService.builder();
      if (!authConfig.getServiceAccountBase64().isEmpty()) {
        docService.injectedScripts(
            "armeria.registerHeaderProvider(function() {\n"
                + "  return firebase.auth().currentUser.getIdToken().then(token => { authorization: 'bearer ' + token });\n"
                + "});");
      }
      sb.serviceUnder(
          "/internal/docs", internalService(docService.build(), ipFilter, serverConfig));
    }

    SettableHealthChecker settableHealthChecker = new SettableHealthChecker(true);
    List<HealthChecker> healthCheckers =
        serverShutDownDelayers.isEmpty()
            ? ImmutableList.of()
            : ImmutableList.of(settableHealthChecker);

    sb.service(
        "/internal/health",
        internalService(HealthCheckService.of(healthCheckers), ipFilter, serverConfig));
    sb.service("/internal/dropwizard", internalService(metricsHttpService, ipFilter, serverConfig));
    sb.service(
        "/internal/metrics",
        internalService(
            new PrometheusExpositionService(collectorRegistry), ipFilter, serverConfig));
    if (sslCommonNamesProvider.isPresent()) {
      SslCommonNamesProvider namesProvider = sslCommonNamesProvider.get();
      sb.service(
          "/internal/rpcacls",
          internalService(
              (ctx, req) ->
                  HttpResponse.of(
                      HttpStatus.OK,
                      MediaType.PLAIN_TEXT_UTF_8,
                      String.join("\n", namesProvider.get())),
              ipFilter,
              serverConfig));
    }

    if (!grpcServices.isEmpty()) {
      GrpcServiceDefinition definition =
          new GrpcServiceDefinition.Builder()
              .addAllServices(grpcServices)
              .decorator(Function.identity())
              .path(serverConfig.getGrpcPath())
              .build();
      grpcServiceDefinitions =
          ImmutableSet.<GrpcServiceDefinition>builder()
              .addAll(grpcServiceDefinitions)
              .add(definition)
              .build();
    }

    for (GrpcServiceDefinition definition : grpcServiceDefinitions) {
      GrpcServiceBuilder serviceBuilder =
          GrpcService.builder()
              .supportedSerializationFormats(GrpcSerializationFormats.values())
              .enableUnframedRequests(true);
      definition.services().forEach(serviceBuilder::addService);
      if (!serverConfig.isDisableGrpcServiceDiscovery()) {
        serviceBuilder.addService(ProtoReflectionService.newInstance());
      }
      definition.customizer().accept(serviceBuilder);
      HttpServiceWithRoutes service = serviceBuilder.build();
      if (definition.path().equals("/")) {
        Optional<SslCommonNamesProvider> sslCommonNamesProvider0 = sslCommonNamesProvider;
        sb.service(
            service,
            s ->
                decorateService(
                    s.decorate(definition.decorator()),
                    tracing,
                    firebaseAuthorizer,
                    jwtAuthorizer,
                    sslCommonNamesProvider0,
                    serverConfig,
                    authConfig));
      } else {
        sb.serviceUnder(
            definition.path(),
            decorateService(
                service.decorate(definition.decorator()),
                tracing,
                firebaseAuthorizer,
                jwtAuthorizer,
                sslCommonNamesProvider,
                serverConfig,
                authConfig));
      }
    }

    for (HttpServiceDefinition definition : httpServiceDefinitions) {
      sb.service(
          definition.route(),
          decorateService(
              definition.service(),
              tracing,
              firebaseAuthorizer,
              jwtAuthorizer,
              sslCommonNamesProvider,
              serverConfig,
              authConfig));
    }

    if (javascriptStaticConfig.getVersion() != 0) {
      sb.service(
          "/static/jsconfig-" + javascriptStaticConfig.getVersion(), javascriptStaticService.get());
    }

    for (StaticSiteServiceDefinition staticSite : staticSites) {
      StaticSiteService.addToServer(
          staticSite.urlRoot(), staticSite.staticPath(), staticSite.classpathRoot(), sb);
    }

    if (ipFilter.isPresent() && !serverConfig.getIpFilterInternalOnly()) {
      sb.decorator(ipFilter.get());
    }

    if (securityConfig.getHttpsOnly()) {
      sb.decorator(httpsOnlyServiceFactory.newDecorator());
    }

    sb.decorator(loggingService);
    sb.meterRegistry(meterRegistry);

    if (serverConfig.getEnableGracefulShutdown()) {
      sb.gracefulShutdownTimeout(Duration.ofSeconds(10), Duration.ofSeconds(30));
    }

    postServerCustomizers.forEach((c) -> c.accept(sb));

    sb.serverListener(
        new ServerListener() {
          @Override
          public void serverStarting(Server server) {}

          @Override
          public void serverStarted(Server server) {}

          @Override
          public void serverStopping(Server server) {}

          @Override
          public void serverStopped(Server server) {
            closeOnStopDependencies.forEach(
                c -> {
                  try {
                    c.close();
                  } catch (IOException e) {
                    logger.info("Exception closing {}", c);
                  }
                });
          }
        });

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

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("Received shutdown signal.");

                  settableHealthChecker.setHealthy(false);

                  if (!serverShutDownDelayers.isEmpty()) {
                    Futures.getUnchecked(
                        Futures.successfulAsList(
                            serverShutDownDelayers.stream()
                                .map(ServerShutDownDelayer::readyForShutdown)
                                .collect(toImmutableList())));
                  }

                  logger.info("Server shutting down.");
                  server.stop().join();
                }));

    if (!fileWatcherBuilder.isEmpty()) {
      FileWatcher fileWatcher = fileWatcherBuilder.build();
      fileWatcher.start();
      Runtime.getRuntime().addShutdownHook(new Thread(fileWatcher::close));
    }

    if (monitoringConfig.isReportTraces()) {
      server
          .nextEventLoop()
          .scheduleAtFixedRate(
              stackdriverReporter.get()::flush,
              0,
              monitoringConfig.getTraceReportInterval().getSeconds(),
              TimeUnit.SECONDS);
    }

    return server;
  }

  private static HttpService decorateService(
      HttpService service,
      Tracing tracing,
      Lazy<FirebaseAuthorizer> firebaseAuthorizer,
      Lazy<JwtAuthorizer.Factory> jwtAuthorizer,
      Optional<SslCommonNamesProvider> sslCommonNamesProvider,
      ServerConfig serverConfig,
      FirebaseAuthConfig authConfig) {
    if (sslCommonNamesProvider.isPresent() && !serverConfig.isDisableSslAuthorization()) {
      AuthServiceBuilder authServiceBuilder = AuthService.builder();
      authServiceBuilder.add(new SslAuthorizer(sslCommonNamesProvider.get()));
      service = service.decorate(authServiceBuilder.newDecorator());
    }
    if (serverConfig.isEnableIapAuthorization()) {
      service =
          service
              .decorate(
                  (delegate, ctx, req) -> {
                    DecodedJWT jwt = ctx.attr(JwtAuthorizer.DECODED_JWT);
                    String loggedInUserEmail =
                        jwt != null ? jwt.getClaim("email").asString() : "unknown";
                    RequestLoggingContext.put(ctx, "logged_in_user", loggedInUserEmail);
                    return delegate.serve(ctx, req);
                  })
              .decorate(
                  AuthService.builder()
                      .addTokenAuthorizer(
                          headers ->
                              OAuth2Token.of(
                                  headers.get(HttpHeaderNames.of("x-goog-iap-jwt-assertion"))),
                          jwtAuthorizer
                              .get()
                              .create(
                                  Algorithm.ES256, "https://www.gstatic.com/iap/verify/public_key"))
                      .newDecorator());
    }
    if (!authConfig.getServiceAccountBase64().isEmpty()) {
      FirebaseAuthorizer authorizer = firebaseAuthorizer.get();
      service =
          service.decorate(
              AuthService.builder().addOAuth2(authorizer).onFailure(authorizer).newDecorator());
    }

    service =
        service
            .decorate(
                MetricCollectingService.newDecorator(
                    RpcMetricLabels.grpcRequestLabeler("grpc_services")))
            .decorate(BraveService.newDecorator(tracing))
            .decorate(
                (delegate, ctx, req) -> {
                  TraceContext traceCtx = tracing.currentTraceContext().get();
                  if (traceCtx != null) {
                    RequestLoggingContext.put(ctx, "traceId", traceCtx.traceIdString());
                    RequestLoggingContext.put(ctx, "spanId", traceCtx.spanIdString());
                  }
                  return delegate.serve(ctx, req);
                });
    return service;
  }

  private static HttpService internalService(
      HttpService service,
      Optional<Function<HttpService, IpFilteringService>> ipFilter,
      ServerConfig config) {
    if (!ipFilter.isPresent() || !config.getIpFilterInternalOnly()) {
      return service;
    }
    return service.decorate(ipFilter.get());
  }

  private static X509Certificate readCertificate(String path) throws CertificateException {
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    try (InputStream is = ResourceUtil.openStream(path)) {
      return (X509Certificate) certificateFactory.generateCertificate(is);
    } catch (IOException e) {
      throw new UncheckedIOException("Error reading certificate from " + path, e);
    }
  }
}
