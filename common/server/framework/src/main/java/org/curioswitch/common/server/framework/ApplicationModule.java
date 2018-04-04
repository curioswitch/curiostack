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
package org.curioswitch.common.server.framework;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dagger.Module;
import dagger.Provides;
import java.time.Clock;
import javax.inject.Singleton;

/**
 * A {@link Module} which bootstraps a generic Java application. While most users will use {@link
 * ServerModule} to bootstrap a server, this {@link Module} can be useful when some server logic is
 * reused inside a non-server app, such as a batch job. At some point, these may be separated into
 * separate artifacts.
 */
@Module
public abstract class ApplicationModule {

  static {
    // Optimistically hope that this module is loaded very early to make sure java.util.Logger uses
    // the bridge to avoid forcing users to specify a system property. They can still do so for more
    // complete JUL coverage.
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
  }

  @Provides
  @Singleton
  static Config config() {
    return ConfigFactory.load();
  }

  /**
   * The default {@link Clock} to use in an application. The use of {@link Clock#systemUTC()} should
   * be reasonable for all users since any time-zone dependent code should use an explicit time zone
   * instead.
   */
  @Provides
  static Clock clock() {
    return Clock.systemUTC();
  }

  private ApplicationModule() {}
}
