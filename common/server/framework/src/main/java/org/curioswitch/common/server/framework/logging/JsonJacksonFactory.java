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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.curioswitch.common.server.framework.logging;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.jackson.JsonConstants;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;

final class JsonJacksonFactory extends AbstractJacksonFactory {
  private final boolean encodeThreadContextAsList;
  private final boolean objectMessageAsJsonObject;

  public JsonJacksonFactory(
      final boolean encodeThreadContextAsList,
      final boolean includeStacktrace,
      final boolean stacktraceAsString,
      final boolean objectMessageAsJsonObject) {
    super(includeStacktrace, stacktraceAsString);
    this.encodeThreadContextAsList = encodeThreadContextAsList;
    this.objectMessageAsJsonObject = objectMessageAsJsonObject;
  }

  @Override
  protected String getPropertyNameForContextMap() {
    return JsonConstants.ELT_CONTEXT_MAP;
  }

  @Override
  protected String getPropertyNameForNanoTime() {
    return JsonConstants.ELT_NANO_TIME;
  }

  @Override
  protected String getPropertyNameForSource() {
    return JsonConstants.ELT_SOURCE;
  }

  @Override
  protected String getPropertyNameForStackTrace() {
    return JsonConstants.ELT_EXTENDED_STACK_TRACE;
  }

  @Override
  protected PrettyPrinter newCompactPrinter() {
    return new MinimalPrettyPrinter();
  }

  @Override
  protected ObjectMapper newObjectMapper() {
    return new Log4jJsonObjectMapper(
        encodeThreadContextAsList,
        includeStacktrace,
        stacktraceAsString,
        objectMessageAsJsonObject);
  }

  @Override
  protected PrettyPrinter newPrettyPrinter() {
    return new DefaultPrettyPrinter();
  }
}
