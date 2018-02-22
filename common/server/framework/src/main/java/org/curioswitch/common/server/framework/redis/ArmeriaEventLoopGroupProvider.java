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

import com.linecorp.armeria.common.CommonPools;
import com.linecorp.armeria.common.Flags;
import io.lettuce.core.resource.EventLoopGroupProvider;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.util.concurrent.TimeUnit;

public enum ArmeriaEventLoopGroupProvider implements EventLoopGroupProvider {
  INSTANCE;

  @Override
  public <T extends EventLoopGroup> T allocate(Class<T> type) {
    if (type.isInstance(CommonPools.workerGroup())) {
      @SuppressWarnings("unchecked")
      T group = (T) CommonPools.workerGroup();
      return group;
    }
    throw new IllegalStateException(
        "Armeria and lettuce should always use same event loop group type, expected: " + type);
  }

  @Override
  public int threadPoolSize() {
    return Flags.numCommonWorkers();
  }

  @Override
  public Future<Boolean> release(
      EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit) {
    return ImmediateEventExecutor.INSTANCE.newSucceededFuture(true);
  }

  @Override
  public Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
    return ImmediateEventExecutor.INSTANCE.newSucceededFuture(true);
  }
}
