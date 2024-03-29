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

package org.curioswitch.common.server.framework.database;

import org.curioswitch.common.server.framework.database.smf.CurioReflectionService;
import org.simpleflatmapper.jooq.JooqMapperFactory;
import org.simpleflatmapper.jooq.SfmRecordMapperProvider;

/** Utilities for working with databases. */
public final class DatabaseUtil {

  // Make singleton to allow better code generation.
  private static final SfmRecordMapperProvider MAPPER_PROVIDER =
      JooqMapperFactory.newInstance()
          .ignorePropertyNotFound()
          .reflectionService(CurioReflectionService.newInstance())
          .newRecordMapperProvider();

  /**
   * Returns a {@link SfmRecordMapperProvider} configured to allow missing properties, which are
   * common when mapping from DB objects to business logic objects.
   */
  public static SfmRecordMapperProvider sfmRecordMapperProvider() {
    return MAPPER_PROVIDER;
  }

  private DatabaseUtil() {}
}
