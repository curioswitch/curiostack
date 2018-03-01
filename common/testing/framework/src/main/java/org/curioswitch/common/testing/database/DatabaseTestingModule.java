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
package org.curioswitch.common.testing.database;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import dagger.Module;
import dagger.Provides;
import dagger.producers.Production;
import java.util.concurrent.Executor;
import org.curioswitch.common.server.framework.database.ForDatabase;
import org.jooq.DSLContext;
import org.jooq.tools.jdbc.MockDataProvider;

@Module
public abstract class DatabaseTestingModule {

  @Provides
  @ForDatabase
  static ListeningExecutorService dbExecutor() {
    return MoreExecutors.listeningDecorator(MoreExecutors.newDirectExecutorService());
  }

  @Provides
  @Production
  static Executor executor() {
    return MoreExecutors.directExecutor();
  }

  @Provides
  static DSLContext db(MockDataProvider dataProvider) {
    return DatabaseTestUtil.newDbContext(dataProvider);
  }
}
