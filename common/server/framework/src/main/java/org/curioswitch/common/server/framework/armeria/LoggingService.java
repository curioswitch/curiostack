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

package org.curioswitch.common.server.framework.armeria;

import com.linecorp.armeria.common.Request;
import com.linecorp.armeria.common.Response;
import com.linecorp.armeria.common.logging.LogLevel;
import com.linecorp.armeria.common.logging.RequestLog;
import com.linecorp.armeria.common.logging.RequestLogAvailability;
import com.linecorp.armeria.server.Service;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingService;
import java.util.Objects;
import java.util.function.Function;
import org.slf4j.Logger;

public class LoggingService<I extends Request, O extends Response> extends SimpleDecoratingService<I, O> {
  private static final String REQUEST_FORMAT = "Request: {}";
  private static final String RESPONSE_FORMAT = "Response: {}";
  private final LogLevel level;

  public static <I extends Request, O extends Response> Function<Service<I, O>, LoggingService<I, O>> newDecorator() {
    return LoggingService::new;
  }

  public static <I extends Request, O extends Response> Function<Service<I, O>, LoggingService<I, O>> newDecorator(LogLevel level) {
    return (delegate) -> {
      return new LoggingService(delegate, level);
    };
  }

  public LoggingService(Service<I, O> delegate) {
    this(delegate, LogLevel.INFO);
  }

  public LoggingService(Service<I, O> delegate, LogLevel level) {
    super(delegate);
    this.level = (LogLevel)Objects.requireNonNull(level, "level");
  }

  public O serve(ServiceRequestContext ctx, I req) throws Exception {
    ctx.log().addListener(this::logRequest, RequestLogAvailability.REQUEST_END);
    ctx.log().addListener(this::logResponse, RequestLogAvailability.COMPLETE);
    return this.delegate().serve(ctx, req);
  }

  protected void logRequest(RequestLog log) {
    Logger logger = ((ServiceRequestContext)log.context()).logger();
    if (this.level.isEnabled(logger)) {
      this.level.log(logger, "Request: {}", log.toStringRequestOnly());
    }

  }

  protected void logResponse(RequestLog log) {
    Logger logger = ((ServiceRequestContext)log.context()).logger();
    if (this.level.isEnabled(logger)) {
      this.level.log(logger, "Response: {}", log.toStringResponseOnly());
    }

  }
}

