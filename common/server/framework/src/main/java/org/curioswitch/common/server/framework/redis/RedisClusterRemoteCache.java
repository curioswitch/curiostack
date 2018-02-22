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

import static org.curioswitch.common.server.framework.redis.RedisConstants.DEFAULT_METER_ID_PREFIX;

import brave.Span;
import brave.Span.Kind;
import brave.Tracer;
import brave.Tracing;
import io.lettuce.core.SetArgs;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nullable;

class RedisClusterRemoteCache<K, V> implements RemoteCache<K, V> {

  private final RedisClusterAsyncCommands<K, V> redis;
  private final String name;

  private final Counter success;
  private final Counter failure;

  RedisClusterRemoteCache(
      RedisClusterAsyncCommands<K, V> redis, String name, MeterRegistry registry) {
    this.redis = redis;
    this.name = name;

    String requests = DEFAULT_METER_ID_PREFIX.name("requests");
    success =
        registry.counter(
            requests, DEFAULT_METER_ID_PREFIX.tags("result", "success", "cache", name));
    failure =
        registry.counter(
            requests, DEFAULT_METER_ID_PREFIX.tags("result", "failure", "cache", name));
  }

  @Override
  public CompletionStage<V> get(K key) {
    Span span = newSpan("get");
    return record(redis.get(key), span, true);
  }

  @Override
  public CompletionStage<String> set(K key, V value, SetArgs setArgs) {
    Span span = newSpan("set");
    return record(redis.set(key, value, setArgs), span, false);
  }

  @Override
  public CompletionStage<Long> del(K key) {
    Span span = newSpan("del");
    return record(redis.del(key), span, false);
  }

  @Nullable
  Span newSpan(String method) {
    Tracer tracer = Tracing.currentTracer();
    if (tracer == null) {
      return null;
    }
    return tracer.nextSpan().kind(Kind.CLIENT).name(method).tag("component", name).start();
  }

  private <T> CompletionStage<T> record(
      CompletionStage<T> stage, @Nullable Span span, boolean recordHit) {
    return stage.whenComplete(
        (val, t) -> {
          if (t != null) {
            failure.increment();
          } else {
            success.increment();
          }
          if (span != null) {
            if (t != null) {
              span.tag("result", "error");
            } else {
              span.tag("result", "success");
            }
            if (recordHit) {
              span.tag("hit", val != null ? "true" : "false");
            }
            span.finish();
          }
        });
  }
}
