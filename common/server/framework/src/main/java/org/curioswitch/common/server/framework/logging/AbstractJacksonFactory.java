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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

// https://raw.githubusercontent.com/apache/logging-log4j2/master/log4j-layout-jackson/src/main/java/org/apache/logging/log4j/jackson/AbstractJacksonFactory.java
abstract class AbstractJacksonFactory {
  protected final boolean includeStacktrace;

  protected final boolean stacktraceAsString;

  public AbstractJacksonFactory(final boolean includeStacktrace, final boolean stacktraceAsString) {
    super();
    this.includeStacktrace = includeStacktrace;
    this.stacktraceAsString = stacktraceAsString;
  }

  protected abstract String getPropertyNameForContextMap();

  protected abstract String getPropertyNameForNanoTime();

  protected abstract String getPropertyNameForSource();

  protected abstract String getPropertyNameForStackTrace();

  protected abstract PrettyPrinter newCompactPrinter();

  protected abstract ObjectMapper newObjectMapper();

  protected abstract PrettyPrinter newPrettyPrinter();

  public ObjectWriter newWriter(
      final boolean locationInfo, final boolean properties, final boolean compact) {
    final SimpleFilterProvider filters = new SimpleFilterProvider();
    final Set<String> except = new HashSet<>(4);
    if (!locationInfo) {
      except.add(this.getPropertyNameForSource());
    }
    if (!properties) {
      except.add(this.getPropertyNameForContextMap());
    }
    if (!includeStacktrace) {
      except.add(this.getPropertyNameForStackTrace());
    }
    except.add(this.getPropertyNameForNanoTime());
    filters.addFilter(
        Log4jLogEvent.class.getName(), SimpleBeanPropertyFilter.serializeAllExcept(except));
    final ObjectWriter writer =
        this.newObjectMapper().writer(compact ? this.newCompactPrinter() : this.newPrettyPrinter());
    return writer.with(filters);
  }
}
