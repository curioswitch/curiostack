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
package org.curioswitch.common.server.framework.logging;

import com.google.common.collect.ImmutableSet;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.client.logging.LoggingClientBuilder;
import com.linecorp.armeria.common.HttpHeadersBuilder;
import com.linecorp.armeria.common.logging.LogLevel;
import com.linecorp.armeria.common.logging.LoggingDecoratorBuilder;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.logging.LoggingService;
import com.linecorp.armeria.server.logging.LoggingServiceBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import dagger.multibindings.Multibinds;
import dagger.producers.monitoring.ProductionComponentMonitor;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Singleton;
import org.curioswitch.common.server.framework.ApplicationModule;
import org.curioswitch.common.server.framework.config.LoggingConfig;
import org.curioswitch.common.server.framework.config.ModifiableLoggingConfig;

@Module(includes = ApplicationModule.class)
public abstract class LoggingModule {

  @Multibinds
  abstract @RequestHeaderSanitizer Set<Consumer<HttpHeadersBuilder>> requestHeaderSanitizers();

  @Multibinds
  abstract @ResponseHeaderSanitizer Set<Consumer<HttpHeadersBuilder>> responseHeaderSanitizers();

  @Provides
  @Singleton
  static LoggingConfig config(Config config) {
    return ConfigBeanFactory.create(config.getConfig("logging"), ModifiableLoggingConfig.class)
        .toImmutable();
  }

  @Provides
  @Singleton
  static Function<HttpService, LoggingService> loggingService(
      LoggingConfig config,
      @RequestHeaderSanitizer Set<Consumer<HttpHeadersBuilder>> requestHeaderSanitizers,
      @ResponseHeaderSanitizer Set<Consumer<HttpHeadersBuilder>> responseHeaderSanitizers) {
    LoggingServiceBuilder builder = LoggingService.builder();
    configureLoggingDecorator(builder, config, requestHeaderSanitizers, responseHeaderSanitizers);
    if (config.getLogAllServerRequests()) {
      builder.requestLogLevel(LogLevel.INFO);
      builder.successfulResponseLogLevel(LogLevel.INFO);
    }
    return builder::build;
  }

  @Provides
  @Singleton
  static Function<HttpClient, LoggingClient> loggingClient(
      LoggingConfig config,
      @RequestHeaderSanitizer Set<Consumer<HttpHeadersBuilder>> requestHeaderSanitizers,
      @ResponseHeaderSanitizer Set<Consumer<HttpHeadersBuilder>> responseHeaderSanitizers) {
    LoggingClientBuilder builder = LoggingClient.builder();
    configureLoggingDecorator(builder, config, requestHeaderSanitizers, responseHeaderSanitizers);
    if (config.getLogAllClientRequests()) {
      builder.requestLogLevel(LogLevel.INFO);
      builder.successfulResponseLogLevel(LogLevel.INFO);
    }
    return builder::build;
  }

  private static void configureLoggingDecorator(
      LoggingDecoratorBuilder builder,
      LoggingConfig config,
      @RequestHeaderSanitizer Set<Consumer<HttpHeadersBuilder>> requestHeaderSanitizers,
      @ResponseHeaderSanitizer Set<Consumer<HttpHeadersBuilder>> responseHeaderSanitizers) {
    if (!config.getBlacklistedRequestHeaders().isEmpty() || !requestHeaderSanitizers.isEmpty()) {
      builder.requestHeadersSanitizer(
          new HeaderSanitizer(requestHeaderSanitizers, config.getBlacklistedRequestHeaders()));
    }

    if (!config.getBlacklistedResponseHeaders().isEmpty() || !responseHeaderSanitizers.isEmpty()) {
      builder.responseHeadersSanitizer(
          new HeaderSanitizer(responseHeaderSanitizers, config.getBlacklistedResponseHeaders()));
    }
  }

  @Provides
  @ElementsIntoSet
  static Set<ProductionComponentMonitor.Factory> loggingMonitor(
      Lazy<TracingProductionComponentMonitor.Factory> tracingFactory, LoggingConfig config) {
    ImmutableSet.Builder<ProductionComponentMonitor.Factory> monitors = ImmutableSet.builder();
    if (config.getLogProducerExecution()) {
      monitors.add(new LoggingProductionComponentMonitor.Factory());
    }
    if (config.getTraceProducerExecution()) {
      monitors.add(tracingFactory.get());
    }
    return monitors.build();
  }

  private LoggingModule() {}
}
