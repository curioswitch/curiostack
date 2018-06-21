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
package org.curioswitch.common.server.framework.monitoring;

import brave.Tracing;
import brave.sampler.Sampler;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.io.Resources;
import com.linecorp.armeria.common.metric.PrometheusMeterRegistries;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.log4j2.InstrumentedAppender;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.curioswitch.common.server.framework.ApplicationModule;
import org.curioswitch.common.server.framework.config.ModifiableMonitoringConfig;
import org.curioswitch.common.server.framework.config.MonitoringConfig;
import org.curioswitch.gcloud.trace.GcloudTraceModule;

@Module(includes = {ApplicationModule.class, GcloudTraceModule.class})
public abstract class MonitoringModule {

  @Provides
  @Singleton
  static MonitoringConfig monitoringConfig(Config config) {
    return ConfigBeanFactory.create(
            config.getConfig("monitoring"), ModifiableMonitoringConfig.class)
        .toImmutable();
  }

  @Provides
  @Singleton
  static MetricRegistry metricRegistry() {
    MetricRegistry registry = new MetricRegistry();
    configureDefaultMetrics(registry);
    return registry;
  }

  @Provides
  @Singleton
  static CollectorRegistry collectorRegistry() {
    CollectorRegistry registry = CollectorRegistry.defaultRegistry;
    DefaultExports.initialize();
    configureLogMetrics();
    return registry;
  }

  @Provides
  @Singleton
  static MeterRegistry meterRegistry(CollectorRegistry collectorRegistry) {
    return PrometheusMeterRegistries.newRegistry(collectorRegistry);
  }

  @Provides
  @Singleton
  static Tracing tracing(Lazy<StackdriverReporter> reporter, MonitoringConfig config) {
    Tracing.Builder builder =
        Tracing.newBuilder()
            .localServiceName(config.getServerName())
            .traceId128Bit(true)
            .supportsJoin(false)
            .sampler(Sampler.ALWAYS_SAMPLE);
    if (config.isReportTraces()) {
      builder.spanReporter(reporter.get());
    }
    return builder.build();
  }

  private static void configureDefaultMetrics(MetricRegistry registry) {
    configureGitMetrics(registry);
    configureJvmMetrics(registry);
  }

  private static void configureGitMetrics(MetricRegistry registry) {
    try {
      Properties gitProperties = new Properties();
      gitProperties.load(Resources.getResource("git.properties").openStream());
      for (String key : gitProperties.stringPropertyNames()) {
        String value = gitProperties.getProperty(key);
        registry.register(key, (Gauge<String>) () -> value);
      }
    } catch (IOException e) {
      // git properties missing, ignore.
    }
  }

  private static void configureJvmMetrics(MetricRegistry registry) {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    registry.register("jvm.buffer-pool", new BufferPoolMetricSet(mBeanServer));
    registry.register("jvm.class-loading", new ClassLoadingGaugeSet());
    registry.register("jvm.file-descriptor-ratio", new FileDescriptorRatioGauge());
    registry.register("jvm.gc", new GarbageCollectorMetricSet());
    registry.register("jvm.memory", new MemoryUsageGaugeSet());
    registry.register("jvm.threads", new ThreadStatesGaugeSet());
  }

  private static void configureLogMetrics() {
    InstrumentedAppender appender = InstrumentedAppender.createAppender("PROMETHEUS");
    appender.start();
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    Configuration config = context.getConfiguration();
    config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).addAppender(appender, null, null);
    context.updateLoggers(config);
  }

  private MonitoringModule() {}
}
