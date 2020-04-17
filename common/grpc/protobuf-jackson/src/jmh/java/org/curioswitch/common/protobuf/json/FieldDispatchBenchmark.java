/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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

import com.google.common.collect.ImmutableMap;
import java.util.function.Consumer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Microbenchmark for checking various ways of dispatching methods based on the name of a field.
 * Taking advantage of the fact that Jackson returns interned strings means string identity
 * comparison is the fastest.
 */
@SuppressWarnings({"ReferenceEquality", "OperatorPrecedence", "StringEquality"})
public class FieldDispatchBenchmark {

  private static final ImmutableMap<String, Integer> FIELDS =
      ImmutableMap.<String, Integer>builder()
          .put("optionalInt32", 1)
          .put("optional_int32", 1)
          .put("optionalInt64", 2)
          .put("optional_int64", 2)
          .put("optionalUint32", 3)
          .put("optional_uint32", 3)
          .put("optionalUint64", 4)
          .put("optional_uint64", 4)
          .put("optionalSint32", 5)
          .put("optional_sint32", 5)
          .put("optionalSint64", 6)
          .put("optional_sint64", 6)
          .put("optionalFixed32", 7)
          .put("optional_fixed32", 7)
          .put("optionalFixed64", 8)
          .put("optional_fixed64", 8)
          .put("optionalSfixed32", 9)
          .put("optional_sfixed32", 9)
          .put("optionalSfixed64", 10)
          .put("optional_sfixed64", 10)
          .build();

  private static final ImmutableMap<String, Consumer<Blackhole>> CONSUMERS =
      ImmutableMap.<String, Consumer<Blackhole>>builder()
          .put("optionalInt32", (bh) -> bh.consume(1))
          .put("optional_int32", (bh) -> bh.consume(1))
          .put("optionalInt64", (bh) -> bh.consume(2))
          .put("optional_int64", (bh) -> bh.consume(2))
          .put("optionalUint32", (bh) -> bh.consume(3))
          .put("optional_uint32", (bh) -> bh.consume(3))
          .put("optionalUint64", (bh) -> bh.consume(4))
          .put("optional_uint64", (bh) -> bh.consume(4))
          .put("optionalSint32", (bh) -> bh.consume(5))
          .put("optional_sint32", (bh) -> bh.consume(5))
          .put("optionalSint64", (bh) -> bh.consume(6))
          .put("optional_sint64", (bh) -> bh.consume(6))
          .put("optionalFixed32", (bh) -> bh.consume(7))
          .put("optional_fixed32", (bh) -> bh.consume(7))
          .put("optionalFixed64", (bh) -> bh.consume(8))
          .put("optional_fixed64", (bh) -> bh.consume(8))
          .put("optionalSfixed32", (bh) -> bh.consume(9))
          .put("optional_sfixed32", (bh) -> bh.consume(9))
          .put("optionalSfixed64", (bh) -> bh.consume(10))
          .put("optional_sfixed64", (bh) -> bh.consume(0))
          .build();

  private static final String FIELD_A = "optional_sfixed64";

  @Benchmark
  public void mapDispatch(Blackhole bh) {
    String var4 = FIELD_A;
    Integer fieldNumberBoxed = FIELDS.get(var4);
    if (fieldNumberBoxed == null) {
      bh.consume(-1);
      return;
    }
    int fieldNumber = fieldNumberBoxed;
    if (fieldNumber == 1) {
      bh.consume(1);
    } else if (fieldNumber == 2) {
      bh.consume(2);
    } else if (fieldNumber == 3) {
      bh.consume(3);
    } else if (fieldNumber == 4) {
      bh.consume(4);
    } else if (fieldNumber == 5) {
      bh.consume(5);
    } else if (fieldNumber == 6) {
      bh.consume(6);
    } else if (fieldNumber == 7) {
      bh.consume(7);
    } else if (fieldNumber == 8) {
      bh.consume(8);
    } else if (fieldNumber == 9) {
      bh.consume(9);
    } else {
      bh.consume(10);
    }
  }

  @Benchmark
  public void switchDispatchInterned(Blackhole bh) {
    String var4 = FIELD_A;
    if (var4 == "optionalInt32" || var4 == "optional_int32") {
      bh.consume(1);
    } else if (var4 == "optionalInt64" || var4 == "optional_int64") {
      bh.consume(2);
    } else if (var4 == "optionalUint32" || var4 == "optional_uint32") {
      bh.consume(3);
    } else if (var4 == "optionalUint64" || var4 == "optional_uint64") {
      bh.consume(4);
    } else if (var4 == "optionalSint32" || var4 == "optional_sint32") {
      bh.consume(5);
    } else if (var4 == "optionalSint64" || var4 == "optional_sint64") {
      bh.consume(6);
    } else if (var4 == "optionalFixed32" || var4 == "optional_fixed32") {
      bh.consume(7);
    } else if (var4 == "optionalFixed64" || var4 == "optional_fixed64") {
      bh.consume(8);
    } else if (var4 == "optionalSfixed32" || var4 == "optional_sfixed32") {
      bh.consume(9);
    } else if (var4 == "optionalSfixed64" || var4 == "optional_sfixed64") {
      bh.consume(10);
    } else {
      throw new IllegalStateException("foobar");
    }
  }

  @Benchmark
  public void switchDispatch(Blackhole bh) {
    String var4 = FIELD_A;
    int var13 = var4.hashCode();
    if (var13 == 645283598 && var4.equals("optionalInt32")
        || var13 == -845937425 && var4.equals("optional_int32")) {
      bh.consume(1);
    } else if (var13 == 645283693 && var4.equals("optionalInt64")
        || var13 == -845937330 && var4.equals("optional_int64")) {
      bh.consume(2);
    } else if (var13 == -1132228935 && var4.equals("optionalUint32")
        || var13 == -115440392 && var4.equals("optional_uint32")) {
      bh.consume(3);
    } else if (var13 == -1132228840 && var4.equals("optionalUint64")
        || var13 == -115440297 && var4.equals("optional_uint64")) {
      bh.consume(4);
    } else if (var13 == -1189487237 && var4.equals("optionalSint32")
        || var13 == -172698694 && var4.equals("optional_sint32")) {
      bh.consume(5);
    } else if (var13 == -1189487142 && var4.equals("optionalSint64")
        || var13 == -172698599 && var4.equals("optional_sint64")) {
      bh.consume(6);
    } else if (var13 == -1158176429 && var4.equals("optionalFixed32")
        || var13 == 297497332 && var4.equals("optional_fixed32")) {
      bh.consume(7);
    } else if (var13 == -1158176334 && var4.equals("optionalFixed64")
        || var13 == 297497427 && var4.equals("optional_fixed64")) {
      bh.consume(8);
    } else if (var13 == 858609792 && var4.equals("optionalSfixed32")
        || var13 == -1260143873 && var4.equals("optional_sfixed32")) {
      bh.consume(9);
    } else {
      bh.consume(10);
    }
  }

  @Benchmark
  public void virtualDispatch(Blackhole bh) {
    String var4 = FIELD_A;
    CONSUMERS.get(var4).accept(bh);
  }
}
