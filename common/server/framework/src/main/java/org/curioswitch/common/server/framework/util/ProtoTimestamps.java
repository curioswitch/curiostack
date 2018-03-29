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
package org.curioswitch.common.server.framework.util;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

/**
 * Common utilities for dealing with {@link Timestamp}, such as conversion to {@link java.time}
 * types.
 */
public final class ProtoTimestamps {

  /** Converts a {@link Timestamp} to {@link LocalDateTime}. */
  public static LocalDateTime toLocalDateTime(Timestamp timestamp, ZoneOffset zone) {
    return LocalDateTime.ofEpochSecond(Timestamps.toSeconds(timestamp), timestamp.getNanos(), zone);
  }

  /** Converts a {@link LocalDateTime} to {@link Timestamp}. */
  public static Timestamp fromLocalDateTime(LocalDateTime localDateTime, ZoneOffset zone) {
    return Timestamps.fromNanos(
        TimeUnit.SECONDS.toNanos(localDateTime.toEpochSecond(zone)) + localDateTime.getNano());
  }

  /**
   * Converts a {@link Timestamp} to {@link LocalDate}. This will be the date that includes the
   * {@link Timestamp}.
   */
  public static LocalDate toLocalDate(Timestamp timestamp, ZoneOffset zone) {
    return toLocalDateTime(timestamp, zone).toLocalDate();
  }

  /**
   * Converts a {@link LocalDate} to {@link Timestamp}. This will be the {@link Timestamp} at the
   * start of the day.
   */
  public static Timestamp fromLocalDate(LocalDate localDate, ZoneOffset zone) {
    return fromLocalDateTime(localDate.atStartOfDay(), zone);
  }

  /** Converts a {@link Timestamp} to {@link Instant}. */
  public static Instant toInstant(Timestamp timestamp) {
    return Instant.ofEpochSecond(Timestamps.toSeconds(timestamp), timestamp.getNanos());
  }

  /** Converts a {@link Instant} to {@link Timestamp}. */
  public static Timestamp fromInstant(Instant instant) {
    return Timestamps.fromNanos(
        TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano());
  }

  private ProtoTimestamps() {}
}
