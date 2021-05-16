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

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import dagger.producers.monitoring.ProducerMonitor;
import dagger.producers.monitoring.ProducerToken;
import dagger.producers.monitoring.ProductionComponentMonitor;
import javax.annotation.Nullable;
import javax.inject.Inject;

class TracingProductionComponentMonitor extends ProductionComponentMonitor {

  static class Factory extends ProductionComponentMonitor.Factory {

    private final Tracing tracing;

    @Inject
    Factory(Tracing tracing) {
      this.tracing = tracing;
    }

    @Override
    public ProductionComponentMonitor create(Object component) {
      return new TracingProductionComponentMonitor(
          component.getClass().getSimpleName(), tracing.tracer());
    }
  }

  private final String componentName;
  private final Tracer tracer;

  TracingProductionComponentMonitor(String componentName, Tracer tracer) {
    this.componentName = componentName;
    this.tracer = tracer;
  }

  @Override
  public ProducerMonitor producerMonitorFor(ProducerToken token) {
    return new TracingProducerMonitor(componentName + "." + token.toString());
  }

  private class TracingProducerMonitor extends ProducerMonitor {

    private final String name;

    @Nullable private Span span;

    private TracingProducerMonitor(String name) {
      this.name = name;
    }

    @Override
    public void methodStarting() {
      if (tracer.currentSpan() == null) {
        return;
      }
      span = tracer.nextSpan().name(name).start();
    }

    @Override
    public void succeeded(Object value) {
      if (span != null) {
        span.tag("success", "true");
        span.finish();
      }
    }

    @Override
    public void failed(Throwable t) {
      if (span != null) {
        span.tag("success", "false");
        span.finish();
      }
    }
  }
}
