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

// Includes work from:
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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.jackson.XmlConstants;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;

// Mostly
// https://github.com/apache/logging-log4j2/blob/master/log4j-layout-jackson-json/src/main/java/org/apache/logging/log4j/jackson/json/layout/StackdriverJsonLayout.java
@Plugin(
    name = "StackdriverJsonLayout",
    category = Node.CATEGORY,
    elementType = Layout.ELEMENT_TYPE,
    printObject = true)
public final class StackdriverJsonLayout extends AbstractJacksonLayout {
  public static class Builder<B extends Builder<B>> extends AbstractJacksonLayout.Builder<B>
      implements org.apache.logging.log4j.core.util.Builder<StackdriverJsonLayout> {

    @PluginBuilderAttribute private boolean propertiesAsList;

    @PluginBuilderAttribute private boolean objectMessageAsJsonObject;

    @PluginElement("AdditionalField")
    private KeyValuePair[] additionalFields;

    public Builder() {
      super();
      setCharset(StandardCharsets.UTF_8);
    }

    @Override
    public StackdriverJsonLayout build() {
      final boolean encodeThreadContextAsList = isProperties() && propertiesAsList;
      final String headerPattern = toStringOrNull(getHeader());
      final String footerPattern = toStringOrNull(getFooter());
      return new StackdriverJsonLayout(
          getConfiguration(),
          isLocationInfo(),
          isProperties(),
          encodeThreadContextAsList,
          isComplete(),
          isCompact(),
          getEventEol(),
          headerPattern,
          footerPattern,
          getCharset(),
          isIncludeStacktrace(),
          isStacktraceAsString(),
          isIncludeNullDelimiter(),
          getAdditionalFields(),
          getObjectMessageAsJsonObject());
    }

    @Override
    public KeyValuePair[] getAdditionalFields() {
      return additionalFields;
    }

    public boolean getObjectMessageAsJsonObject() {
      return objectMessageAsJsonObject;
    }

    public boolean isPropertiesAsList() {
      return propertiesAsList;
    }

    @Override
    public B setAdditionalFields(final KeyValuePair[] additionalFields) {
      this.additionalFields = additionalFields;
      return asBuilder();
    }

    public B setObjectMessageAsJsonObject(final boolean objectMessageAsJsonObject) {
      this.objectMessageAsJsonObject = objectMessageAsJsonObject;
      return asBuilder();
    }

    public B setPropertiesAsList(final boolean propertiesAsList) {
      this.propertiesAsList = propertiesAsList;
      return asBuilder();
    }
  }

  @JsonRootName(XmlConstants.ELT_EVENT)
  public static class JsonLogEventWithAdditionalFields extends LogEventWithAdditionalFields {

    private final LogEvent event;

    public JsonLogEventWithAdditionalFields(
        final LogEvent logEvent, final Map<String, String> additionalFields) {
      super(logEvent, additionalFields);
      this.event = logEvent;
    }

    @JsonGetter
    public String getMessage() {
      ThrowableProxy throwProxy = event.getThrownProxy();
      if (throwProxy == null) {
        return event.getMessage().getFormattedMessage();
      } else {
        return event.getMessage().getFormattedMessage()
            + "\n"
            + throwProxy.getExtendedStackTraceAsString();
      }
    }

    @Override
    @JsonAnyGetter
    public Map<String, String> getAdditionalFields() {
      return super.getAdditionalFields();
    }

    @Override
    @JsonUnwrapped
    @JsonIgnoreProperties("message")
    public Object getLogEvent() {
      return super.getLogEvent();
    }
  }

  private static final String DEFAULT_FOOTER = "]";

  private static final String DEFAULT_HEADER = "[";

  static final String CONTENT_TYPE = "application/json";

  /**
   * Creates a JSON Layout using the default settings. Useful for testing.
   *
   * @return A JSON Layout.
   */
  public static StackdriverJsonLayout createDefaultLayout() {
    return new StackdriverJsonLayout(
        new DefaultConfiguration(),
        false,
        false,
        false,
        false,
        false,
        false,
        DEFAULT_HEADER,
        DEFAULT_FOOTER,
        StandardCharsets.UTF_8,
        true,
        false,
        false,
        null,
        false);
  }

