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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedMap;
import com.linecorp.armeria.common.RpcRequest;
import com.linecorp.armeria.common.logging.RequestLog;
import com.linecorp.armeria.common.metric.MetricLabel;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.curioswitch.common.server.framework.immutables.CurioStyle;
import org.immutables.value.Value.Immutable;

public final class RpcMetricLabels {

  @Immutable
  @CurioStyle
  public interface RpcMetricLabel extends MetricLabel<RpcMetricLabel> {
    static RpcMetricLabel of(String name) {
      return ImmutableRpcMetricLabel.builder().name(name).build();
    }

    String name();
  }

  private static final Splitter PATH_SPLITTER = Splitter.on('/');

  public static final RpcMetricLabel SERVICE = RpcMetricLabel.of("service");
  public static final RpcMetricLabel METHOD = RpcMetricLabel.of("method");

  public static Function<RequestLog, Map<RpcMetricLabel, String>> grpcRequestLabeler() {
    return log -> {
      // The service name and method name are currently both present in the method's log for
      // armeria.
      List<String> methodParts =
          PATH_SPLITTER.splitToList(((RpcRequest) log.requestContent()).method());
      return ImmutableSortedMap.of(SERVICE, methodParts.get(0), METHOD, methodParts.get(1));
    };
  }

  private RpcMetricLabels() {}
}
