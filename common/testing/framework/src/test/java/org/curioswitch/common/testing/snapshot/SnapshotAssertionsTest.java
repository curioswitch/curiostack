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

package org.curioswitch.common.testing.snapshot;

import static org.curioswitch.common.testing.snapshot.SnapshotAssertions.assertSnapshotMatches;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.curioswitch.common.server.framework.immutables.CurioStyle;
import org.curioswitch.testing.TestProto;
import org.immutables.value.Value.Immutable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ClassCanBeStatic")
class SnapshotAssertionsTest {

  @Immutable
  @CurioStyle
  @JsonDeserialize(as = ImmutableTestType.class)
  @JsonSerialize(as = ImmutableTestType.class)
  interface TestType {
    String value1();

    int value2();

    long value3();
  }

  private static final TestType OBJ1 =
      ImmutableTestType.builder().value1("obj1").value2(10).value3(30).build();

  private static final TestType OBJ2 =
      ImmutableTestType.builder().value1("obj2").value2(5).value3(20).build();

  private static final TestType OBJ3 =
      ImmutableTestType.builder().value1("obj3").value2(1000).value3(-275).build();

  private static final TestType OBJ4 =
      ImmutableTestType.builder().value1("obj4").value2(3).value3(1).build();

  private static final TestProto PROTO1 = TestProto.newBuilder().setStringValue("proto1").build();

  @Test
  void topLevel() {
    assertSnapshotMatches(OBJ1);
  }

  @Test
  void proto() {
    assertSnapshotMatches(PROTO1);
  }

  @Nested
  class SnapshotMatches {
    @Test
    void once() {
      assertSnapshotMatches(OBJ2);
    }

    @Test
    void twice() {
      assertSnapshotMatches(OBJ1);
      assertSnapshotMatches(OBJ3);
    }

    @Test
    void thrice() {
      assertSnapshotMatches(OBJ2);
      assertSnapshotMatches(OBJ3);
      assertSnapshotMatches(OBJ4);
    }
  }
}
