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

package org.curioswitch.common.server.framework.monitoring;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.log4j2.InstrumentedAppender;
import com.google.common.io.Resources;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

@Module
public class MonitoringModule {

  @Provides
  @Singleton
  MetricRegistry metricRegistry() {
    MetricRegistry registry = new MetricRegistry();
    configureDefaultMetrics(registry);
    return registry;
  }

  private static void configureDefaultMetrics(MetricRegistry registry) {
    configureGitMetrics(registry);
    configureJvmMetrics(registry);
    configureLogMetrics(registry);
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

  private static void configureLogMetrics(MetricRegistry registry) {
    InstrumentedAppender appender = new InstrumentedAppender(registry);
    appender.start();
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    Configuration config = context.getConfiguration();
    config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).addAppender(appender, null, null);
    context.updateLoggers(config);
  }
}
