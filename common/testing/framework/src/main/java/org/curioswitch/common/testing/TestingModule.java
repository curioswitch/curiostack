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
package org.curioswitch.common.testing;

import static org.mockito.Mockito.mock;

import dagger.Module;
import dagger.Provides;
import org.curioswitch.common.server.framework.logging.RequestLoggingContext;
import org.curioswitch.common.testing.database.DatabaseTestingModule;

/**
 * A module providing mock implementations of commonly used dependencies. In general, it should be
 * sufficient to only include {@link TestingModule} even if you don't use all the modules specified
 * in {@code includes} because they will be ignored.
 */
@Module(includes = {DatabaseTestingModule.class})
public abstract class TestingModule {

  @Provides
  static RequestLoggingContext requestLoggingContext() {
    return mock(RequestLoggingContext.class);
  }

  private TestingModule() {}
}
