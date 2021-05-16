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

package org.curioswitch.curiostack.gcloud.core.grpc;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.linecorp.armeria.common.logging.RequestLog;
import com.linecorp.armeria.common.logging.RequestOnlyLog;
import com.linecorp.armeria.common.metric.MeterIdPrefix;
import com.linecorp.armeria.common.metric.MeterIdPrefixFunction;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;

public class MetricLabels {

  private static final String NAME = "grpc_clients";
  private static final Splitter PATH_SPLITTER = Splitter.on('/');

  public static MeterIdPrefixFunction grpcRequestLabeler() {
    return GrpcRequestLabeler.INSTANCE;
  }

  private enum GrpcRequestLabeler implements MeterIdPrefixFunction {
    INSTANCE;

    @Override
    public MeterIdPrefix activeRequestPrefix(MeterRegistry registry, RequestOnlyLog log) {
      List<String> methodParts =
          Lists.reverse(PATH_SPLITTER.splitToList(log.requestHeaders().path()));
      if (methodParts.size() == 2) {
        return new MeterIdPrefix(NAME, "service", methodParts.get(1), "method", methodParts.get(0));
      } else {
        return new MeterIdPrefix(NAME);
      }
    }

    @Override
    public MeterIdPrefix completeRequestPrefix(MeterRegistry registry, RequestLog log) {
      return activeRequestPrefix(registry, log);
    }
  }

  private MetricLabels() {}
}
