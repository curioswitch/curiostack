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

package org.curioswitch.common.server.framework.logging;

import dagger.producers.monitoring.ProducerMonitor;
import dagger.producers.monitoring.ProducerToken;
import dagger.producers.monitoring.ProductionComponentMonitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class LoggingProductionComponentMonitor extends ProductionComponentMonitor {

  private static final Logger logger = LogManager.getLogger();

  static class Factory extends ProductionComponentMonitor.Factory {

    @Override
    public ProductionComponentMonitor create(Object component) {
      return new LoggingProductionComponentMonitor(component.getClass().getSimpleName());
    }
  }

  private final String componentName;

  private LoggingProductionComponentMonitor(String componentName) {
    this.componentName = componentName;
  }

  @Override
  public ProducerMonitor producerMonitorFor(ProducerToken token) {
    return new LoggingProducerMonitor(componentName, token);
  }

  private static class LoggingProducerMonitor extends ProducerMonitor {
    private final String componentName;
    private final String token;

    private LoggingProducerMonitor(String componentName, ProducerToken token) {
      this.componentName = componentName;
      this.token = token.toString();
    }

    @Override
    public void methodStarting() {
      logger.info("Starting: {} {}", componentName, token);
    }

    @Override
    public void methodFinished() {
      logger.info("Finishing: {} {}", componentName, token);
    }

    @Override
    public void succeeded(Object value) {
      logger.info("Succeeded: {} {}", componentName, token);
    }

    @Override
    public void failed(Throwable t) {
      logger.warn("Failed: {} {}", componentName, token, t);
    }
  }
}
