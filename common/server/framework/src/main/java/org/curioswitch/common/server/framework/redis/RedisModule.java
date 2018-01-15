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

package org.curioswitch.common.server.framework.redis;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.lettuce.core.RedisClient;
import javax.inject.Singleton;
import org.curioswitch.common.server.framework.config.ModifiableRedisConfig;
import org.curioswitch.common.server.framework.config.RedisConfig;
import org.curioswitch.common.server.framework.inject.EagerInit;

@Module
public abstract class RedisModule {

  @Provides
  @Singleton
  static RedisConfig redisConfig(Config config) {
    return ConfigBeanFactory.create(config.getConfig("redis"), ModifiableRedisConfig.class)
        .toImmutable();
  }

  @Provides
  @Singleton
  static RedisClient redisClient(RedisConfig config) {
    return RedisClient.create(config.getUrl());
  }

  @Provides
  @EagerInit
  @IntoSet
  static Object init(RedisClient redisClient) {
    return redisClient;
  }
}
