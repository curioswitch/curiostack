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

import static com.google.common.base.Preconditions.checkNotNull;

import brave.Span;
import brave.Tracing;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.server.ServiceRequestContext;
import io.netty.util.AttributeKey;
import java.util.Optional;
import javax.inject.Inject;

public final class RequestLoggingContext {

  private static final AttributeKey<ImmutableMap<String, String>> LOGGING_CONTEXT =
      AttributeKey.valueOf(RequestLoggingContext.class, "LOGGING_CONTEXT");

  public static void put(RequestContext ctx, String key, String value) {
    // Copy into a new map similar to what log4j2 does for thread-safety.
    ImmutableMap<String, String> oldLoggingContext = ctx.attr(LOGGING_CONTEXT).get();
    if (oldLoggingContext == null) {
      oldLoggingContext = ImmutableMap.of();
    }
    ImmutableMap.Builder<String, String> newLoggingContext =
        ImmutableMap.builderWithExpectedSize(oldLoggingContext.size() + 1);
    oldLoggingContext.forEach(
        (k, v) -> {
          if (!k.equals(key)) {
            newLoggingContext.put(k, v);
          }
        });
    newLoggingContext.put(key, value);
    ctx.attr(LOGGING_CONTEXT).set(newLoggingContext.build());
  }

  static ImmutableMap<String, String> get() {
    ImmutableMap<String, String> loggingContext =
        MoreObjects.firstNonNull(
            RequestContext.mapCurrent(ctx -> ctx.attr(LOGGING_CONTEXT).get(), ImmutableMap::of),
            ImmutableMap.of());
    checkNotNull(loggingContext);
    return loggingContext;
  }

  private final RequestContext ctx;
  private final Optional<Tracing> tracing;

  @Inject
  public RequestLoggingContext(ServiceRequestContext ctx, Optional<Tracing> tracing) {
    this.ctx = ctx;
    this.tracing = tracing;
  }

  public void addToLogs(String key, String value) {
    put(ctx, key, value);
  }

  public void addToLogsAndSpan(String key, String value) {
    addToLogs(key, value);
    if (tracing.isPresent()) {
      Span span = tracing.get().tracer().currentSpan();
      if (span != null) {
        span.tag(key, value);
      }
    }
  }
}
