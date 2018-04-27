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

package org.curioswitch.curiostack.gcloud.core.util;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.CompletableFuture.completedFuture;

import io.netty.util.concurrent.EventExecutor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncRefreshingValue<T> {

  private static final Logger logger = LoggerFactory.getLogger(AsyncRefreshingValue.class);

  private static final Duration REFRESH_SKEW = Duration.ofMinutes(5);

  private final Supplier<CompletableFuture<T>> refresher;
  private final Function<T, Instant> expirationTimeExtractor;
  private final EventExecutor executor;
  private final Clock clock;

  @Nullable
  private volatile T currentValue;
  @Nullable
  private volatile Instant expirationTime;

  @Nullable private volatile CompletableFuture<T> pendingRefresh;

  public AsyncRefreshingValue(
      Supplier<CompletableFuture<T>> refresher,
      Function<T, Instant> expirationTimeExtractor,
      EventExecutor executor,
      Clock clock) {
    this.refresher = refresher;
    this.expirationTimeExtractor = expirationTimeExtractor;
    this.executor = executor;
    this.clock = clock;
  }

  public CompletableFuture<T> get() {
    if (expirationTime == null) {
      return refreshIfNotRefreshing();
    }
    if (clock.instant().isBefore(expirationTime)) {
      // Even if the value was updated during this check, it can only be newer and is always safe
      // to return without making this atomic.
      return completedFuture(currentValue);
    }
    CompletableFuture<T> pendingRefresh = this.pendingRefresh;
    if (pendingRefresh != null) {
      return pendingRefresh;
    }
    // Since we eagerly refresh, it should be extremely rare to have to refresh on demand like this
    // so synchronization overhead isn't a big deal.
    return refreshIfNotRefreshing();
  }

  private synchronized CompletableFuture<T> refreshIfNotRefreshing() {
    CompletableFuture<T> pendingRefresh = this.pendingRefresh;
    if (pendingRefresh != null) {
      return pendingRefresh;
    }
    pendingRefresh = this.refresh();
    this.pendingRefresh = pendingRefresh;
    return pendingRefresh;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private CompletableFuture<T> refresh() {
    return refresher
        .get()
        .handleAsync(
            (newValue, t) -> {
              if (t != null) {
                logger.warn("Unexpected error refreshing value.", t);
                // 30 sec delay should be long enough to not require backoff.
                executor.schedule(this::refresh, 30, TimeUnit.SECONDS);
                return currentValue;
              }

              currentValue = newValue;
              expirationTime = expirationTimeExtractor.apply(currentValue);
              pendingRefresh = null;

              // Optimistically refresh before expiration.
              Instant refreshTime = expirationTime.minus(REFRESH_SKEW);
              long refreshDelayMillis = MILLIS.between(clock.instant(), refreshTime);
              if (refreshDelayMillis > 0) {
                executor.schedule(this::refresh, refreshDelayMillis, TimeUnit.MILLISECONDS);
              }
              return newValue;
            },
            executor);
  }
}