  @PluginBuilderFactory
  public static <B extends Builder<B>> B newBuilder() {
    return new Builder<B>().asBuilder();
  }

  private StackdriverJsonLayout(
      final Configuration config,
      final boolean locationInfo,
      final boolean properties,
      final boolean encodeThreadContextAsList,
      final boolean complete,
      final boolean compact,
      final boolean eventEol,
      final String headerPattern,
      final String footerPattern,
      final Charset charset,
      final boolean includeStacktrace,
      final boolean stacktraceAsString,
      final boolean includeNullDelimiter,
      final KeyValuePair[] additionalFields,
      final boolean objectMessageAsJsonObject) {
    super(
        config,
        new JsonJacksonFactory(
                encodeThreadContextAsList,
                includeStacktrace,
                stacktraceAsString,
                objectMessageAsJsonObject)
            .newWriter(locationInfo, properties, compact),
        charset,
        compact,
        complete,
        eventEol,
        PatternLayout.newSerializerBuilder()
            .setConfiguration(config)
            .setPattern(headerPattern)
            .setDefaultPattern(DEFAULT_HEADER)
            .build(),
        PatternLayout.newSerializerBuilder()
            .setConfiguration(config)
            .setPattern(footerPattern)
            .setDefaultPattern(DEFAULT_FOOTER)
            .build(),
        includeNullDelimiter,
        additionalFields);
  }

  @Override
  protected LogEventWithAdditionalFields createLogEventWithAdditionalFields(
      final LogEvent event, final Map<String, String> additionalFieldsMap) {
    return new JsonLogEventWithAdditionalFields(event, additionalFieldsMap);
  }

  @Override
  public Map<String, String> getContentFormat() {
    final Map<String, String> result = new HashMap<>();
    result.put("version", "2.0");
    return result;
  }

  /**
   * @return The content type.
   */
  @Override
  public String getContentType() {
    return CONTENT_TYPE + "; charset=" + this.getCharset();
  }

  /**
   * Returns appropriate JSON footer.
   *
   * @return a byte array containing the footer, closing the JSON array.
   */
  @Override
  @Nullable
  public byte[] getFooter() {
    if (!this.complete) {
      return null;
    }
    final StringBuilder buf = new StringBuilder();
    buf.append(this.eol);
    final String str = serializeToString(getFooterSerializer());
    if (str != null) {
      buf.append(str);
    }
    buf.append(this.eol);
    return getBytes(buf.toString());
  }

  /**
   * Returns appropriate JSON header.
   *
   * @return a byte array containing the header, opening the JSON array.
   */
  @Override
  @Nullable
  public byte[] getHeader() {
    if (!this.complete) {
      return null;
    }
    final StringBuilder buf = new StringBuilder();
    final String str = serializeToString(getHeaderSerializer());
    if (str != null) {
      buf.append(str);
    }
    buf.append(this.eol);
    return getBytes(buf.toString());
  }

  @Override
  public void toSerializable(final LogEvent event, final Writer writer) throws IOException {
    if (complete && eventCount > 0) {
      writer.append(", ");
    }
    super.toSerializable(event, writer);
  }

  @Override
  protected Object wrapLogEvent(LogEvent event) {
    Object wrapped = super.wrapLogEvent(event);

    ImmutableMap.Builder<String, String> additional =
        ImmutableMap.builderWithExpectedSize(additionalFields.length + 1);
    additional.put("severity", stackdriverSeverity(event));

    if (wrapped instanceof LogEventWithAdditionalFields) {
      additional.putAll(((LogEventWithAdditionalFields) wrapped).getAdditionalFields());
    }

    return createLogEventWithAdditionalFields(event, additional.build());
  }

  private static String stackdriverSeverity(LogEvent event) {
    switch (event.getLevel().name()) {
      case "FATAL":
        return "CRITICAL";
      case "ERROR":
        return "ERROR";
      case "WARN":
        return "WARNING";
      case "INFO":
        return "INFO";
      case "DEBUG":
      case "TRACE":
        return "DEBUG";
      default:
        return "DEFAULT";
    }
  }
}
