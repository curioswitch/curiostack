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

import java.time.Duration;
import org.curioswitch.common.server.framework.immutables.JavaBeanStyle;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;

/** Configuration properties for a database accessed by the server. */
@Immutable
@Modifiable
@JavaBeanStyle
public interface DatabaseConfig {

  /** The JDBC connection URL to connect to. */
  String getJdbcUrl();

  /** The username to use to connect to the database. */
  String getUsername();

  /** The password to use to connect to the database. */
  String getPassword();

  /**
   * The duration that a connection is allowed to be out of the pool before being considered leaked.
   * 0 means no leak detection.
   */
  Duration getLeakDetectionThreshold();

  /** Whether to log all queries to INFO level. */
  boolean getLogQueries();

  /**
   * The max lifetime for database connections. Should be less than the wait_timeout setting in the
   * DB itself.
   */
  Duration getConnectionMaxLifetime();

  /**
   * The timeout for socket connect. Defaults to 0, no timeout. Setting this to a relatively low
   * value may be needed to support automatic database failover.
   */
  Duration getConnectTimeout();

  /**
   * The timeout for socket read. Defaults to 0, no timeout. Setting this to a relatively low value
   * may be needed to support automatic database failover.
   */
  Duration getSocketTimeout();
}
