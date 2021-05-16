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

// Includes work from:
// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.curioswitch.common.protobuf.json;

import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** Utilities extracted from protobuf-java-util module. */
final class ProtobufUtil {

  // Visible for testing
  static final long DURATION_SECONDS_MIN = -315576000000L;
  // Visible for testing
  static final long DURATION_SECONDS_MAX = 315576000000L;

  private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);
  private static final long NANOS_PER_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1);
  private static final long NANOS_PER_MICROSECOND = TimeUnit.MICROSECONDS.toNanos(1);

  private static final long MILLIS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);

  private static final String FIELD_PATH_SEPARATOR = ",";

  private static final ThreadLocal<SimpleDateFormat> timestampFormat =
      ThreadLocal.withInitial(ProtobufUtil::createTimestampFormat);

  // TODO(chokoswitch): Switch to java.time
  private static SimpleDateFormat createTimestampFormat() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
    GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    // We use Proleptic Gregorian Calendar (i.e., Gregorian calendar extends
    // backwards to year one) for timestamp formatting.
    calendar.setGregorianChange(new Date(Long.MIN_VALUE));
    sdf.setCalendar(calendar);
    return sdf;
  }

  static String formatTimestamp(Timestamp timestamp) {
    checkValid(timestamp);

    long seconds = timestamp.getSeconds();
    int nanos = timestamp.getNanos();

    StringBuilder result = new StringBuilder();
    // Format the seconds part.
    Date date = new Date(seconds * MILLIS_PER_SECOND);
    result.append(timestampFormat.get().format(date));
    // Format the nanos part.
    if (nanos != 0) {
      result.append(".");
      result.append(formatNanos(nanos));
    }
    result.append("Z");
    return result.toString();
  }

  static Timestamp parseTimestamp(String value) throws ParseException {
    int dayOffset = value.indexOf('T');
    if (dayOffset == -1) {
      throw new ParseException("Failed to parse timestamp: invalid timestamp \"" + value + "\"", 0);
    }
    int timezoneOffsetPosition = value.indexOf('Z', dayOffset);
    if (timezoneOffsetPosition == -1) {
      timezoneOffsetPosition = value.indexOf('+', dayOffset);
    }
    if (timezoneOffsetPosition == -1) {
      timezoneOffsetPosition = value.indexOf('-', dayOffset);
    }
    if (timezoneOffsetPosition == -1) {
      throw new ParseException("Failed to parse timestamp: missing valid timezone offset.", 0);
    }
    // Parse seconds and nanos.
    String timeValue = value.substring(0, timezoneOffsetPosition);
    String secondValue = timeValue;
    String nanoValue = "";
    int pointPosition = timeValue.indexOf('.');
    if (pointPosition != -1) {
      secondValue = timeValue.substring(0, pointPosition);
      nanoValue = timeValue.substring(pointPosition + 1);
    }
    Date date = timestampFormat.get().parse(secondValue);
    long seconds = date.getTime() / MILLIS_PER_SECOND;
    int nanos = nanoValue.isEmpty() ? 0 : parseNanos(nanoValue);
    // Parse timezone offsets.
    if (value.charAt(timezoneOffsetPosition) == 'Z') {
      if (value.length() != timezoneOffsetPosition + 1) {
        throw new ParseException(
            "Failed to parse timestamp: invalid trailing data \""
                + value.substring(timezoneOffsetPosition)
                + "\"",
            0);
      }
    } else {
      String offsetValue = value.substring(timezoneOffsetPosition + 1);
      long offset = parseTimezoneOffset(offsetValue);
      if (value.charAt(timezoneOffsetPosition) == '+') {
        seconds -= offset;
      } else {
        seconds += offset;
      }
    }
    try {
      return normalizedTimestamp(seconds, nanos);
    } catch (IllegalArgumentException e) {
      throw new ParseException("Failed to parse timestamp: timestamp is out of range.", 0);
    }
  }

  static String formatDuration(Duration duration) {
    checkValid(duration);

    long seconds = duration.getSeconds();
    int nanos = duration.getNanos();

    StringBuilder result = new StringBuilder();
    if (seconds < 0 || nanos < 0) {
      result.append("-");
      seconds = -seconds;
      nanos = -nanos;
    }
    result.append(seconds);
    if (nanos != 0) {
      result.append(".");
      result.append(formatNanos(nanos));
    }
    result.append("s");
    return result.toString();
  }

  static Duration parseDuration(String value) throws ParseException {
    // Must ended with "s".
    if (value.isEmpty() || value.charAt(value.length() - 1) != 's') {
      throw new ParseException("Invalid duration string: " + value, 0);
    }
    boolean negative = false;
    if (value.charAt(0) == '-') {
      negative = true;
      value = value.substring(1);
    }
    String secondValue = value.substring(0, value.length() - 1);
    String nanoValue = "";
    int pointPosition = secondValue.indexOf('.');
    if (pointPosition != -1) {
      nanoValue = secondValue.substring(pointPosition + 1);
      secondValue = secondValue.substring(0, pointPosition);
    }
    long seconds = Long.parseLong(secondValue);
    int nanos = nanoValue.isEmpty() ? 0 : parseNanos(nanoValue);
    if (seconds < 0) {
      throw new ParseException("Invalid duration string: " + value, 0);
    }
    if (negative) {
      seconds = -seconds;
      nanos = -nanos;
    }
    try {
      return normalizedDuration(seconds, nanos);
    } catch (IllegalArgumentException e) {
      throw new ParseException("Duration value is out of range.", 0);
    }
  }

  static String fieldMaskToJson(FieldMask fieldMask) {
    StringBuilder out = new StringBuilder();
    for (String path : fieldMask.getPathsList()) {
      if (path.isEmpty()) {
        continue;
      }
      out.append(snakeToCamel(path));
      out.append(FIELD_PATH_SEPARATOR);
    }
    if (out.length() > 0) {
      // Strip trailing comma
      out.setLength(out.length() - 1);
    }
    return out.toString();
  }

  static FieldMask fieldMaskFromJson(String s) {
    FieldMask.Builder builder = FieldMask.newBuilder();
    int i = 0;
    int j = -1;
    while ((j = s.indexOf(FIELD_PATH_SEPARATOR, ++j)) != -1) {
      if (i == j) {
        continue;
      }
      builder.addPaths(camelToSnake(s.substring(i, j)));
      i = j + 1;
    }
    if (i < s.length()) {
      builder.addPaths(camelToSnake(s.substring(i)));
    }
    return builder.build();
  }

  // Visible for testing
  static String snakeToCamel(String s) {
    StringBuilder out = null;
    int i = 0;
    int j = -1;
    while ((j = s.indexOf('_', ++j)) != -1) {
      if (i == 0) {
        out = new StringBuilder(s.length());
        if (i != j) {
          out.append(toLowerCase(s.charAt(i)));
          out.append(s, i + 1, j);
        }
      } else {
        if (i != j) {
          out.append(toUpperCase(s.charAt(i)));
          out.append(s, i + 1, j);
        }
      }
      i = j + 1;
    }
    if (i == 0) {
      return s.toLowerCase(Locale.ROOT);
    }
    if (i < s.length()) {
      out.append(toUpperCase(s.charAt(i)));
      out.append(s, i + 1, s.length());
    }
    return out.toString();
  }

  // Visible for testing
  static String camelToSnake(String s) {
    StringBuilder out = null;
    int i = 0;
    for (int j = 0; j < s.length(); j++) {
      char c = s.charAt(j);
      if (c < 'A' || c > 'Z') {
        continue;
      }
      if (i == 0) {
        // include some extra space for separators
        out = new StringBuilder(s.length() + 4);
      }
      if (i != j) {
        out.append(toLowerCase(s.charAt(i)));
        out.append(s, i + 1, j);
      }
      out.append('_');
      i = j;
    }
    if (i == 0) {
      return s.toLowerCase(Locale.ROOT);
    }
    out.append(toLowerCase(s.charAt(i)));
    out.append(s, i + 1, s.length());
    return out.toString();
  }

  private static char toLowerCase(char c) {
    if (c < 'A' || c > 'Z') {
      return c;
    }
    return (char) (c - ('A' - 'a'));
  }

  private static char toUpperCase(char c) {
    if (c < 'a' || c > 'z') {
      return c;
    }
    return (char) (c + ('A' - 'a'));
  }

  @SuppressWarnings("NarrowingCompoundAssignment")
  private static Duration normalizedDuration(long seconds, int nanos) {
    if (nanos <= -NANOS_PER_SECOND || nanos >= NANOS_PER_SECOND) {
      seconds = checkedAdd(seconds, nanos / NANOS_PER_SECOND);
      nanos %= NANOS_PER_SECOND;
    }
    if (seconds > 0 && nanos < 0) {
      nanos += NANOS_PER_SECOND; // no overflow since nanos is negative (and we're adding)
      seconds--; // no overflow since seconds is positive (and we're decrementing)
    }
    if (seconds < 0 && nanos > 0) {
      nanos -= NANOS_PER_SECOND; // no overflow since nanos is positive (and we're subtracting)
      seconds++; // no overflow since seconds is negative (and we're incrementing)
    }
    Duration duration = Duration.newBuilder().setSeconds(seconds).setNanos(nanos).build();
    return checkValid(duration);
  }

  private static boolean isValid(long seconds, int nanos) {
    if (seconds < DURATION_SECONDS_MIN || seconds > DURATION_SECONDS_MAX) {
      return false;
    }
    if (nanos < -999999999L || nanos >= NANOS_PER_SECOND) {
      return false;
    }
    if (seconds < 0 || nanos < 0) {
      if (seconds > 0 || nanos > 0) {
        return false;
      }
    }
    return true;
  }

  static String formatNanos(int nanos) {
    // Determine whether to use 3, 6, or 9 digits for the nano part.
    if (nanos % NANOS_PER_MILLISECOND == 0) {
      return String.format(Locale.ENGLISH, "%1$03d", nanos / NANOS_PER_MILLISECOND);
    } else if (nanos % NANOS_PER_MICROSECOND == 0) {
      return String.format(Locale.ENGLISH, "%1$06d", nanos / NANOS_PER_MICROSECOND);
    } else {
      return String.format(Locale.ENGLISH, "%1$09d", nanos);
    }
  }

  private static int parseNanos(String value) throws ParseException {
    int result = 0;
    for (int i = 0; i < 9; ++i) {
      result = result * 10;
      if (i < value.length()) {
        if (value.charAt(i) < '0' || value.charAt(i) > '9') {
          throw new ParseException("Invalid nanoseconds.", 0);
        }
        result += value.charAt(i) - '0';
      }
    }
    return result;
  }

  private static long parseTimezoneOffset(String value) throws ParseException {
    int pos = value.indexOf(':');
    if (pos == -1) {
      throw new ParseException("Invalid offset value: " + value, 0);
    }
    String hours = value.substring(0, pos);
    String minutes = value.substring(pos + 1);
    return (Long.parseLong(hours) * 60 + Long.parseLong(minutes)) * 60;
  }

  private static Timestamp normalizedTimestamp(long seconds, int nanos) {
    if (nanos <= -NANOS_PER_SECOND || nanos >= NANOS_PER_SECOND) {
      seconds = checkedAdd(seconds, nanos / NANOS_PER_SECOND);
      nanos = (int) (nanos % NANOS_PER_SECOND);
    }
    if (nanos < 0) {
      nanos =
          (int)
              (nanos + NANOS_PER_SECOND); // no overflow since nanos is negative (and we're adding)
      seconds = checkedSubtract(seconds, 1);
    }
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build();
    return checkValid(timestamp);
  }

  private static Duration checkValid(Duration duration) {
    long seconds = duration.getSeconds();
    int nanos = duration.getNanos();
    if (!isValid(seconds, nanos)) {
      throw new IllegalArgumentException(
          String.format(
              "Duration is not valid. See proto definition for valid values. "
                  + "Seconds (%s) must be in range [-315,576,000,000, +315,576,000,000]. "
                  + "Nanos (%s) must be in range [-999,999,999, +999,999,999]. "
                  + "Nanos must have the same sign as seconds",
              seconds, nanos));
    }
    return duration;
  }

  private static Timestamp checkValid(Timestamp timestamp) {
    long seconds = timestamp.getSeconds();
    int nanos = timestamp.getNanos();
    if (!isValid(seconds, nanos)) {
      throw new IllegalArgumentException(
          String.format(
              "Timestamp is not valid. See proto definition for valid values. "
                  + "Seconds (%s) must be in range [-62,135,596,800, +253,402,300,799]. "
                  + "Nanos (%s) must be in range [0, +999,999,999].",
              seconds, nanos));
    }
    return timestamp;
  }

  @SuppressWarnings("ShortCircuitBoolean")
  private static long checkedAdd(long a, long b) {
    long result = a + b;
    if ((a ^ b) < 0 | (a ^ result) >= 0) {
      return result;
    }
    throw new ArithmeticException("overflow: checkedAdd(" + a + ", " + b + ")");
  }

  @SuppressWarnings("ShortCircuitBoolean")
  private static long checkedSubtract(long a, long b) {
    long result = a - b;
    if ((a ^ b) >= 0 | (a ^ result) >= 0) {
      return result;
    }
    throw new ArithmeticException("overflow: checkedSubtract(" + a + ", " + b + ")");
  }

  private ProtobufUtil() {}
}
