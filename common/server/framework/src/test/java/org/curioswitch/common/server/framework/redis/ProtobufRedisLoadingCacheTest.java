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

package org.curioswitch.common.server.framework.redis;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ProtobufRedisLoadingCacheTest {

  private static final Duration TTL = Duration.ofMinutes(10);

  private static final StringValue KEY1 = StringValue.newBuilder().setValue("key1").build();

  @Mock private RemoteCache<StringValue, Int32Value> remoteCache;

  private ProtobufRedisLoadingCache<StringValue, Int32Value> cache;

  @BeforeEach
  void setUp() {
    cache =
        new ProtobufRedisLoadingCache<>(
            StringValue.getDefaultInstance(),
            Int32Value.getDefaultInstance(),
            TTL,
            null,
            remoteCache);
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class DeleteFromRedis {
    @Test
    void normal() {
      when(remoteCache.del(KEY1)).thenReturn(completedFuture(1L));

      assertThat(cache.deleteFromRedis(KEY1)).completesWithValue(true);
    }

    @Test
    void notFound() {
      when(remoteCache.del(KEY1)).thenReturn(completedFuture(0L));

      assertThat(cache.deleteFromRedis(KEY1)).completesWithValue(false);
    }
  }
}
