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

package org.curioswitch.common.testing.assertj.proto;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.curioswitch.common.testing.assertj.proto.ProtoAssert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Unit tests for {@link ProtoAssert}. */
@RunWith(Parameterized.class)
public class ProtoAssertTest extends ProtoAssertTestBase {

  @Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    return ProtoAssertTestBase.parameters();
  }

  public ProtoAssertTest(TestType testType) {
    super(testType);
  }

  @Test
  public void testDifferentClasses() throws InvalidProtocolBufferException {
    Message message = parse("o_int: 3");
    DynamicMessage dynamicMessage =
        DynamicMessage.parseFrom(message.getDescriptorForType(), message.toByteString());

    assertThat(message).isEqualTo(dynamicMessage);
    assertThat(dynamicMessage).isEqualTo(message);
  }

  @Test
  public void testFullDiffOnlyWhenRelevant() {
    // There are no matches, so 'Full diff' should not be printed.
    assertThatThrownBy(() -> assertThat(parse("o_int: 3")).isEqualTo(parse("o_int: 4")))
        .isInstanceOf(AssertionError.class)
        .satisfies(e -> assertThat(e.getMessage()).doesNotContain("Full diff"));

    // r_string is matched, so the 'Full diff' contains extra information.
    assertThatThrownBy(
            () ->
                assertThat(parse("o_int: 3 r_string: 'abc'"))
                    .isEqualTo(parse("o_int: 4 r_string: 'abc'")))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Full diff");
  }

  @Test
  public void testIgnoringFieldAbsence() {
    Message message = parse("o_int: 3");
    Message diffMessage = parse("o_int: 3 o_enum: DEFAULT");

    // Make sure the implementation is reflexive.
    if (isProto3()) {
      assertThat(diffMessage).isEqualTo(message);
      assertThat(message).isEqualTo(diffMessage);
    } else {
      assertThat(diffMessage).isNotEqualTo(message);
      assertThat(message).isNotEqualTo(diffMessage);
    }
    assertThat(diffMessage).ignoringFieldAbsence().isEqualTo(message);
    assertThat(message).ignoringFieldAbsence().isEqualTo(diffMessage);

    if (!isProto3()) {
      Message customDefaultMessage = parse("o_int: 3");
      Message diffCustomDefaultMessage = parse("o_int: 3 o_long_defaults_to_42: 42");

      assertThat(diffCustomDefaultMessage).isNotEqualTo(customDefaultMessage);
      assertThat(diffCustomDefaultMessage).ignoringFieldAbsence().isEqualTo(customDefaultMessage);
      assertThat(customDefaultMessage).isNotEqualTo(diffCustomDefaultMessage);
      assertThat(customDefaultMessage).ignoringFieldAbsence().isEqualTo(diffCustomDefaultMessage);
    }

    if (!isProto3()) {
      assertThatThrownBy(() -> assertThat(diffMessage).isEqualTo(message))
          .isInstanceOf(AssertionError.class)
          .satisfies(isEqualToFailureMessage())
          .hasMessageContaining("added: o_enum: DEFAULT");
    }

    assertThatThrownBy(() -> assertThat(diffMessage).ignoringFieldAbsence().isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("matched: o_int: 3")
        .satisfies(
            e -> {
              if (!isProto3()) {
                // Proto 3 doesn't cover the field at all when it's not set.
                assertThat(e.getMessage()).contains("matched: o_enum: DEFAULT");
              }
            });
  }

  @Test
  public void testIgnoringFieldAbsence_scoped() {
    Message message = parse("o_sub_test_message: { o_test_message: {} }");
    Message emptyMessage = parse("");
    Message partialMessage = parse("o_sub_test_message: {}");

    // All three are equal if we ignore field absence entirely.
    assertThat(emptyMessage).ignoringFieldAbsence().isEqualTo(message);
    assertThat(partialMessage).ignoringFieldAbsence().isEqualTo(message);

    // If we ignore only o_sub_test_message.o_test_message, only the partial message is equal.
    FieldDescriptor subTestMessageField = getFieldDescriptor("o_sub_test_message");
    FieldDescriptor subTestMessageTestMessageField =
        checkNotNull(subTestMessageField.getMessageType().findFieldByName("o_test_message"));
    assertThat(partialMessage)
        .ignoringFieldAbsenceOfFieldDescriptors(subTestMessageTestMessageField)
        .isEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(emptyMessage)
                    .ignoringFieldAbsenceOfFieldDescriptors(subTestMessageTestMessageField)
                    .isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("deleted: o_sub_test_message");

    // But, we can ignore both.
    assertThat(partialMessage)
        .ignoringFieldAbsenceOfFieldDescriptors(subTestMessageField)
        .ignoringFieldAbsenceOfFieldDescriptors(subTestMessageTestMessageField)
        .isEqualTo(message);
    assertThat(partialMessage)
        .ignoringFieldAbsenceOfFieldDescriptors(subTestMessageField, subTestMessageTestMessageField)
        .isEqualTo(message);
    assertThat(emptyMessage)
        .ignoringFieldAbsenceOfFieldDescriptors(subTestMessageField)
        .ignoringFieldAbsenceOfFieldDescriptors(subTestMessageTestMessageField)
        .isEqualTo(message);
    assertThat(emptyMessage)
        .ignoringFieldAbsenceOfFieldDescriptors(subTestMessageField, subTestMessageTestMessageField)
        .isEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(message)
                    .ignoringFieldAbsenceOfFieldDescriptors(getFieldDescriptor("r_string"))
                    .isEqualTo(message))
        .hasMessageContaining("r_string")
        .hasMessageContaining("repeated fields cannot be absent");

    if (isProto3()) {
      assertThatThrownBy(
              () ->
                  assertThat(message)
                      .ignoringFieldAbsenceOfFieldDescriptors(getFieldDescriptor("o_double"))
                      .isEqualTo(message))
          .hasMessageContaining("o_double")
          .hasMessageContaining("is a primitive field in a Proto 3 message");
    } else {
      assertThat(message)
          .ignoringFieldAbsenceOfFieldDescriptors(getFieldDescriptor("o_double"))
          .isEqualTo(message);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUnknownFields() throws InvalidProtocolBufferException {
    Message message =
        fromUnknownFields(
            UnknownFieldSet.newBuilder()
                .addField(99, UnknownFieldSet.Field.newBuilder().addVarint(42).build())
                .build());
    Message diffMessage =
        fromUnknownFields(
            UnknownFieldSet.newBuilder()
                .addField(93, UnknownFieldSet.Field.newBuilder().addVarint(42).build())
                .build());

    assertThat(diffMessage).isNotEqualTo(message);
    assertThat(diffMessage).ignoringFieldAbsence().isEqualTo(message);

    assertThatThrownBy(() -> assertThat(diffMessage).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("added: 93[0]: 42")
        .hasMessageContaining("deleted: 99[0]: 42");

    assertThatThrownBy(() -> assertThat(diffMessage).ignoringFieldAbsence().isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage());
  }

  @Test
  public void testRepeatedFieldOrder() {
    Message message = parse("r_string: \"foo\" r_string: \"bar\"");
    Message eqMessage = parse("r_string: \"bar\" r_string: \"foo\"");
    Message diffMessage = parse("r_string: \"foo\" r_string: \"foo\" r_string: \"bar\"");

    assertThat(message).isEqualTo(message.toBuilder().build());
    assertThat(message).ignoringRepeatedFieldOrder().isEqualTo(message.toBuilder().build());
    assertThat(diffMessage).isNotEqualTo(message);
    assertThat(diffMessage).ignoringRepeatedFieldOrder().isNotEqualTo(message);
    assertThat(eqMessage).isNotEqualTo(message);
    assertThat(eqMessage).ignoringRepeatedFieldOrder().isEqualTo(message);

    Message nestedMessage =
        parse(
            "r_test_message: { o_int: 33 r_string: \"foo\" r_string: \"bar\" } "
                + "r_test_message: { o_int: 44 r_string: \"baz\" r_string: \"qux\" } ");
    Message diffNestedMessage =
        parse(
            "r_test_message: { o_int: 33 r_string: \"qux\" r_string: \"baz\" } "
                + "r_test_message: { o_int: 44 r_string: \"bar\" r_string: \"foo\" } ");
    Message eqNestedMessage =
        parse(
            "r_test_message: { o_int: 44 r_string: \"qux\" r_string: \"baz\" } "
                + "r_test_message: { o_int: 33 r_string: \"bar\" r_string: \"foo\" } ");

    assertThat(nestedMessage).isEqualTo(nestedMessage.toBuilder().build());
    assertThat(nestedMessage)
        .ignoringRepeatedFieldOrder()
        .isEqualTo(nestedMessage.toBuilder().build());
    assertThat(diffNestedMessage).isNotEqualTo(nestedMessage);
    assertThat(diffNestedMessage).ignoringRepeatedFieldOrder().isNotEqualTo(nestedMessage);
    assertThat(eqNestedMessage).isNotEqualTo(nestedMessage);
    assertThat(eqNestedMessage).ignoringRepeatedFieldOrder().isEqualTo(nestedMessage);

    assertThatThrownBy(() -> assertThat(eqMessage).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("modified: r_string[0]: \"foo\" -> \"bar\"")
        .hasMessageContaining("modified: r_string[1]: \"bar\" -> \"foo\"");

    assertThatThrownBy(
            () -> assertThat(eqMessage).ignoringRepeatedFieldOrder().isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("moved: r_string[0] -> r_string[1]: \"foo\"")
        .hasMessageContaining("moved: r_string[1] -> r_string[0]: \"bar\"");

    assertThatThrownBy(
            () -> assertThat(diffMessage).ignoringRepeatedFieldOrder().isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("matched: r_string[0]: \"foo\"")
        .hasMessageContaining("moved: r_string[1] -> r_string[2]: \"bar\"")
        .hasMessageContaining("added: r_string[1]: \"foo\"");
  }

  @Test
  public void testRepeatedFieldOrder_scoped() {
    Message message =
        parse("r_string: 'a' r_string: 'b' o_sub_test_message: { r_string: 'c' r_string: 'd' }");
    Message diffSubMessage =
        parse("r_string: 'a' r_string: 'b' o_sub_test_message: { r_string: 'd' r_string: 'c' }");
    Message diffAll =
        parse("r_string: 'b' r_string: 'a' o_sub_test_message: { r_string: 'd' r_string: 'c' }");

    FieldDescriptor rootMessageRepeatedfield = getFieldDescriptor("r_string");
    FieldDescriptor subMessageRepeatedField =
        checkNotNull(
            getFieldDescriptor("o_sub_test_message").getMessageType().findFieldByName("r_string"));

    // Ignoring all repeated field order tests pass.
    assertThat(diffSubMessage).ignoringRepeatedFieldOrder().isEqualTo(message);
    assertThat(diffAll).ignoringRepeatedFieldOrder().isEqualTo(message);

    // Ignoring only some results in failures.
    //
    // TODO(user): Whether we check failure message substrings or not is currently ad-hoc on a
    // per-test basis, and not especially maintainable.  We should make the tests consistent
    // according to some reasonable rule in this regard.
    assertThatThrownBy(
            () ->
                assertThat(diffSubMessage)
                    .ignoringRepeatedFieldOrderOfFieldDescriptors(rootMessageRepeatedfield)
                    .isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("modified: o_sub_test_message.r_string[0]: \"c\" -> \"d\"");
    assertThat(diffSubMessage)
        .ignoringRepeatedFieldOrderOfFieldDescriptors(subMessageRepeatedField)
        .isEqualTo(message);

    assertThat(diffAll)
        .ignoringRepeatedFieldOrderOfFieldDescriptors(rootMessageRepeatedfield)
        .isNotEqualTo(message);
    assertThat(diffAll)
        .ignoringRepeatedFieldOrderOfFieldDescriptors(subMessageRepeatedField)
        .isNotEqualTo(message);
    assertThat(diffAll)
        .ignoringRepeatedFieldOrderOfFieldDescriptors(
            rootMessageRepeatedfield, subMessageRepeatedField)
        .isEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(message)
                    .ignoringRepeatedFieldOrderOfFields(getFieldNumber("o_int"))
                    .isEqualTo(message))
        .hasMessageContaining("o_int")
        .hasMessageContaining("is not a repeated field");
  }

  @Test
  public void testDoubleTolerance() {
    Message message = parse("o_double: 1.0");
    Message diffMessage = parse("o_double: 1.1");

    assertThat(diffMessage).isNotEqualTo(message);
    assertThat(diffMessage).usingDoubleTolerance(0.2).isEqualTo(message);
    assertThat(diffMessage).usingDoubleTolerance(0.05).isNotEqualTo(message);
    assertThat(diffMessage).usingFloatTolerance(0.2f).isNotEqualTo(message);
  }

  @Test
  public void testDoubleTolerance_scoped() {
    Message message = parse("o_double: 1.0 o_double2: 1.0");
    Message diffMessage = parse("o_double: 1.1 o_double2: 1.5");

    int doubleFieldNumber = getFieldNumber("o_double");
    int double2FieldNumber = getFieldNumber("o_double2");

    assertThat(diffMessage).usingDoubleTolerance(0.6).isEqualTo(message);
    assertThat(diffMessage).usingDoubleTolerance(0.2).isNotEqualTo(message);

    // usingDoubleTolerance*() statements override all previous statements.
    assertThat(diffMessage)
        .usingDoubleTolerance(0.2)
        .usingDoubleToleranceForFields(0.6, double2FieldNumber)
        .isEqualTo(message);
    assertThat(diffMessage)
        .usingDoubleToleranceForFields(0.6, doubleFieldNumber, double2FieldNumber)
        .usingDoubleToleranceForFields(0.2, doubleFieldNumber)
        .isEqualTo(message);

    assertThat(diffMessage)
        .usingDoubleTolerance(0.2)
        .usingDoubleToleranceForFields(0.6, doubleFieldNumber)
        .isNotEqualTo(message);
    assertThat(diffMessage)
        .usingDoubleToleranceForFields(0.6, double2FieldNumber)
        .usingDoubleTolerance(0.2)
        .isNotEqualTo(message);
    assertThat(diffMessage)
        .usingDoubleToleranceForFields(0.6, doubleFieldNumber, double2FieldNumber)
        .usingDoubleToleranceForFields(0.2, double2FieldNumber)
        .isNotEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(message)
                    .usingDoubleToleranceForFields(3.14159, getFieldNumber("o_int"))
                    .isEqualTo(message))
        .hasMessageContaining("o_int")
        .hasMessageContaining("is not a double field");
  }

  @Test
  public void testFloatTolerance() {
    Message message = parse("o_float: 1.0");
    Message diffMessage = parse("o_float: 1.1");

    assertThat(diffMessage).isNotEqualTo(message);
    assertThat(diffMessage).usingFloatTolerance(0.2f).isEqualTo(message);
    assertThat(diffMessage).usingFloatTolerance(0.05f).isNotEqualTo(message);
    assertThat(diffMessage).usingDoubleTolerance(0.2).isNotEqualTo(message);
  }

  @Test
  public void testFloatTolerance_scoped() {
    Message message = parse("o_float: 1.0 o_float2: 1.0");
    Message diffMessage = parse("o_float: 1.1 o_float2: 1.5");

    int floatFieldNumber = getFieldNumber("o_float");
    int float2FieldNumber = getFieldNumber("o_float2");

    assertThat(diffMessage).usingFloatTolerance(0.6f).isEqualTo(message);
    assertThat(diffMessage).usingFloatTolerance(0.2f).isNotEqualTo(message);

    // usingFloatTolerance*() statements override all previous statements.
    assertThat(diffMessage)
        .usingFloatTolerance(0.2f)
        .usingFloatToleranceForFields(0.6f, float2FieldNumber)
        .isEqualTo(message);
    assertThat(diffMessage)
        .usingFloatTolerance(0.2f)
        .usingFloatToleranceForFields(0.6f, floatFieldNumber)
        .isNotEqualTo(message);
    assertThat(diffMessage)
        .usingFloatToleranceForFields(0.6f, float2FieldNumber)
        .usingFloatTolerance(0.2f)
        .isNotEqualTo(message);
    assertThat(diffMessage)
        .usingFloatToleranceForFields(0.6f, floatFieldNumber, float2FieldNumber)
        .usingFloatToleranceForFields(0.2f, floatFieldNumber)
        .isEqualTo(message);
    assertThat(diffMessage)
        .usingFloatToleranceForFields(0.6f, floatFieldNumber, float2FieldNumber)
        .usingFloatToleranceForFields(0.2f, float2FieldNumber)
        .isNotEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(message)
                    .usingFloatToleranceForFields(3.1416f, getFieldNumber("o_int"))
                    .isEqualTo(message))
        .hasMessageContaining("o_int")
        .hasMessageContaining("is not a float field");
  }

  @Test
  public void testComparingExpectedFieldsOnly() {
    Message message = parse("o_int: 3 r_string: 'foo'");
    Message narrowMessage = parse("o_int: 3");

    assertThat(message).comparingExpectedFieldsOnly().isEqualTo(narrowMessage);
    assertThat(narrowMessage).comparingExpectedFieldsOnly().isNotEqualTo(message);

    assertThatThrownBy(
            () -> assertThat(message).comparingExpectedFieldsOnly().isNotEqualTo(narrowMessage))
        .hasMessageContaining("ignored: r_string");
  }

  @Test
  public void testIgnoringExtraRepeatedFieldElements_respectingOrder() {
    Message message = parse("r_string: 'foo' r_string: 'bar'");
    Message eqMessage = parse("r_string: 'foo' r_string: 'foobar' r_string: 'bar'");
    Message diffMessage = parse("r_string: 'bar' r_string: 'foobar' r_string: 'foo'");

    assertThat(eqMessage).ignoringExtraRepeatedFieldElements().isEqualTo(message);
    assertThat(diffMessage).ignoringExtraRepeatedFieldElements().isNotEqualTo(message);

    assertThatThrownBy(
            () -> assertThat(eqMessage).ignoringExtraRepeatedFieldElements().isNotEqualTo(message))
        .hasMessageContaining("ignored: r_string[?] -> r_string[1]: \"foobar\"");

    assertThatThrownBy(
            () -> assertThat(diffMessage).ignoringExtraRepeatedFieldElements().isEqualTo(message))
        .hasMessageContaining("out_of_order: r_string[1] -> r_string[0]: \"bar\"")
        .hasMessageContaining("moved: r_string[0] -> r_string[2]: \"foo\"");
  }

  @Test
  public void testIgnoringExtraRepeatedFieldElements_ignoringOrder() {
    Message message = parse("r_string: 'foo' r_string: 'bar'");
    Message eqMessage = parse("r_string: 'baz' r_string: 'bar' r_string: 'qux' r_string: 'foo'");
    Message diffMessage = parse("r_string: 'abc' r_string: 'foo' r_string: 'xyz'");

    assertThat(eqMessage)
        .ignoringExtraRepeatedFieldElements()
        .ignoringRepeatedFieldOrder()
        .isEqualTo(message);
    assertThat(diffMessage)
        .ignoringExtraRepeatedFieldElements()
        .ignoringRepeatedFieldOrder()
        .isNotEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(diffMessage)
                    .ignoringExtraRepeatedFieldElements()
                    .ignoringRepeatedFieldOrder()
                    .isEqualTo(message))
        .hasMessageContaining("moved: r_string[0] -> r_string[1]: \"foo\"")
        .hasMessageContaining("deleted: r_string[1]: \"bar\"");
  }

  @Test
  public void testIgnoringExtraRepeatedFieldElements_empty() {
    Message message = parse("o_int: 2");
    Message diffMessage = parse("o_int: 2 r_string: 'error'");

    assertThat(diffMessage).ignoringExtraRepeatedFieldElements().isNotEqualTo(message);
    assertThat(diffMessage)
        .ignoringExtraRepeatedFieldElements()
        .ignoringRepeatedFieldOrder()
        .isNotEqualTo(message);

    assertThat(diffMessage)
        .comparingExpectedFieldsOnly()
        .ignoringExtraRepeatedFieldElements()
        .isEqualTo(message);
    assertThat(diffMessage)
        .comparingExpectedFieldsOnly()
        .ignoringExtraRepeatedFieldElements()
        .ignoringRepeatedFieldOrder()
        .isEqualTo(message);
  }

  @Test
  public void testIgnoringExtraRepeatedFieldElements_scoped() {
    Message message = parse("r_string: 'a' o_sub_test_message: { r_string: 'c' }");
    Message diffMessage =
        parse("r_string: 'a' o_sub_test_message: { r_string: 'b' r_string: 'c' }");

    FieldDescriptor rootRepeatedField = getFieldDescriptor("r_string");
    FieldDescriptor subMessageRepeatedField =
        checkNotNull(
            getFieldDescriptor("o_sub_test_message").getMessageType().findFieldByName("r_string"));

    assertThat(diffMessage).ignoringExtraRepeatedFieldElements().isEqualTo(message);
    assertThat(diffMessage)
        .ignoringExtraRepeatedFieldElementsOfFieldDescriptors(rootRepeatedField)
        .isNotEqualTo(message);
    assertThat(diffMessage)
        .ignoringExtraRepeatedFieldElementsOfFieldDescriptors(subMessageRepeatedField)
        .isEqualTo(message);
    assertThat(diffMessage)
        .ignoringExtraRepeatedFieldElementsOfFieldDescriptors(
            rootRepeatedField, subMessageRepeatedField)
        .isEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(message)
                    .ignoringExtraRepeatedFieldElementsOfFields(getFieldNumber("o_int"))
                    .isEqualTo(message))
        .hasMessageContaining("o_int")
        .hasMessageContaining("it cannot contain extra elements");
  }

  // Utility which fills a proto map field, based on the java.util.Map.
  private Message makeProtoMap(Map<String, Integer> map) {
    StringBuilder textProto = new StringBuilder();
    for (String key : map.keySet()) {
      int value = map.get(key);
      textProto
          .append("test_message_map { key: '")
          .append(key)
          .append("' value { o_int: ")
          .append(value)
          .append(" } } ");
    }
    return parse(textProto.toString());
  }

  @Test
  public void testIgnoringExtraRepeatedFieldElements_map() {
    Message message = makeProtoMap(ImmutableMap.of("foo", 2, "bar", 3));
    Message eqMessage = makeProtoMap(ImmutableMap.of("bar", 3, "qux", 4, "foo", 2));
    Message diffMessage = makeProtoMap(ImmutableMap.of("quz", 5, "foo", 2));
    Message emptyMessage = parse("");

    assertThat(eqMessage).ignoringExtraRepeatedFieldElements().isEqualTo(message);
    assertThat(eqMessage)
        .ignoringRepeatedFieldOrder()
        .ignoringExtraRepeatedFieldElements()
        .isEqualTo(message);
    assertThat(diffMessage).ignoringExtraRepeatedFieldElements().isNotEqualTo(message);
    assertThat(diffMessage)
        .ignoringExtraRepeatedFieldElements()
        .ignoringRepeatedFieldOrder()
        .isNotEqualTo(message);

    assertThat(message).ignoringExtraRepeatedFieldElements().isNotEqualTo(emptyMessage);

    assertThatThrownBy(
            () -> assertThat(diffMessage).ignoringExtraRepeatedFieldElements().isEqualTo(message))
        .hasMessageContaining("matched: test_message_map[\"foo\"].o_int: 2")
        .hasMessageContaining("ignored: test_message_map[\"quz\"]")
        .hasMessageContaining("deleted: test_message_map[\"bar\"]");
  }

  @Test
  public void testReportingMismatchesOnly_isEqualTo() {
    Message message = parse("r_string: \"foo\" r_string: \"bar\"");
    Message diffMessage = parse("r_string: \"foo\" r_string: \"not_bar\"");

    assertThatThrownBy(() -> assertThat(diffMessage).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("foo")
        .hasMessageContaining("bar")
        .hasMessageContaining("not_bar");

    assertThatThrownBy(() -> assertThat(diffMessage).reportingMismatchesOnly().isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .satisfies(e -> assertThat(e.getMessage()).doesNotContain("foo"))
        .hasMessageContaining("bar")
        .hasMessageContaining("not_bar");
  }

  @Test
  public void testReportingMismatchesOnly_isNotEqualTo() {
    Message message = parse("o_int: 33 r_string: \"foo\" r_string: \"bar\"");
    Message diffMessage = parse("o_int: 33 r_string: \"bar\" r_string: \"foo\"");

    assertThatThrownBy(
            () -> assertThat(diffMessage).ignoringRepeatedFieldOrder().isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("33")
        .hasMessageContaining("foo")
        .hasMessageContaining("bar");

    assertThatThrownBy(
            () ->
                assertThat(diffMessage)
                    .ignoringRepeatedFieldOrder()
                    .reportingMismatchesOnly()
                    .isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .satisfies(
            e ->
                assertThat(e.getMessage())
                    .doesNotContain("33")
                    .doesNotContain("foo")
                    .doesNotContain("bar"));
  }

  @Test
  public void testHasAllRequiredFields() {
    // Proto 3 doesn't have required fields.
    if (isProto3()) {
      return;
    }

    assertThat(parsePartial("")).hasAllRequiredFields();
    assertThat(parsePartial("o_required_string_message: { required_string: \"foo\" }"))
        .hasAllRequiredFields();

    assertThatThrownBy(
            () -> assertThat(parsePartial("o_required_string_message: {}")).hasAllRequiredFields())
        // For some reason, can't get upstream regex to match even though it looks like it does,
        // so just relax a bit.
        .hasMessageContaining("Not true that")
        .hasMessageContaining("all required fields set. Missing:")
        .hasMessageContaining("[o_required_string_message.required_string]");

    assertThatThrownBy(
            () ->
                assertThat(
                        parsePartial("r_required_string_message: {} r_required_string_message: {}"))
                    .hasAllRequiredFields())
        // For some reason, can't get upstream regex to match even though it looks like it does,
        // so just relax a bit.
        .hasMessageContaining("Not true that")
        .hasMessageContaining("all required fields set. Missing:")
        .hasMessageContaining("r_required_string_message[0].required_string")
        .hasMessageContaining("r_required_string_message[1].required_string");
  }
}
