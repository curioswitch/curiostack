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
package org.curioswitch.common.server.framework.monitoring;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.linecorp.armeria.common.metric.MeterIdPrefix;
import com.linecorp.armeria.common.metric.MeterIdPrefixFunction;
import java.util.List;

public final class RpcMetricLabels {

  private static final Splitter PATH_SPLITTER = Splitter.on('/');

  public static MeterIdPrefixFunction grpcRequestLabeler(String name) {
    return (registry, log) -> {
      // The service name and method name will always be the last two path components.
      List<String> methodParts = ImmutableList.copyOf(PATH_SPLITTER.split(log.path())).reverse();
      if (methodParts.size() == 2) {
        return new MeterIdPrefix(name, "service", methodParts.get(1), "method", methodParts.get(0));
      } else {
        return new MeterIdPrefix(name);
      }
    };
  }

  private RpcMetricLabels() {}
}
