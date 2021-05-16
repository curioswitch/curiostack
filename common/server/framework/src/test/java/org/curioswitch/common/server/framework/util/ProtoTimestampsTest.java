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

package org.curioswitch.common.server.framework.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ClassCanBeStatic")
class ProtoTimestampsTest {

  private static final LocalDateTime LOCAL_DATE_TIME =
      LocalDateTime.parse("2017-07-04T10:15:30.123456789");

  private static final Instant INSTANT = Instant.parse("2017-07-04T10:15:30.123456789Z");

  private static final LocalDate LOCAL_DATE = LocalDate.parse("2017-07-04");

  private static final Timestamp TIMESTAMP =
      Timestamp.newBuilder().setSeconds(1499163330).setNanos(123456789).build();

  private static final Timestamp TIMESTAMP_START_OF_DAY = Timestamps.fromSeconds(1499126400);

  @Nested
  class ForLocalDateTime {

    @Test
    void to() {
      assertThat(ProtoTimestamps.toLocalDateTime(TIMESTAMP, ZoneOffset.UTC))
          .isEqualTo(LOCAL_DATE_TIME);
    }

    @Test
    void from() {
      assertThat(ProtoTimestamps.fromLocalDateTime(LOCAL_DATE_TIME, ZoneOffset.UTC))
          .isEqualTo(TIMESTAMP);
    }
  }

  @Nested
  class ForLocalDate {

    @Test
    void to() {
      assertThat(ProtoTimestamps.toLocalDate(TIMESTAMP, ZoneOffset.UTC)).isEqualTo(LOCAL_DATE);
    }

    @Test
    void from() {
      assertThat(ProtoTimestamps.fromLocalDate(LOCAL_DATE_TIME.toLocalDate(), ZoneOffset.UTC))
          .isEqualTo(TIMESTAMP_START_OF_DAY);
    }
  }

  @Nested
  class ForInstant {

    @Test
    void to() {
      assertThat(ProtoTimestamps.toInstant(TIMESTAMP)).isEqualTo(INSTANT);
    }

    @Test
    void from() {
      assertThat(ProtoTimestamps.fromInstant(INSTANT)).isEqualTo(TIMESTAMP);
    }
  }
}
