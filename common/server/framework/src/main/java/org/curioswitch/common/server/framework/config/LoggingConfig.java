/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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
package org.curioswitch.common.server.framework.config;

import java.util.List;
import org.curioswitch.common.server.framework.immutables.JavaBeanStyle;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;

/** Configuration related to logging. */
@Immutable
@Modifiable
@JavaBeanStyle
public interface LoggingConfig {

  /**
   * Whether to do fine-grained logging of individual producer function execution. This creates a
   * lot of output and generally should only be enabled when debugging a specific issue.
   */
  boolean getLogProducerExecution();

  /**
   * Whether to do fine-grained tracing of individual producer function execution. This can cause
   * significant overhead and should be used with care.
   */
  boolean getTraceProducerExecution();

  /**
   * Header names that will not be logged to request logs. By default, AUTHORIZATION is not logged.
   */
  List<String> getBlacklistedRequestHeaders();

  /** Header names that will not be logged to response logs. */
  List<String> getBlacklistedResponseHeaders();

  /** Whether to log content in request logs. */
  boolean getLogRequestContent();

  /** Whether to log content in response logs. */
  boolean getLogResponseContent();

  /** Whether to log all server requests. By default, only unsuccessful requests are logged. */
  boolean getLogAllServerRequests();

  /**
   * Whether to log all client requests. When full request logging is desired, it is generally
   * sufficient to only enable {@code logAllServerRequests} without also logging client requests. By
   * default, only unsuccessful requests are logged
   */
  boolean getLogAllClientRequests();
}
