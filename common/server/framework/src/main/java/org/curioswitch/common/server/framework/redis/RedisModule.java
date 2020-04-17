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
package org.curioswitch.common.server.framework.redis;

import static org.curioswitch.common.server.framework.redis.RedisConstants.DEFAULT_METER_ID_PREFIX;

import brave.Tracing;
import com.linecorp.armeria.common.CommonPools;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Module;
import dagger.Provides;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.tracing.BraveTracing;
import io.micrometer.core.instrument.MeterRegistry;
import javax.inject.Singleton;
import org.curioswitch.common.server.framework.config.ModifiableRedisConfig;
import org.curioswitch.common.server.framework.config.RedisConfig;
import org.curioswitch.common.server.framework.monitoring.MonitoringModule;

@Module(includes = MonitoringModule.class)
public abstract class RedisModule {

  @Provides
  @Singleton
  static RedisConfig redisConfig(Config config) {
    return ConfigBeanFactory.create(config.getConfig("redis"), ModifiableRedisConfig.class)
        .toImmutable();
  }

  @Provides
  @Singleton
  static RedisClusterClient redisClusterClient(
      RedisConfig config, MeterRegistry registry, Tracing tracing) {
    RedisClusterClient client =
        RedisClusterClient.create(
            DefaultClientResources.builder()
                .eventExecutorGroup(CommonPools.workerGroup())
                .eventLoopGroupProvider(ArmeriaEventLoopGroupProvider.INSTANCE)
                .commandLatencyCollector(
                    new MicrometerCommandLatencyCollector(DEFAULT_METER_ID_PREFIX, registry))
                .tracing(BraveTracing.create(tracing))
                .build(),
            config.getUrl());
    client.setOptions(ClusterClientOptions.builder().validateClusterNodeMembership(false).build());
    return client;
  }

  @Provides
  @Singleton
  static RedisClient redisClient(RedisConfig config, MeterRegistry registry, Tracing tracing) {
    return RedisClient.create(
        DefaultClientResources.builder()
            .eventExecutorGroup(CommonPools.workerGroup())
            .eventLoopGroupProvider(ArmeriaEventLoopGroupProvider.INSTANCE)
            .commandLatencyCollector(
                new MicrometerCommandLatencyCollector(DEFAULT_METER_ID_PREFIX, registry))
            .tracing(BraveTracing.create(tracing))
            .build(),
        config.getUrl());
  }

  private RedisModule() {}
}
