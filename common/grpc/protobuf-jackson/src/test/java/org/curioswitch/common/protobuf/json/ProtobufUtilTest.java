/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
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

package org.curioswitch.common.protobuf.json;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;

import com.google.common.base.CaseFormat;
import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import java.text.ParseException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProtobufUtilTest {

  // Timestamp for "0001-01-01T00:00:00Z"
  private static final long TIMESTAMP_SECONDS_MIN = -62135596800L;

  // Timestamp for "9999-12-31T23:59:59Z"
  private static final long TIMESTAMP_SECONDS_MAX = 253402300799L;

  @Test
  void testDurationStringFormat() throws Exception {
    // Generated output should contain 3, 6, or 9 fractional digits.
    Duration duration = Duration.newBuilder().setSeconds(1).build();
    assertThat(ProtobufUtil.formatDuration(duration)).isEqualTo("1s");
    duration = Duration.newBuilder().setNanos(10000000).build();
    assertThat(ProtobufUtil.formatDuration(duration)).isEqualTo("0.010s");
    duration = Duration.newBuilder().setNanos(10000).build();
    assertThat(ProtobufUtil.formatDuration(duration)).isEqualTo("0.000010s");
    duration = Duration.newBuilder().setNanos(10).build();
    assertThat(ProtobufUtil.formatDuration(duration)).isEqualTo("0.000000010s");

    // Parsing accepts an fractional digits as long as they fit into nano
    // precision.
    duration = ProtobufUtil.parseDuration("0.1s");
    assertThat(duration.getNanos()).isEqualTo(100000000);
    duration = ProtobufUtil.parseDuration("0.0001s");
    assertThat(duration.getNanos()).isEqualTo(100000);
    duration = ProtobufUtil.parseDuration("0.0000001s");
    assertThat(duration.getNanos()).isEqualTo(100);

    // Duration must support range from -315,576,000,000s to +315576000000s
    // which includes negative values.
    duration = ProtobufUtil.parseDuration("315576000000.999999999s");
    assertThat(duration.getSeconds()).isEqualTo(315576000000L);
    assertThat(duration.getNanos()).isEqualTo(999999999);
    duration = ProtobufUtil.parseDuration("-315576000000.999999999s");
    assertThat(duration.getSeconds()).isEqualTo(-315576000000L);
    assertThat(duration.getNanos()).isEqualTo(-999999999);
  }

  @ParameterizedTest
  @ArgumentsSource(InvalidDurationsSource.class)
  void testInvalidDurationFormat(Duration value) {
    assertThatThrownBy(() -> ProtobufUtil.formatDuration(value))
        .isInstanceOf(IllegalArgumentException.class);
  }

  static class InvalidDurationsSource implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
              // Value too small
              Duration.newBuilder().setSeconds(ProtobufUtil.DURATION_SECONDS_MIN - 1).build(),
              // Value too large
              Duration.newBuilder().setSeconds(ProtobufUtil.DURATION_SECONDS_MAX + 1).build(),
              // Invalid nanos value.
              Duration.newBuilder().setSeconds(1).setNanos(-1).build(),
              // Invalid nanos value.
              Duration.newBuilder().setSeconds(-1).setNanos(1).build())
          .map(Arguments::of);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // Value too small
        "-315576000001s",
        // Value too large.
        "315576000001s",
        // Empty.
        "",
        // Missing "s",
        "0",
        // Invalid trailing data.
        "0s0",
        // Invalid prefix.
        "--1s",
      })
  void testInvalidDurationStrings(String value) {
    assertThatThrownBy(() -> ProtobufUtil.parseDuration(value)).isInstanceOf(ParseException.class);
  }

  @Test
  void testTimestampStringFormat() throws Exception {
    Timestamp start = ProtobufUtil.parseTimestamp("0001-01-01T00:00:00Z");
    Timestamp end = ProtobufUtil.parseTimestamp("9999-12-31T23:59:59.999999999Z");
    assertThat(start.getSeconds()).isEqualTo(TIMESTAMP_SECONDS_MIN);
    assertThat(start.getNanos()).isEqualTo(0);
    assertThat(end.getSeconds()).isEqualTo(TIMESTAMP_SECONDS_MAX);
    assertThat(end.getNanos()).isEqualTo(999999999);
    assertThat(ProtobufUtil.formatTimestamp(start)).isEqualTo("0001-01-01T00:00:00Z");
    assertThat(ProtobufUtil.formatTimestamp(end)).isEqualTo("9999-12-31T23:59:59.999999999Z");

    Timestamp value = ProtobufUtil.parseTimestamp("1970-01-01T00:00:00Z");
    assertThat(value.getSeconds()).isEqualTo(0);
    assertThat(value.getNanos()).isEqualTo(0);

    // Test that 3, 6, or 9 digits are used for the fractional part.
    value = Timestamp.newBuilder().setNanos(10).build();
    assertThat(ProtobufUtil.formatTimestamp(value)).isEqualTo("1970-01-01T00:00:00.000000010Z");
    value = Timestamp.newBuilder().setNanos(10000).build();
    assertThat(ProtobufUtil.formatTimestamp(value)).isEqualTo("1970-01-01T00:00:00.000010Z");
    value = Timestamp.newBuilder().setNanos(10000000).build();
    assertThat(ProtobufUtil.formatTimestamp(value)).isEqualTo("1970-01-01T00:00:00.010Z");
  }

  @ParameterizedTest
  @ArgumentsSource(InvalidTimestampsSource.class)
  void testTimestampInvalidStringFormat(Timestamp value) {
    assertThatThrownBy(() -> ProtobufUtil.formatTimestamp(value))
        .isInstanceOf(IllegalArgumentException.class);
  }

  static class InvalidTimestampsSource implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
              // Invalid nanos value.
              Timestamp.newBuilder().setNanos(1000000000).build())
          .map(Arguments::of);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // Missing 'T'.
        "1970-01-01 00:00:00Z",
        // Missing 'Z'.
        "1970-01-01T00:00:00",
        // Invalid offset.
        "1970-01-01T00:00:00+0000",
        // Trailing text.
        "1970-01-01T00:00:00Z0",
        // Invalid nanosecond value.
        "1970-01-01T00:00:00.ABCZ",
      })
  void testInvalidTimestampStringParse(String value) {
    assertThatThrownBy(() -> ProtobufUtil.parseTimestamp(value)).isInstanceOf(ParseException.class);
  }

  @Test
  void testToJsonString() {
    FieldMask mask = FieldMask.getDefaultInstance();
    assertThat(ProtobufUtil.fieldMaskToJson(mask)).isEqualTo("");
    mask = FieldMask.newBuilder().addPaths("foo").build();
    assertThat(ProtobufUtil.fieldMaskToJson(mask)).isEqualTo("foo");
    mask = FieldMask.newBuilder().addPaths("foo.bar_baz").addPaths("").build();
    assertThat(ProtobufUtil.fieldMaskToJson(mask)).isEqualTo("foo.barBaz");
    mask = FieldMask.newBuilder().addPaths("foo").addPaths("bar_baz").build();
    assertThat(ProtobufUtil.fieldMaskToJson(mask)).isEqualTo("foo,barBaz");
  }

  @Test
  void testFromJsonString() {
    FieldMask mask = ProtobufUtil.fieldMaskFromJson("");
    assertThat(mask.getPathsCount()).isEqualTo(0);
    mask = ProtobufUtil.fieldMaskFromJson("foo");
    assertThat(mask.getPathsCount()).isEqualTo(1);
    assertThat(mask.getPaths(0)).isEqualTo("foo");
    mask = ProtobufUtil.fieldMaskFromJson("foo.barBaz");
    assertThat(mask.getPathsCount()).isEqualTo(1);
    assertThat(mask.getPaths(0)).isEqualTo("foo.bar_baz");
    mask = ProtobufUtil.fieldMaskFromJson("foo,barBaz");
    assertThat(mask.getPathsCount()).isEqualTo(2);
    assertThat(mask.getPaths(0)).isEqualTo("foo");
    assertThat(mask.getPaths(1)).isEqualTo("bar_baz");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "a",
        "abc",
        "abc_",
        "Abc",
        "abc_bdef",
        "abc__bdef",
        "abc_bdef_ghi",
        "Abc_bdef_ghi",
        "_",
        "____",
        "_abc",
        "_abc_",
        "_abc_d"
      })
  void snakeToCamel(String value) {
    String camel = ProtobufUtil.snakeToCamel(value);
    assertThat(camel).isEqualTo(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, value));

    assertThat(ProtobufUtil.camelToSnake(camel))
        .isEqualTo(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, camel));
  }
}
