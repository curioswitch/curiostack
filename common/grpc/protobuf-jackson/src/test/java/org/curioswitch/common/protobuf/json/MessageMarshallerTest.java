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
package org.curioswitch.common.protobuf.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.NullValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.FieldMaskUtil;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.google.protobuf.util.JsonTestProto.TestAllTypes;
import com.google.protobuf.util.JsonTestProto.TestAny;
import com.google.protobuf.util.JsonTestProto.TestCustomJsonName;
import com.google.protobuf.util.JsonTestProto.TestDuration;
import com.google.protobuf.util.JsonTestProto.TestFieldMask;
import com.google.protobuf.util.JsonTestProto.TestMap;
import com.google.protobuf.util.JsonTestProto.TestOneof;
import com.google.protobuf.util.JsonTestProto.TestRecursive;
import com.google.protobuf.util.JsonTestProto.TestRegression;
import com.google.protobuf.util.JsonTestProto.TestStruct;
import com.google.protobuf.util.JsonTestProto.TestTimestamp;
import com.google.protobuf.util.JsonTestProto.TestWrappers;
import com.google.protobuf.util.Timestamps;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class MessageMarshallerTest {

  @Test
  public void allFields() throws Exception {
    assertMatchesUpstream(JsonTestUtil.testAllTypesAllFields());
  }

  @Test
  public void unknownEnumValue() throws Exception {
    TestAllTypes message =
        TestAllTypes.newBuilder()
            .setOptionalNestedEnumValue(12345)
            .addRepeatedNestedEnumValue(12345)
            .addRepeatedNestedEnumValue(0)
            .build();
    assertMatchesUpstream(message);

    TestMap.Builder mapBuilder = TestMap.newBuilder();
    mapBuilder.putInt32ToEnumMapValue(1, 0);
    Map<Integer, Integer> mapWithInvalidValues = new HashMap<>();
    mapWithInvalidValues.put(2, 12345);
    mapBuilder.putAllInt32ToEnumMapValue(mapWithInvalidValues);
    TestMap mapMessage = mapBuilder.build();
    assertMatchesUpstream(mapMessage);
  }

  @Test
  public void specialFloatValues() throws Exception {
    TestAllTypes message =
        TestAllTypes.newBuilder()
            .addRepeatedFloat(Float.NaN)
            .addRepeatedFloat(Float.POSITIVE_INFINITY)
            .addRepeatedFloat(Float.NEGATIVE_INFINITY)
            .addRepeatedDouble(Double.NaN)
            .addRepeatedDouble(Double.POSITIVE_INFINITY)
            .addRepeatedDouble(Double.NEGATIVE_INFINITY)
            .build();
    assertMatchesUpstream(message);
  }

  @Test
  public void parserAcceptsStringForNumericField() throws Exception {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    mergeFromJson(
        "{\n"
            + "  \"optionalInt32\": \"1234\",\n"
            + "  \"optionalUint32\": \"5678\",\n"
            + "  \"optionalSint32\": \"9012\",\n"
            + "  \"optionalFixed32\": \"3456\",\n"
            + "  \"optionalSfixed32\": \"7890\",\n"
            + "  \"optionalFloat\": \"1.5\",\n"
            + "  \"optionalDouble\": \"1.25\",\n"
            + "  \"optionalBool\": \"true\"\n"
            + "}",
        builder);
    TestAllTypes message = builder.build();
    assertEquals(1234, message.getOptionalInt32());
    assertEquals(5678, message.getOptionalUint32());
    assertEquals(9012, message.getOptionalSint32());
    assertEquals(3456, message.getOptionalFixed32());
    assertEquals(7890, message.getOptionalSfixed32());
    assertEquals(1.5f, message.getOptionalFloat(), 0.000001);
    assertEquals(1.25, message.getOptionalDouble(), 0.000001);
    assertEquals(true, message.getOptionalBool());
  }

  @Test
  public void parserAcceptsFloatingPointValueForIntegerField() throws Exception {
    // Test that numeric values like "1.000", "1e5" will also be accepted.
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    mergeFromJson(
        "{\n"
            + "  \"repeatedInt32\": [1.000, 1e5, \"1.000\", \"1e5\"],\n"
            + "  \"repeatedUint32\": [1.000, 1e5, \"1.000\", \"1e5\"],\n"
            + "  \"repeatedInt64\": [1.000, 1e5, \"1.000\", \"1e5\"],\n"
            + "  \"repeatedUint64\": [1.000, 1e5, \"1.000\", \"1e5\"]\n"
            + "}",
        builder);
    int[] expectedValues = new int[] {1, 100000, 1, 100000};
    assertEquals(4, builder.getRepeatedInt32Count());
    assertEquals(4, builder.getRepeatedUint32Count());
    assertEquals(4, builder.getRepeatedInt64Count());
    assertEquals(4, builder.getRepeatedUint64Count());
    for (int i = 0; i < 4; ++i) {
      assertEquals(expectedValues[i], builder.getRepeatedInt32(i));
      assertEquals(expectedValues[i], builder.getRepeatedUint32(i));
      assertEquals(expectedValues[i], builder.getRepeatedInt64(i));
      assertEquals(expectedValues[i], builder.getRepeatedUint64(i));
    }

    // Non-integers will still be rejected.
    assertRejects("optionalInt32", "1.5");
    assertRejects("optionalUint32", "1.5");
    assertRejects("optionalInt64", "1.5");
    assertRejects("optionalUint64", "1.5");
  }

  private static void assertRejects(String name, String value) {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    // Numeric form is rejected.
    assertThatThrownBy(() -> mergeFromJson("{\"" + name + "\":" + value + "}", builder))
        .isInstanceOf(IOException.class);
    // String form is also rejected.
    assertThatThrownBy(() -> mergeFromJson("{\"" + name + "\":\"" + value + "\"}", builder))
        .isInstanceOf(IOException.class);
  }

  private static void assertAccepts(String name, String value) throws IOException {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    // Both numeric form and string form are accepted.
    mergeFromJson("{\"" + name + "\":" + value + "}", builder);
    builder.clear();
    mergeFromJson("{\"" + name + "\":\"" + value + "\"}", builder);
  }

  @Test
  public void parserRejectOutOfRangeNumericValues() throws Exception {
    assertAccepts("optionalInt32", String.valueOf(Integer.MAX_VALUE));
    assertAccepts("optionalInt32", String.valueOf(Integer.MIN_VALUE));
    assertRejects("optionalInt32", String.valueOf(Integer.MAX_VALUE + 1L));
    assertRejects("optionalInt32", String.valueOf(Integer.MIN_VALUE - 1L));

    assertAccepts("optionalUint32", String.valueOf(Integer.MAX_VALUE + 1L));
    assertRejects("optionalUint32", "123456789012345");
    assertRejects("optionalUint32", "-1");

    BigInteger one = new BigInteger("1");
    BigInteger maxLong = new BigInteger(String.valueOf(Long.MAX_VALUE));
    BigInteger minLong = new BigInteger(String.valueOf(Long.MIN_VALUE));
    assertAccepts("optionalInt64", maxLong.toString());
    assertAccepts("optionalInt64", minLong.toString());
    assertRejects("optionalInt64", maxLong.add(one).toString());
    assertRejects("optionalInt64", minLong.subtract(one).toString());

    assertAccepts("optionalUint64", maxLong.add(one).toString());
    assertRejects("optionalUint64", "1234567890123456789012345");
    assertRejects("optionalUint64", "-1");

    assertAccepts("optionalBool", "true");
    assertRejects("optionalBool", "1");
    assertRejects("optionalBool", "0");

    assertAccepts("optionalFloat", String.valueOf(Float.MAX_VALUE));
    assertAccepts("optionalFloat", String.valueOf(-Float.MAX_VALUE));
    assertRejects("optionalFloat", String.valueOf(Double.MAX_VALUE));
    assertRejects("optionalFloat", String.valueOf(-Double.MAX_VALUE));

    BigDecimal moreThanOne = new BigDecimal("1.000001");
    BigDecimal maxDouble = new BigDecimal(Double.MAX_VALUE);
    BigDecimal minDouble = new BigDecimal(-Double.MAX_VALUE);
    assertAccepts("optionalDouble", maxDouble.toString());
    assertAccepts("optionalDouble", minDouble.toString());
    assertRejects("optionalDouble", maxDouble.multiply(moreThanOne).toString());
    assertRejects("optionalDouble", minDouble.multiply(moreThanOne).toString());
  }

  @Test
  public void parserAcceptsNull() throws Exception {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    mergeFromJson(
        "{\n"
            + "  \"optionalInt32\": null,\n"
            + "  \"optionalInt64\": null,\n"
            + "  \"optionalUint32\": null,\n"
            + "  \"optionalUint64\": null,\n"
            + "  \"optionalSint32\": null,\n"
            + "  \"optionalSint64\": null,\n"
            + "  \"optionalFixed32\": null,\n"
            + "  \"optionalFixed64\": null,\n"
            + "  \"optionalSfixed32\": null,\n"
            + "  \"optionalSfixed64\": null,\n"
            + "  \"optionalFloat\": null,\n"
            + "  \"optionalDouble\": null,\n"
            + "  \"optionalBool\": null,\n"
            + "  \"optionalString\": null,\n"
            + "  \"optionalBytes\": null,\n"
            + "  \"optionalNestedMessage\": null,\n"
            + "  \"optionalNestedEnum\": null,\n"
            + "  \"repeatedInt32\": null,\n"
            + "  \"repeatedInt64\": null,\n"
            + "  \"repeatedUint32\": null,\n"
            + "  \"repeatedUint64\": null,\n"
            + "  \"repeatedSint32\": null,\n"
            + "  \"repeatedSint64\": null,\n"
            + "  \"repeatedFixed32\": null,\n"
            + "  \"repeatedFixed64\": null,\n"
            + "  \"repeatedSfixed32\": null,\n"
            + "  \"repeatedSfixed64\": null,\n"
            + "  \"repeatedFloat\": null,\n"
            + "  \"repeatedDouble\": null,\n"
            + "  \"repeatedBool\": null,\n"
            + "  \"repeatedString\": null,\n"
            + "  \"repeatedBytes\": null,\n"
            + "  \"repeatedNestedMessage\": null,\n"
            + "  \"repeatedNestedEnum\": null\n"
            + "}",
        builder);
    TestAllTypes message = builder.build();
    assertEquals(TestAllTypes.getDefaultInstance(), message);

    // Repeated field elements cannot be null.
    TestAllTypes.Builder builder2 = TestAllTypes.newBuilder();
    assertThatThrownBy(
            () -> mergeFromJson("{\n" + "  \"repeatedInt32\": [null, null],\n" + "}", builder2))
        .isInstanceOf(IOException.class);

    TestAllTypes.Builder builder3 = TestAllTypes.newBuilder();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n" + "  \"repeatedNestedMessage\": [null, null],\n" + "}", builder3))
        .isInstanceOf(IOException.class);
  }

  @Test
  public void nullInOneOf() throws Exception {
    TestOneof.Builder builder = TestOneof.newBuilder();
    mergeFromJson("{\n" + "  \"oneofNullValue\": null \n" + "}", builder);
    TestOneof message = builder.build();
    assertEquals(TestOneof.OneofFieldCase.ONEOF_NULL_VALUE, message.getOneofFieldCase());
    assertEquals(NullValue.NULL_VALUE, message.getOneofNullValue());
  }

  @Test
  public void parserRejectDuplicatedFields() throws Exception {
    // NOTE: Upstream parser does not correctly reject duplicates with the same field variableName,
    // only when json variableName and proto variableName are both specified. We handle both cases.
    // Also, since
    // we keep track based on field number, the logic is much simpler so most of these tests
    // are redundant and just preserved to maintain parity with upstream.

    // We also actually do a mergeValue as the method indicates. If the field is already set in the
    // input, it will be overwritten. We only want to ensure a valid JSON input.

    // Duplicated optional fields.
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n"
                        + "  \"optionalNestedMessage\": {},\n"
                        + "  \"optional_nested_message\": {}\n"
                        + "}",
                    builder))
        .isInstanceOf(InvalidProtocolBufferException.class);
    builder.clear();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n"
                        + "  \"optionalNestedMessage\": {},\n"
                        + "  \"optionalNestedMessage\": {}\n"
                        + "}",
                    builder))
        .isInstanceOf(InvalidProtocolBufferException.class);

    // Duplicated repeated fields.
    builder.clear();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n"
                        + "  \"repeatedInt32\": [1, 2],\n"
                        + "  \"repeated_int32\": [5, 6]\n"
                        + "}",
                    builder))
        .isInstanceOf(InvalidProtocolBufferException.class);
    builder.clear();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n"
                        + "  \"repeatedInt32\": [1, 2],\n"
                        + "  \"repeatedInt32\": [5, 6]\n"
                        + "}",
                    builder))
        .isInstanceOf(InvalidProtocolBufferException.class);

    // Duplicated oneof fields, same variableName.
    TestOneof.Builder builder2 = TestOneof.newBuilder();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n" + "  \"oneofInt32\": 1,\n" + "  \"oneof_int32\": 2\n" + "}", builder2))
        .isInstanceOf(InvalidProtocolBufferException.class);
    builder2.clear();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n" + "  \"oneofInt32\": 1,\n" + "  \"oneofInt32\": 2\n" + "}", builder2))
        .isInstanceOf(InvalidProtocolBufferException.class);

    // Duplicated oneof fields, different variableName.
    builder2.clear();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n" + "  \"oneofInt32\": 1,\n" + "  \"oneofNullValue\": null\n" + "}",
                    builder2))
        .isInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  public void mapFields() throws Exception {
    assertMatchesUpstream(JsonTestUtil.testMapAllTypes());

    TestMap message =
        TestMap.newBuilder().putInt32ToInt32Map(1, 2).putInt32ToInt32Map(3, 4).build();
    assertMatchesUpstream(message);
  }

  @Test
  public void mapNullValueIsRejected() throws Exception {
    TestMap.Builder builder = TestMap.newBuilder();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n"
                        + "  \"int32ToInt32Map\": {null: 1},\n"
                        + "  \"int32ToMessageMap\": {null: 2}\n"
                        + "}",
                    builder))
        .isInstanceOf(InvalidProtocolBufferException.class);

    TestMap.Builder builder2 = TestMap.newBuilder();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n"
                        + "  \"int32ToInt32Map\": {\"1\": null},\n"
                        + "  \"int32ToMessageMap\": {\"2\": null}\n"
                        + "}",
                    builder2))
        .isInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  public void parserAcceptsNonQuotedObjectKey() throws Exception {
    TestMap.Builder builder = TestMap.newBuilder();
    mergeFromJson(
        "{\n" + "  int32ToInt32Map: {1: 2},\n" + "  stringToInt32Map: {hello: 3}\n" + "}", builder);
    TestMap message = builder.build();
    assertEquals(2, message.getInt32ToInt32Map().get(1).intValue());
    assertEquals(3, message.getStringToInt32Map().get("hello").intValue());
  }

  @Test
  public void wrappers() throws Exception {
    TestWrappers.Builder builder = TestWrappers.newBuilder();
    builder.getBoolValueBuilder().setValue(false);
    builder.getInt32ValueBuilder().setValue(0);
    builder.getInt64ValueBuilder().setValue(0);
    builder.getUint32ValueBuilder().setValue(0);
    builder.getUint64ValueBuilder().setValue(0);
    builder.getFloatValueBuilder().setValue(0.0f);
    builder.getDoubleValueBuilder().setValue(0.0);
    builder.getStringValueBuilder().setValue("");
    builder.getBytesValueBuilder().setValue(ByteString.EMPTY);
    TestWrappers message = builder.build();
    assertMatchesUpstream(message);

    builder = TestWrappers.newBuilder();
    builder.getBoolValueBuilder().setValue(true);
    builder.getInt32ValueBuilder().setValue(1);
    builder.getInt64ValueBuilder().setValue(2);
    builder.getUint32ValueBuilder().setValue(3);
    builder.getUint64ValueBuilder().setValue(4);
    builder.getFloatValueBuilder().setValue(5.0f);
    builder.getDoubleValueBuilder().setValue(6.0);
    builder.getStringValueBuilder().setValue("7");
    builder.getBytesValueBuilder().setValue(ByteString.copyFrom(new byte[] {8}));
    message = builder.build();
    assertMatchesUpstream(message);
  }

  @Test
  public void timestamp() throws Exception {
    TestTimestamp message =
        TestTimestamp.newBuilder()
            .setTimestampValue(Timestamps.parse("1970-01-01T00:00:00Z"))
            .build();
    assertMatchesUpstream(message);
  }

  @Test
  public void duration() throws Exception {
    TestDuration message =
        TestDuration.newBuilder().setDurationValue(Durations.parse("12345s")).build();
    assertMatchesUpstream(message);
  }

  @Test
  public void fieldMask() throws Exception {
    TestFieldMask message =
        TestFieldMask.newBuilder()
            .setFieldMaskValue(FieldMaskUtil.fromString("foo.bar,baz,foo_bar.baz"))
            .build();
    assertMatchesUpstream(message);
  }

  @Test
  public void struct() throws Exception {
    // Build a struct with all possible values.
    TestStruct.Builder builder = TestStruct.newBuilder();
    Struct.Builder structBuilder = builder.getStructValueBuilder();
    structBuilder.putFields("null_value", Value.newBuilder().setNullValueValue(0).build());
    structBuilder.putFields("number_value", Value.newBuilder().setNumberValue(1.25).build());
    structBuilder.putFields("string_value", Value.newBuilder().setStringValue("hello").build());
    Struct.Builder subStructBuilder = Struct.newBuilder();
    subStructBuilder.putFields("number_value", Value.newBuilder().setNumberValue(1234).build());
    structBuilder.putFields(
        "struct_value", Value.newBuilder().setStructValue(subStructBuilder.build()).build());
    ListValue.Builder listBuilder = ListValue.newBuilder();
    listBuilder.addValues(Value.newBuilder().setNumberValue(1.125).build());
    listBuilder.addValues(Value.newBuilder().setNullValueValue(0).build());
    structBuilder.putFields(
        "list_value", Value.newBuilder().setListValue(listBuilder.build()).build());
    TestStruct message = builder.build();
    assertMatchesUpstream(message);

    builder = TestStruct.newBuilder();
    builder.setValue(Value.newBuilder().setNullValueValue(0).build());
    message = builder.build();
    assertMatchesUpstream(message);

    builder = TestStruct.newBuilder();
    listBuilder = builder.getListValueBuilder();
    listBuilder.addValues(Value.newBuilder().setNumberValue(31831.125).build());
    listBuilder.addValues(Value.newBuilder().setNullValueValue(0).build());
    message = builder.build();
    assertMatchesUpstream(message);
  }

  @Test
  public void anyFields() throws Exception {
    TestAllTypes content = TestAllTypes.newBuilder().setOptionalInt32(1234).build();
    TestAny message = TestAny.newBuilder().setAnyValue(Any.pack(content)).build();
    assertMatchesUpstream(message, TestAllTypes.getDefaultInstance());

    TestAny messageWithDefaultAnyValue =
        TestAny.newBuilder().setAnyValue(Any.getDefaultInstance()).build();
    assertMatchesUpstream(messageWithDefaultAnyValue);

    // Well-known types have a special formatting when embedded in Any.
    //
    // 1. Any in Any.
    Any anyMessage = Any.pack(Any.pack(content));
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());

    // 2. Wrappers in Any.
    anyMessage = Any.pack(Int32Value.newBuilder().setValue(12345).build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());
    anyMessage = Any.pack(UInt32Value.newBuilder().setValue(12345).build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());
    anyMessage = Any.pack(Int64Value.newBuilder().setValue(12345).build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());
    anyMessage = Any.pack(UInt64Value.newBuilder().setValue(12345).build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());
    anyMessage = Any.pack(FloatValue.newBuilder().setValue(12345).build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());
    anyMessage = Any.pack(DoubleValue.newBuilder().setValue(12345).build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());
    anyMessage = Any.pack(BoolValue.newBuilder().setValue(true).build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());
    anyMessage = Any.pack(StringValue.newBuilder().setValue("Hello").build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());
    anyMessage =
        Any.pack(BytesValue.newBuilder().setValue(ByteString.copyFrom(new byte[] {1, 2})).build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());

    // 3. Timestamp in Any.
    anyMessage = Any.pack(Timestamps.parse("1969-12-31T23:59:59Z"));
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());

    // 4. Duration in Any
    anyMessage = Any.pack(Durations.parse("12345.10s"));
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());

    // 5. FieldMask in Any
    anyMessage = Any.pack(FieldMaskUtil.fromString("foo.bar,baz"));
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());

    // 6. Struct in Any
    Struct.Builder structBuilder = Struct.newBuilder();
    structBuilder.putFields("number", Value.newBuilder().setNumberValue(1.125).build());
    anyMessage = Any.pack(structBuilder.build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());

    // 7. Value (number type) in Any
    Value.Builder valueBuilder = Value.newBuilder();
    valueBuilder.setNumberValue(1);
    anyMessage = Any.pack(valueBuilder.build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());

    // 8. Value (null type) in Any
    anyMessage = Any.pack(Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build());
    assertMatchesUpstream(anyMessage, TestAllTypes.getDefaultInstance());
  }

  @Test
  public void anyInMaps() throws Exception {
    TestAny.Builder testAny = TestAny.newBuilder();
    testAny.putAnyMap("int32_wrapper", Any.pack(Int32Value.newBuilder().setValue(123).build()));
    testAny.putAnyMap("int64_wrapper", Any.pack(Int64Value.newBuilder().setValue(456).build()));
    testAny.putAnyMap("timestamp", Any.pack(Timestamps.parse("1969-12-31T23:59:59Z")));
    testAny.putAnyMap("duration", Any.pack(Durations.parse("12345.1s")));
    testAny.putAnyMap("field_mask", Any.pack(FieldMaskUtil.fromString("foo.bar,baz")));
    Value numberValue = Value.newBuilder().setNumberValue(1.125).build();
    Struct.Builder struct = Struct.newBuilder();
    struct.putFields("number", numberValue);
    testAny.putAnyMap("struct", Any.pack(struct.build()));
    Value nullValue = Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
    testAny.putAnyMap(
        "list_value",
        Any.pack(ListValue.newBuilder().addValues(numberValue).addValues(nullValue).build()));
    testAny.putAnyMap("number_value", Any.pack(numberValue));
    testAny.putAnyMap("any_value_number", Any.pack(Any.pack(numberValue)));
    testAny.putAnyMap("any_value_default", Any.pack(Any.getDefaultInstance()));
    testAny.putAnyMap("default", Any.getDefaultInstance());

    assertMatchesUpstream(testAny.build(), TestAllTypes.getDefaultInstance());
  }

  @Test
  public void parserMissingTypeUrl() throws Exception {
    Any.Builder builder = Any.newBuilder();
    assertThatThrownBy(() -> mergeFromJson("{\n" + "  \"optionalInt32\": 1234\n" + "}", builder))
        .isInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  public void parserUnexpectedTypeUrl() throws Exception {
    Any.Builder builder = Any.newBuilder();
    assertThatThrownBy(
            () ->
                mergeFromJson(
                    "{\n"
                        + "  \"@type\": \"type.googleapis.com/json_test.TestAllTypes\",\n"
                        + "  \"optionalInt32\": 12345\n"
                        + "}",
                    builder))
        .isInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  public void parserRejectTrailingComma() throws Exception {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    assertThatThrownBy(() -> mergeFromJson("{\n" + "  \"optionalInt32\": 12345,\n" + "}", builder))
        .isInstanceOf(InvalidProtocolBufferException.class);

    // Note: Upstream parser does not handle this case, but we do thanks to Jackson's validity
    // checks.
    builder.clear();
    assertThatThrownBy(
            () -> mergeFromJson("{\n" + "  \"repeatedInt32\": [12345,]\n" + "}", builder))
        .isInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  public void parserRejectInvalidBase64() throws Exception {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    assertThatThrownBy(() -> mergeFromJson("{\"optionalBytes\": \"!@#$\"", builder))
        .isInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  public void parserAcceptBase64Variants() throws Exception {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    mergeFromJson("{\"optionalBytes\": \"AQI\"}", builder);
  }

  @Test
  public void parserRejectInvalidEnumValue() throws Exception {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    assertThatThrownBy(
            () -> mergeFromJson("{\n" + "  \"optionalNestedEnum\": \"XXX\"\n" + "}", builder))
        .isInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  public void parserUnknownFields() throws Exception {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    String json = "{\n" + "  \"unknownField\": \"XXX\"\n" + "}";
    assertThatThrownBy(() -> mergeFromJson(json, builder))
        .isInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  public void parserIgnoringUnknownFields() throws Exception {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    String json = "{\n" + " \"optionalInt32\": 10, \n" + "  \"unknownField\": \"XXX\"\n" + "}";
    mergeFromJson(true, json, builder);
    assertThat(builder.getOptionalInt32()).isEqualTo(10);
  }

  @Test
  public void customJsonName() throws Exception {
    TestCustomJsonName message = TestCustomJsonName.newBuilder().setValue(12345).build();
    assertMatchesUpstream(message);
  }

  @Test
  public void defaultDoesNotHtmlExcape() throws Exception {
    TestAllTypes message = TestAllTypes.newBuilder().setOptionalString("=").build();
    assertMatchesUpstream(message);
  }

  @Test
  public void includingDefaultValueFields() throws Exception {
    TestAllTypes message = TestAllTypes.getDefaultInstance();
    assertMatchesUpstream(message);
    assertMatchesUpstream(message, true, false, false);

    TestMap mapMessage = TestMap.getDefaultInstance();
    assertMatchesUpstream(mapMessage);
    assertMatchesUpstream(mapMessage, true, false, false);

    TestOneof oneofMessage = TestOneof.getDefaultInstance();
    assertMatchesUpstream(oneofMessage);
    assertMatchesUpstream(oneofMessage, true, false, false);

    oneofMessage = TestOneof.newBuilder().setOneofInt32(42).build();
    assertMatchesUpstream(oneofMessage);
    assertMatchesUpstream(oneofMessage, true, false, false);

    oneofMessage = TestOneof.newBuilder().setOneofNullValue(NullValue.NULL_VALUE).build();
    assertMatchesUpstream(oneofMessage);
    assertMatchesUpstream(oneofMessage, true, false, false);
  }

  @Test
  public void preservingProtoFieldNames() throws Exception {
    TestAllTypes message = TestAllTypes.newBuilder().setOptionalInt32(12345).build();
    assertMatchesUpstream(message);
    assertMatchesUpstream(message, false, true, false);

    // The json_name field option is ignored when configured to use original proto field names.
    TestCustomJsonName messageWithCustomJsonName =
        TestCustomJsonName.newBuilder().setValue(12345).build();
    assertMatchesUpstream(message, false, true, false);

    // Parsers accept both original proto field names and lowerCamelCase names.
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    mergeFromJson("{\"optionalInt32\": 12345}", builder);
    assertEquals(12345, builder.getOptionalInt32());
    builder.clear();
    mergeFromJson("{\"optional_int32\": 54321}", builder);
    assertEquals(54321, builder.getOptionalInt32());
  }

  @Test
  public void omittingInsignificantWhitespace() throws Exception {
    TestAllTypes message = TestAllTypes.newBuilder().setOptionalInt32(12345).build();
    assertMatchesUpstream(message, false, false, true);
    TestAllTypes message1 = TestAllTypes.getDefaultInstance();
    assertMatchesUpstream(message1, false, false, true);
    TestAllTypes message2 = JsonTestUtil.testAllTypesAllFields();
    assertMatchesUpstream(message2, false, false, true);
  }

  // Regression test for b/29892357
  @Test
  public void emptyWrapperTypesInAny() throws Exception {
    Any.Builder builder = Any.newBuilder();
    mergeFromJson(
        "{\n"
            + "  \"@type\": \"type.googleapis.com/google.protobuf.BoolValue\",\n"
            + "  \"value\": false\n"
            + "}\n",
        builder,
        TestAllTypes.getDefaultInstance());
    Any any = builder.build();
    assertEquals(0, any.getValue().size());
  }

  @Test
  public void recursionLimit() throws Exception {
    TestRecursive.Builder builder = TestRecursive.newBuilder();
    mergeFromJson(recursiveJson(ParseSupport.RECURSION_LIMIT - 1), builder);
    TestRecursive message = builder.build();
    for (int i = 0; i < ParseSupport.RECURSION_LIMIT - 1; i++) {
      message = message.getNested();
    }
    assertEquals(1234, message.getValue());

    builder.clear();
    assertThatThrownBy(() -> mergeFromJson(recursiveJson(ParseSupport.RECURSION_LIMIT), builder))
        .isInstanceOf(InvalidProtocolBufferException.class);
  }

  // https://github.com/curioswitch/curiostack/issues/7
  @Test
  public void protoFieldAlreadyCamelCase() throws Exception {
    assertMatchesUpstream(TestRegression.newBuilder().addFeedIds(1).build());
  }

  private static String recursiveJson(int numRecursions) {
    StringBuilder input = new StringBuilder("{\n");
    for (int i = 0; i < numRecursions; i++) {
      input.append(Strings.repeat(" ", (i + 1) * 2));
      input.append("\"nested\": {\n");
    }
    input.append(Strings.repeat(" ", (numRecursions + 1) * 2));
    input.append("\"value\": 1234\n");
    for (int i = numRecursions - 1; i >= 0; i--) {
      input.append(Strings.repeat(" ", (i + 1) * 2));
      input.append("}\n");
    }
    input.append("}");
    return input.toString();
  }

  private static void assertMatchesUpstream(Message message, Message... additionalTypes)
      throws IOException {
    assertMatchesUpstream(message, false, false, false, additionalTypes);
  }

  private static void assertMatchesUpstream(
      Message message,
      boolean includingDefaultValueFields,
      boolean preservingProtoFieldNames,
      boolean omittingInsignificantWhitespace,
      Message... additionalTypes)
      throws IOException {
    MessageMarshaller.Builder marshallerBuilder =
        MessageMarshaller.builder()
            .register(message.getClass())
            .includingDefaultValueFields(includingDefaultValueFields)
            .preservingProtoFieldNames(preservingProtoFieldNames)
            .omittingInsignificantWhitespace(omittingInsignificantWhitespace);
    for (Message m : additionalTypes) {
      marshallerBuilder.register(m.getDefaultInstanceForType());
    }
    MessageMarshaller marshaller = marshallerBuilder.build();
    TypeRegistry.Builder typeRegistry = TypeRegistry.newBuilder();
    for (Message m : additionalTypes) {
      typeRegistry.add(m.getDescriptorForType());
    }
    Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry.build());
    if (includingDefaultValueFields) {
      printer = printer.includingDefaultValueFields();
    }
    if (preservingProtoFieldNames) {
      printer = printer.preservingProtoFieldNames();
    }
    if (omittingInsignificantWhitespace) {
      printer = printer.omittingInsignificantWhitespace();
    }

    String json = marshaller.writeValueAsString(message);
    String upstreamJson = printer.print(message);
    assertThat(json).isEqualTo(upstreamJson);

    String jsonFromBytes =
        new String(marshaller.writeValueAsBytes(message), StandardCharsets.UTF_8);
    assertThat(jsonFromBytes).isEqualTo(upstreamJson);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    marshaller.writeValue(message, bos);
    assertThat(bos.toString(StandardCharsets.UTF_8.toString())).isEqualTo(upstreamJson);

    Message.Builder builder = message.newBuilderForType();
    mergeFromJson(json, builder, additionalTypes);
    assertThat(builder.build()).isEqualTo(message);
  }

  private static void mergeFromJson(
      String json, Message.Builder builder, Message... additionalTypes) throws IOException {
    mergeFromJson(false, json, builder, additionalTypes);
  }

  @SuppressWarnings("InconsistentOverloads")
  private static void mergeFromJson(
      boolean ignoringUnknownFields, String json, Builder builder, Message... additionalTypes)
      throws IOException {
    MessageMarshaller.Builder marshallerBuilder =
        MessageMarshaller.builder()
            .register(builder.getDefaultInstanceForType())
            .ignoringUnknownFields(ignoringUnknownFields);
    for (Message prototype : additionalTypes) {
      marshallerBuilder.register(prototype);
    }
    MessageMarshaller marshaller = marshallerBuilder.build();
    marshaller.mergeValue(json, builder);

    Message.Builder builder2 = builder.build().newBuilderForType();
    marshaller.mergeValue(json.getBytes(StandardCharsets.UTF_8), builder2);
    assertThat(builder2.build()).isEqualTo(builder.build());

    Message.Builder builder3 = builder.build().newBuilderForType();
    try (ByteArrayInputStream bis =
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
      marshaller.mergeValue(bis, builder3);
    }
    assertThat(builder3.build()).isEqualTo(builder.build());
  }
}
