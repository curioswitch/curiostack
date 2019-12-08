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

import com.google.common.collect.ImmutableMap;
import com.linecorp.armeria.common.metric.MeterIdPrefix;
import com.linecorp.armeria.common.metric.MoreMeters;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.metrics.CommandLatencyId;
import io.lettuce.core.metrics.CommandMetrics;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Map;

class MicrometerCommandLatencyCollector implements CommandLatencyCollector {

  private final MeterIdPrefix idPrefix;
  private final MeterRegistry registry;

  MicrometerCommandLatencyCollector(MeterIdPrefix idPrefix, MeterRegistry registry) {
    this.idPrefix = idPrefix.append("totalDuration");
    this.registry = registry;
  }

  @Override
  public void recordCommandLatency(
      SocketAddress local,
      SocketAddress remote,
      ProtocolKeyword commandType,
      long firstResponseLatency,
      long completionLatency) {
    MoreMeters.newTimer(
            registry,
            idPrefix.name(),
            idPrefix.tags("remote", remote.toString(), "command", commandType.name()))
        .record(Duration.ofNanos(completionLatency));
  }

  @Override
  public void shutdown() {}

  @Override
  public Map<CommandLatencyId, CommandMetrics> retrieveMetrics() {
    // Metrics are collected in micrometer, so we don't expose anything here.
    return ImmutableMap.of();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
