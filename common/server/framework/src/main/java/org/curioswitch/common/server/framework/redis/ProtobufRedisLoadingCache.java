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

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import com.spotify.futures.CompletableFuturesExtra;
import dagger.Lazy;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisException;
import io.lettuce.core.SetArgs;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.common.server.framework.armeria.CurrentRequestContextExecutor;
import org.curioswitch.common.server.framework.config.RedisConfig;

/**
 * A {@link AsyncLoadingCache} that is backed by a remote remoteCache cache, specialized for the
 * common case where the key and value are protobuf for easy serialization. This class should be
 * used even when no local caching is desired as it ensures multiple reads of the same key share the
 * same write to remoteCache.
 */
public class ProtobufRedisLoadingCache<K extends Message, V extends Message> {

  /** A {@link Factory} for creating {@link ProtobufRedisLoadingCache}. */
  @Singleton
  public static class Factory {

    private final Lazy<RedisClusterClient> redisClient;
    private final RedisConfig config;
    private final MeterRegistry meterRegistry;

    @Inject
    public Factory(
        Lazy<RedisClusterClient> redisClient, RedisConfig config, MeterRegistry meterRegistry) {
      this.redisClient = redisClient;
      this.config = config;
      this.meterRegistry = meterRegistry;
    }

    /**
     * Constructs a new {@link ProtobufRedisLoadingCache} that can write protobuf {@link Message}
     * keys and values to remoteCache, with an optional local cache layer.
     *
     * @param name name of this cache, will be prefixed onto all keys.
     * @param keyPrototype a prototype for the key {@link Message}, usually gotten from {@code
     *     Key.getDefaultInstance()}.
     * @param valuePrototype a prototype for the value {@link Message}, usually gotten from {@code
     *     Value.getDefaultInstance()}.
     * @param redisTtl the time until expiration of a value in the remoteCache cache. The local
     *     cache should be considered in localCacheSpec.
     * @param localCacheSpec a {@link CaffeineSpec} to control the local cache layer. If {@code
     *     null}, local caching will be disabled.
     */
    public <K extends Message, V extends Message> ProtobufRedisLoadingCache<K, V> create(
        String name,
        K keyPrototype,
        V valuePrototype,
        Duration redisTtl,
        @Nullable CaffeineSpec localCacheSpec) {
      return create(name, keyPrototype, valuePrototype, redisTtl, false, localCacheSpec);
    }

    /**
     * Constructs a new {@link ProtobufRedisLoadingCache} that can write protobuf {@link Message}
     * keys and values to remoteCache, with an optional local cache layer.
     *
     * @param name name of this cache, will be prefixed onto all keys.
     * @param keyPrototype a prototype for the key {@link Message}, usually gotten from {@code
     *     Key.getDefaultInstance()}.
     * @param valuePrototype a prototype for the value {@link Message}, usually gotten from {@code
     *     Value.getDefaultInstance()}.
     * @param redisTtl the time until expiration of a value in the remoteCache cache. The local
     *     cache should be considered in localCacheSpec.
     * @param redisMasterOnly whether remoteCache reads should only happen from master. Best-effort,
     *     temporary persistent storage should set this to {@code true}.
     * @param localCacheSpec a {@link CaffeineSpec} to control the local cache layer. If {@code
     *     null}, local caching will be disabled.
     */
    public <K extends Message, V extends Message> ProtobufRedisLoadingCache<K, V> create(
        String name,
        K keyPrototype,
        V valuePrototype,
        Duration redisTtl,
        boolean redisMasterOnly,
        @Nullable CaffeineSpec localCacheSpec) {
      return new ProtobufRedisLoadingCache<>(
          keyPrototype,
          valuePrototype,
          redisTtl,
          localCacheSpec,
          config.isNoop()
              ? new NoopRemoteCache<>()
              : createRedisRemoteCache(
                  name,
                  redisClient.get(),
                  keyPrototype,
                  valuePrototype,
                  redisMasterOnly ? ReadFrom.MASTER : ReadFrom.NEAREST));
    }

