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

import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import org.curioswitch.common.server.framework.database.DatabaseUtil;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;

/** Utilities for working with a mock database in tests. */
public final class DatabaseTestUtil {

  public static final DSLContext DB = DSL.using(SQLDialect.MYSQL);

  /**
   * Returns a {@link DSLContext} with a mock connection using the provided {@link
   * MockDataProvider}.
   */
  public static DSLContext newDbContext(MockDataProvider dataProvider) {
    MockConnection connection = new MockConnection(dataProvider);
    DSLContext db = DSL.using(connection, SQLDialect.MYSQL);
    db.configuration().set(DatabaseUtil.sfmRecordMapperProvider());
    db.settings().setRenderSchema(false);
    return db;
  }

  /**
   * Setup a {@link MockDataProviderStubber} which can be used to set an expectation to return
   * records or throw an exception when the {@code query} is executed on the {@code provider}.
   */
  public static MockDataProviderStubber whenQueried(MockDataProvider provider, String query) {
    return new MockDataProviderStubber(provider, query);
  }

  public static void verifyQueried(MockDataProvider provider, String query, Object... bindings) {
    try {
      verify(provider)
          .execute(
              argThat(
                  ctx -> {
                    if (!ctx.sql().equals(query)) {
                      return false;
                    }
                    assertThat(ctx.bindings())
                        .containsExactlyElementsOf(ImmutableList.copyOf(bindings));
                    return true;
                  }));
    } catch (SQLException e) {
      throw new IllegalStateException("Mock threw an exception.", e);
    }
  }

  public static class MockDataProviderStubber {
    private final MockDataProvider provider;
    private final String query;

    private MockDataProviderStubber(MockDataProvider provider, String query) {
      this.provider = provider;
      this.query = query;
    }

    /**
     * Sets the {@code records} to be returned when the query is called.
     */
    public void thenReturn(Record... records) {
      final MockResult result;
      if (records.length == 0) {
        result = new MockResult(0, DB.newResult());
      } else if (records.length == 1) {
        result = new MockResult(records[0]);
      } else {
        Result<Record> r = DB.newResult(records[0].fields());
        r.addAll(ImmutableList.copyOf(records));
        result = new MockResult(records.length, r);
      }
      try {
        doReturn(new MockResult[] {result})
            .when(provider)
            .execute(argThat(ctx -> ctx.sql().equals(query)));
      } catch (SQLException e) {
        throw new IllegalStateException("Mock threw an exception.", e);
      }
    }

    /**
     * Sets the {@link Throwable} to be thrown when the query is called.
     */
    public void thenThrow(Throwable t) {
      try {
        doThrow(t).when(provider).execute(argThat(ctx -> ctx.sql().equals(query)));
      } catch (SQLException e) {
        throw new IllegalStateException("Mock threw an exception.", e);
      }
    }
  }

  private DatabaseTestUtil() {}
}