    private <K extends Message, V extends Message> RemoteCache<K, V> createRedisRemoteCache(
        String name,
        RedisClusterClient redisClient,
        K keyPrototype,
        V valuePrototype,
        ReadFrom readFrom) {
      StatefulRedisClusterConnection<K, V> connection =
          redisClient.connect(
              new ProtobufRedisCodec<>(
                  (name + ":").getBytes(StandardCharsets.UTF_8), keyPrototype, valuePrototype));
      connection.setReadFrom(readFrom);
      return new RedisClusterRemoteCache<>(connection.async(), name, meterRegistry);
    }
  }

  private static final Logger logger = LogManager.getLogger();

  private final RemoteCache<K, V> remoteCache;
  private final AsyncLoadingCache<K, V> cache;
  private final SetArgs setArgs;

  ProtobufRedisLoadingCache(
      K keyPrototype,
      V valuePrototype,
      Duration redisTtl,
      @Nullable CaffeineSpec localCacheSpec,
      RemoteCache<K, V> remoteCache) {
    checkNotNull(keyPrototype, "keyPrototype");
    checkNotNull(valuePrototype, "valuePrototype");
    checkNotNull(redisTtl, "redisTtl");
    this.remoteCache = checkNotNull(remoteCache, "remoteCache");
    final Caffeine<Object, Object> caffeineBuilder =
        localCacheSpec != null
            ? Caffeine.from(localCacheSpec)
            : Caffeine.newBuilder().maximumSize(0);
    cache =
        caffeineBuilder
            .executor(CurrentRequestContextExecutor.INSTANCE)
            .buildAsync((k, executor) -> remoteCache.get(k).toCompletableFuture());
    setArgs = SetArgs.Builder.px(redisTtl.toMillis());
  }

  /**
   * Returns the value for the given {@code key}, computing the value from {@code loader} if it is
   * not present in cache.
   */
  public ListenableFuture<V> get(K key, Function<K, ListenableFuture<V>> loader) {
    return CompletableFuturesExtra.toListenableFuture(
        cache.get(key, (k, executor) -> loadWithCache(k, executor, loader)));
  }

  /**
   * Returns the value for the given {@code key} if it is present in cache, otherwise returns {@code
   * null}.
   */
  public ListenableFuture<V> getIfPresent(K key) {
    return CompletableFuturesExtra.toListenableFuture(cache.get(key));
  }

  /**
   * Sets the given {@code value} for the given {@code key} in cache. Simultaneous writes to the
   * same key will result in consecutive updates, so this method should only be used when this is
   * acceptable (e.g., for storing state that might be used to resume an operation). For general
   * cache semantics, use {@link #get(Message, Function)}.
   */
  public ListenableFuture<V> put(K key, V value) {
    CompletableFuture<V> setAndReturnValueFuture =
        remoteCache.set(key, value, setArgs).thenApply(unused -> value).toCompletableFuture();
    cache.put(key, setAndReturnValueFuture);
    return CompletableFuturesExtra.toListenableFuture(setAndReturnValueFuture);
  }

  /**
   * Deletes the given {@code key} from the remoteCache cache. This can be used to invalidate remote
   * caches. It is not practical to invalidate local caches remotely, so this is only useful if the
   * local cache is disabled everywhere (i.e., {@code localCacheSpec} is null on construction).
   */
  public void deleteFromRedis(K key) {
    remoteCache.del(key);
  }

  private CompletableFuture<V> loadWithCache(
      K key, Executor executor, Function<K, ListenableFuture<V>> loader) {
    final CompletionStage<V> fromCache;
    try {
      fromCache = remoteCache.get(key);
    } catch (RedisException t) {
      logger.warn("Error reading from remoteCache cache. Computing value anyways.", t);
      return CompletableFuturesExtra.toCompletableFuture(loader.apply(key));
    }
    return fromCache
        .handleAsync(
            (cached, t) -> {
              if (cached != null) {
                return CompletableFuture.completedFuture(cached);
              }
              if (t != null) {
                logger.warn("Error reading from remoteCache cache. Computing value anyways.", t);
              }
              CompletableFuture<V> loaded =
                  CompletableFuturesExtra.toCompletableFuture(loader.apply(key));
              loaded.thenAcceptAsync(val -> remoteCache.set(key, val, setArgs), executor);
              return loaded;
            },
            executor)
        // Converts CompletionStage<CompletionStage<U>> to CompletionStage<U>
        .thenCompose(Function.identity())
        .toCompletableFuture();
  }
}
