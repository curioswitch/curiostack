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
/*
 * Copyright (c) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.curioswitch.common.testing.assertj.proto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.curioswitch.common.testing.assertj.proto.ProtoAssert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.UnknownFieldSet.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Unit tests for {@link FieldScope}, and their interaction with {@link ProtoAssert}. */
@RunWith(Parameterized.class)
public class FieldScopesTest extends ProtoAssertTestBase {

  @Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    return ProtoAssertTestBase.parameters();
  }

  // Set up for the ignoringTopLevelField tests.
  // ignoringFieldMessage and ignoringFieldDiffMessage are simple messages with two fields set. They
  // are the same for the "good" field, and different for the "bad" field. The *FieldNumber and
  // *FieldDescriptor members point to these fields.

  private final Message ignoringFieldMessage;
  private final Message ignoringFieldDiffMessage;
  private final int goodFieldNumber;
  private final int badFieldNumber;
  private final FieldDescriptor goodFieldDescriptor;
  private final FieldDescriptor badFieldDescriptor;

  public FieldScopesTest(TestType testType) {
    super(testType);

    ignoringFieldMessage = parse("o_int: 3 r_string: \"foo\"");
    ignoringFieldDiffMessage = parse("o_int: 3 r_string: \"bar\"");
    goodFieldNumber = getFieldNumber("o_int");
    badFieldNumber = getFieldNumber("r_string");
    goodFieldDescriptor = getFieldDescriptor("o_int");
    badFieldDescriptor = getFieldDescriptor("r_string");
  }

  @Test
  public void testUnequalMessages() {
    Message message = parse("o_int: 3 r_string: \"foo\"");
    Message diffMessage = parse("o_int: 5 r_string: \"bar\"");

    assertThat(diffMessage).isNotEqualTo(message);
  }

  @Test
  public void testFieldScopes_all() {
    Message message = parse("o_int: 3 r_string: \"foo\"");
    Message diffMessage = parse("o_int: 5 r_string: \"bar\"");

    assertThat(diffMessage).withPartialScope(FieldScopes.all()).isNotEqualTo(message);
    assertThat(diffMessage).ignoringFieldScope(FieldScopes.all()).isEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(diffMessage).ignoringFieldScope(FieldScopes.all()).isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("ignored: o_int")
        .hasMessageContaining("ignored: r_string");
  }

  @Test
  public void testFieldScopes_none() {
    Message message = parse("o_int: 3 r_string: \"foo\"");
    Message diffMessage = parse("o_int: 5 r_string: \"bar\"");

    assertThat(diffMessage).ignoringFieldScope(FieldScopes.none()).isNotEqualTo(message);
    assertThat(diffMessage).withPartialScope(FieldScopes.none()).isEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(diffMessage).withPartialScope(FieldScopes.none()).isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("ignored: o_int")
        .hasMessageContaining("ignored: r_string");
  }

  @Test
  public void testIgnoringTopLevelField_ignoringField() {
    assertThat(ignoringFieldDiffMessage)
        .ignoringFields(goodFieldNumber)
        .isNotEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .ignoringFields(badFieldNumber)
        .isEqualTo(ignoringFieldMessage);

    assertThatThrownBy(
            () ->
                assertThat(ignoringFieldDiffMessage)
                    .ignoringFields(goodFieldNumber)
                    .isEqualTo(ignoringFieldMessage))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("modified: r_string[0]: \"foo\" -> \"bar\"");

    assertThatThrownBy(
            () ->
                assertThat(ignoringFieldDiffMessage)
                    .ignoringFields(badFieldNumber)
                    .isNotEqualTo(ignoringFieldMessage))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("ignored: r_string");
  }

  @Test
  public void testIgnoringTopLevelField_fieldScopes_ignoringFields() {
    assertThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.ignoringFields(goodFieldNumber))
        .isNotEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.ignoringFields(goodFieldNumber))
        .isEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.ignoringFields(badFieldNumber))
        .isEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.ignoringFields(badFieldNumber))
        .isNotEqualTo(ignoringFieldMessage);
  }

  @Test
  public void testIgnoringTopLevelField_fieldScopes_allowingFields() {
    assertThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.allowingFields(goodFieldNumber))
        .isEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.allowingFields(goodFieldNumber))
        .isNotEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.allowingFields(badFieldNumber))
        .isNotEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.allowingFields(badFieldNumber))
        .isEqualTo(ignoringFieldMessage);
  }

  @Test
  public void testIgnoringTopLevelField_fieldScopes_allowingFieldDescriptors() {
    assertThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.allowingFieldDescriptors(goodFieldDescriptor))
        .isEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.allowingFieldDescriptors(goodFieldDescriptor))
        .isNotEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.allowingFieldDescriptors(badFieldDescriptor))
        .isNotEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.allowingFieldDescriptors(badFieldDescriptor))
        .isEqualTo(ignoringFieldMessage);
  }

  @Test
  public void testIgnoringTopLevelField_fieldScopes_ignoringFieldDescriptors() {
    assertThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.ignoringFieldDescriptors(goodFieldDescriptor))
        .isNotEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.ignoringFieldDescriptors(goodFieldDescriptor))
        .isEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.ignoringFieldDescriptors(badFieldDescriptor))
        .isEqualTo(ignoringFieldMessage);
    assertThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.ignoringFieldDescriptors(badFieldDescriptor))
        .isNotEqualTo(ignoringFieldMessage);
  }

  @Test
  public void testEmptySubMessage() {
    Message message = parse("o_int: 1 o_sub_test_message: { }");
    Message eqMessage = parse("o_int: 2 o_sub_test_message: { }");
    Message diffMessage = parse("o_int: 3");

    // Different logic gets exercised when we add an 'ignore' clause.
    // Let's ensure o_sub_test_message is compared properly in all cases.
    int fieldNumber = getFieldNumber("o_int");

    assertThat(eqMessage).isNotEqualTo(message);
    assertThat(eqMessage).ignoringFieldAbsence().isNotEqualTo(message);
    assertThat(eqMessage).ignoringFields(fieldNumber).isEqualTo(message);
    assertThat(eqMessage).ignoringFields(fieldNumber).ignoringFieldAbsence().isEqualTo(message);

    assertThat(diffMessage).isNotEqualTo(message);
    assertThat(diffMessage).ignoringFieldAbsence().isNotEqualTo(message);
    assertThat(diffMessage).ignoringFields(fieldNumber).isNotEqualTo(message);
    assertThat(diffMessage).ignoringFields(fieldNumber).ignoringFieldAbsence().isEqualTo(message);
  }

  @Test
  public void testIgnoreSubMessageField() {
    Message message = parse("o_int: 1 o_sub_test_message: { o_int: 2 }");
    Message diffMessage = parse("o_int: 2 o_sub_test_message: { o_int: 2 }");
    Message eqMessage1 = parse("o_int: 1");
    Message eqMessage2 = parse("o_int: 1 o_sub_test_message: {}");
    Message eqMessage3 = parse("o_int: 1 o_sub_test_message: { o_int: 3 r_string: \"x\" }");
    int fieldNumber = getFieldNumber("o_sub_test_message");

    assertThat(diffMessage).ignoringFields(fieldNumber).isNotEqualTo(message);
    assertThat(eqMessage1).ignoringFields(fieldNumber).isEqualTo(message);
    assertThat(eqMessage2).ignoringFields(fieldNumber).isEqualTo(message);
    assertThat(eqMessage3).ignoringFields(fieldNumber).isEqualTo(message);

    assertThatThrownBy(() -> assertThat(diffMessage).ignoringFields(fieldNumber).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("modified: o_int: 1 -> 2");

    assertThatThrownBy(
            () -> assertThat(eqMessage3).ignoringFields(fieldNumber).isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("ignored: o_sub_test_message");
  }

  @Test
  public void testIgnoreFieldOfSubMessage() {
    // Ignore o_int of sub message fields.
    Message message = parse("o_int: 1 o_sub_test_message: { o_int: 2 r_string: \"foo\" }");
    Message diffMessage1 = parse("o_int: 2 o_sub_test_message: { o_int: 2 r_string: \"foo\" }");
    Message diffMessage2 = parse("o_int: 1 o_sub_test_message: { o_int: 2 r_string: \"bar\" }");
    Message eqMessage = parse("o_int: 1 o_sub_test_message: { o_int: 3 r_string: \"foo\" }");

    FieldDescriptor fieldDescriptor =
        getFieldDescriptor("o_sub_test_message").getMessageType().findFieldByName("o_int");
    FieldScope partialScope = FieldScopes.ignoringFieldDescriptors(fieldDescriptor);

    assertThat(diffMessage1).withPartialScope(partialScope).isNotEqualTo(message);
    assertThat(diffMessage2).withPartialScope(partialScope).isNotEqualTo(message);
    assertThat(eqMessage).withPartialScope(partialScope).isEqualTo(message);

    assertThatThrownBy(
            () -> assertThat(diffMessage1).withPartialScope(partialScope).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("modified: o_int: 1 -> 2");

    assertThatThrownBy(
            () -> assertThat(diffMessage2).withPartialScope(partialScope).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("modified: o_sub_test_message.r_string[0]: \"foo\" -> \"bar\"");
  }

  @Test
  public void testIgnoringAllButOneFieldOfSubMessage() {
    // Consider all of TestMessage, but none of o_sub_test_message, except
    // o_sub_test_message.o_int.
    Message message =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 4 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"bar\" }");

    // All of these differ in a critical field.
    Message diffMessage1 =
        parse(
            "o_int: 999999 o_sub_test_message: { o_int: 4 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"bar\" }");
    Message diffMessage2 =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 999999 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"bar\" }");
    Message diffMessage3 =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 4 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 999999 r_string: \"bar\" }");
    Message diffMessage4 =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 4 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"999999\" }");

    // This one only differs in o_sub_test_message.r_string, which is ignored.
    Message eqMessage =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 4 r_string: \"999999\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"bar\" }");

    FieldScope fieldScope =
        FieldScopes.ignoringFields(getFieldNumber("o_sub_test_message"))
            .allowingFieldDescriptors(
                getFieldDescriptor("o_sub_test_message").getMessageType().findFieldByName("o_int"));

    assertThat(diffMessage1).withPartialScope(fieldScope).isNotEqualTo(message);
    assertThat(diffMessage2).withPartialScope(fieldScope).isNotEqualTo(message);
    assertThat(diffMessage3).withPartialScope(fieldScope).isNotEqualTo(message);
    assertThat(diffMessage4).withPartialScope(fieldScope).isNotEqualTo(message);
    assertThat(eqMessage).withPartialScope(fieldScope).isEqualTo(message);

    assertThatThrownBy(
            () -> assertThat(diffMessage4).withPartialScope(fieldScope).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("modified: r_sub_test_message[0].r_string[0]: \"bar\" -> \"999999\"");

    assertThatThrownBy(
            () -> assertThat(eqMessage).withPartialScope(fieldScope).isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("ignored: o_sub_test_message.r_string");
  }

  @Test
  public void testFromSetFields() {
    Message scopeMessage =
        parse(
            "o_int: 1 r_string: \"x\" o_test_message: { o_int: 1 } "
                + "r_test_message: { r_string: \"x\" } r_test_message: { o_int: 1 } "
                + "o_sub_test_message: { o_test_message: { o_int: 1 } }");

    // 1 = compared, [2, 3] = ignored, 4 = compared and fails
    Message message =
        parse(
            "o_int: 1 r_string: \"1\" o_test_message: {o_int: 1 r_string: \"2\" } "
                + "r_test_message: { o_int: 1 r_string: \"1\" } "
                + "r_test_message: { o_int: 1 r_string: \"1\" } "
                + "o_sub_test_message: { o_int: 2 o_test_message: { o_int: 1 r_string: \"2\" } }");
    Message diffMessage =
        parse(
            "o_int: 4 r_string: \"4\" o_test_message: {o_int: 4 r_string: \"3\" } "
                + "r_test_message: { o_int: 4 r_string: \"4\" } "
                + "r_test_message: { o_int: 4 r_string: \"4\" }"
                + "o_sub_test_message: { r_string: \"3\" o_int: 3 "
                + "o_test_message: { o_int: 4 r_string: \"3\" } }");
    Message eqMessage =
        parse(
            "o_int: 1 r_string: \"1\" o_test_message: {o_int: 1 r_string: \"3\" } "
                + "r_test_message: { o_int: 1 r_string: \"1\" } "
                + "r_test_message: { o_int: 1 r_string: \"1\" }"
                + "o_sub_test_message: { o_int: 3 o_test_message: { o_int: 1 r_string: \"3\" } }");

    assertThat(diffMessage).isNotEqualTo(message);
    assertThat(eqMessage).isNotEqualTo(message);

    assertThat(diffMessage)
        .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
        .isNotEqualTo(message);
    assertThat(eqMessage)
        .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
        .isEqualTo(message);

    assertThatThrownBy(() -> assertThat(diffMessage).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("1 -> 4")
        .hasMessageContaining("\"1\" -> \"4\"")
        .hasMessageContaining("2 -> 3")
        .hasMessageContaining("\"2\" -> \"3\"");

    assertThatThrownBy(
            () ->
                assertThat(diffMessage)
                    .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
                    .isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("1 -> 4")
        .hasMessageContaining("\"1\" -> \"4\"")
        .satisfies(
            e ->
                assertThat(e.getMessage())
                    .doesNotContain("2 -> 3")
                    .doesNotContain("\"2\" -> \"3\""));

    assertThatThrownBy(
            () ->
                assertThat(eqMessage)
                    .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
                    .isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("ignored: o_test_message.r_string")
        .hasMessageContaining("ignored: o_sub_test_message.o_int")
        .hasMessageContaining("ignored: o_sub_test_message.o_test_message.r_string");
  }

  @Ignore // Ignored upstream.
  public void testFromSetFields_unknownFields() throws InvalidProtocolBufferException {
    if (isProto3()) {
      // No unknown fields in Proto 3.
      return;
    }

    // Make sure that merging of repeated fields, separation by tag number, and separation by
    // unknown field type all work.
    Message scopeMessage =
        fromUnknownFields(
            UnknownFieldSet.newBuilder()
                .addField(20, Field.newBuilder().addFixed32(1).addFixed64(1).build())
                .addField(
                    21,
                    Field.newBuilder()
                        .addVarint(1)
                        .addLengthDelimited(ByteString.copyFrom("1", StandardCharsets.UTF_8))
                        .addGroup(
                            UnknownFieldSet.newBuilder()
                                .addField(1, Field.newBuilder().addFixed32(1).build())
                                .build())
                        .addGroup(
                            UnknownFieldSet.newBuilder()
                                .addField(2, Field.newBuilder().addFixed64(1).build())
                                .build())
                        .build())
                .build());

    // 1 = compared, [2, 3] = ignored, 4 = compared and fails
    Message message =
        fromUnknownFields(
            UnknownFieldSet.newBuilder()
                .addField(19, Field.newBuilder().addFixed32(2).addFixed64(2).build())
                .addField(
                    20,
                    Field.newBuilder()
                        .addFixed32(1)
                        .addFixed64(1)
                        .addVarint(2)
                        .addLengthDelimited(ByteString.copyFrom("2", StandardCharsets.UTF_8))
                        .addGroup(
                            UnknownFieldSet.newBuilder()
                                .addField(1, Field.newBuilder().addFixed32(2).build())
                                .build())
                        .build())
                .addField(
                    21,
                    Field.newBuilder()
                        .addFixed32(2)
                        .addFixed64(2)
                        .addVarint(1)
                        .addLengthDelimited(ByteString.copyFrom("1", StandardCharsets.UTF_8))
                        .addGroup(
                            UnknownFieldSet.newBuilder()
                                .addField(1, Field.newBuilder().addFixed32(1).addFixed64(2).build())
                                .addField(2, Field.newBuilder().addFixed32(2).addFixed64(1).build())
                                .addField(3, Field.newBuilder().addFixed32(2).build())
                                .build())
                        .build())
                .build());
    Message diffMessage =
        fromUnknownFields(
            UnknownFieldSet.newBuilder()
                .addField(19, Field.newBuilder().addFixed32(3).addFixed64(3).build())
                .addField(
                    20,
                    Field.newBuilder()
                        .addFixed32(4)
                        .addFixed64(4)
                        .addVarint(3)
                        .addLengthDelimited(ByteString.copyFrom("3", StandardCharsets.UTF_8))
                        .addGroup(
                            UnknownFieldSet.newBuilder()
                                .addField(1, Field.newBuilder().addFixed32(3).build())
                                .build())
                        .build())
                .addField(
                    21,
                    Field.newBuilder()
                        .addFixed32(3)
                        .addFixed64(3)
                        .addVarint(4)
                        .addLengthDelimited(ByteString.copyFrom("4", StandardCharsets.UTF_8))
                        .addGroup(
                            UnknownFieldSet.newBuilder()
                                .addField(1, Field.newBuilder().addFixed32(4).addFixed64(3).build())
                                .addField(2, Field.newBuilder().addFixed32(3).addFixed64(4).build())
                                .addField(3, Field.newBuilder().addFixed32(3).build())
                                .build())
                        .build())
                .build());
    Message eqMessage =
        fromUnknownFields(
            UnknownFieldSet.newBuilder()
                .addField(19, Field.newBuilder().addFixed32(3).addFixed64(3).build())
                .addField(
                    20,
                    Field.newBuilder()
                        .addFixed32(1)
                        .addFixed64(1)
                        .addVarint(3)
                        .addLengthDelimited(ByteString.copyFrom("3", StandardCharsets.UTF_8))
                        .addGroup(
                            UnknownFieldSet.newBuilder()
                                .addField(1, Field.newBuilder().addFixed32(3).build())
                                .build())
                        .build())
                .addField(
                    21,
                    Field.newBuilder()
                        .addFixed32(3)
                        .addFixed64(3)
                        .addVarint(1)
                        .addLengthDelimited(ByteString.copyFrom("1", StandardCharsets.UTF_8))
                        .addGroup(
                            UnknownFieldSet.newBuilder()
                                .addField(1, Field.newBuilder().addFixed32(1).addFixed64(3).build())
                                .addField(2, Field.newBuilder().addFixed32(3).addFixed64(1).build())
                                .addField(3, Field.newBuilder().addFixed32(3).build())
                                .build())
                        .build())
                .build());

    assertThat(diffMessage).isNotEqualTo(message);
    assertThat(eqMessage).isNotEqualTo(message);

    assertThat(diffMessage)
        .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
        .isNotEqualTo(message);
    assertThat(eqMessage)
        .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
        .isEqualTo(message);

    assertThatThrownBy(() -> assertThat(diffMessage).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("1 -> 4")
        .hasMessageContaining("\"1\" -> \"4\"")
        .hasMessageContaining("2 -> 3")
        .hasMessageContaining("\"2\" -> \"3\"");

    assertThatThrownBy(
            () ->
                assertThat(diffMessage)
                    .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
                    .isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("1 -> 4")
        .hasMessageContaining("\"1\" -> \"4\"")
        .satisfies(
            t ->
                assertThat(t.getMessage())
                    .doesNotContain("2 -> 3")
                    .doesNotContain("\"2\" -> \"3\""));

    assertThatThrownBy(
            () ->
                assertThat(eqMessage)
                    .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
                    .isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("1 -> 4")
        .hasMessageContaining("\"1\" -> \"4\"")
        .satisfies(
            t ->
                assertThat(t.getMessage())
                    .doesNotContain("2 -> 3")
                    .doesNotContain("\"2\" -> \"3\""));
  }

  @Test
  public void testFieldNumbersAreRecursive() {
    // o_int is compared, r_string is not.
    Message message =
        parse("o_int: 1 r_string: \"foo\" r_test_message: { o_int: 2 r_string: \"bar\" }");
    Message diffMessage =
        parse("o_int: 2 r_string: \"bar\" r_test_message: { o_int: 1 r_string: \"foo\" }");
    Message eqMessage =
        parse("o_int: 1 r_string: \"bar\" r_test_message: { o_int: 2 r_string: \"foo\" }");
    int fieldNumber = getFieldNumber("o_int");
    FieldDescriptor fieldDescriptor = getFieldDescriptor("o_int");

    assertThat(diffMessage)
        .withPartialScope(FieldScopes.allowingFields(fieldNumber))
        .isNotEqualTo(message);
    assertThat(eqMessage)
        .withPartialScope(FieldScopes.allowingFields(fieldNumber))
        .isEqualTo(message);
    assertThat(diffMessage)
        .withPartialScope(FieldScopes.allowingFieldDescriptors(fieldDescriptor))
        .isNotEqualTo(message);
    assertThat(eqMessage)
        .withPartialScope(FieldScopes.allowingFieldDescriptors(fieldDescriptor))
        .isEqualTo(message);

    assertThatThrownBy(
            () ->
                assertThat(diffMessage)
                    .withPartialScope(FieldScopes.allowingFields(fieldNumber))
                    .isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("modified: o_int: 1 -> 2")
        .hasMessageContaining("modified: r_test_message[0].o_int: 2 -> 1");

    assertThatThrownBy(
            () ->
                assertThat(eqMessage)
                    .withPartialScope(FieldScopes.allowingFields(fieldNumber))
                    .isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("ignored: r_test_message[0].r_string");
  }

  @Test
  public void testMultipleFieldNumbers() {
    Message message = parse("o_int: 1 r_string: \"x\" o_enum: TWO");
    Message diffMessage = parse("o_int: 2 r_string: \"y\" o_enum: TWO");
    Message eqMessage =
        parse("o_int: 1 r_string: \"x\" o_enum: ONE o_sub_test_message: { r_string: \"bar\" }");

    FieldScope fieldScope =
        FieldScopes.allowingFields(getFieldNumber("o_int"), getFieldNumber("r_string"));

    assertThat(diffMessage).withPartialScope(fieldScope).isNotEqualTo(message);
    assertThat(eqMessage).withPartialScope(fieldScope).isEqualTo(message);

    assertThatThrownBy(
            () -> assertThat(diffMessage).withPartialScope(fieldScope).isEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isEqualToFailureMessage())
        .hasMessageContaining("modified: o_int: 1 -> 2")
        .hasMessageContaining("modified: r_string[0]: \"x\" -> \"y\"");

    assertThatThrownBy(
            () -> assertThat(eqMessage).withPartialScope(fieldScope).isNotEqualTo(message))
        .isInstanceOf(AssertionError.class)
        .satisfies(isNotEqualToFailureMessage())
        .hasMessageContaining("ignored: o_enum")
        .hasMessageContaining("ignored: o_sub_test_message");
  }

  @Test
  public void testInvalidFieldNumber() {
    Message message1 = parse("o_int: 44");
    Message message2 = parse("o_int: 33");

    assertThatThrownBy(() -> assertThat(message1).ignoringFields(999).isEqualTo(message2))
        .hasStackTraceContaining(
            "Message type " + fullMessageName() + " has no field with number 999.");
  }

  @Test
  public void testIgnoreFieldsAtDifferentLevels() {
    // Ignore all 'o_int' fields, in different ways.
    Message message =
        parse(
            "o_int: 1 r_string: \"foo\" o_sub_test_message: { o_int: 2 "
                + "o_sub_sub_test_message: { o_int: 3 r_string: \"bar\" } }");

    // Even though o_int is ignored, message presence is not.  So these all fail.
    Message diffMessage1 = parse("r_string: \"baz\"");
    Message diffMessage2 = parse("r_string: \"foo\"");
    Message diffMessage3 = parse("r_string: \"foo\" o_sub_test_message: {}");
    Message diffMessage4 =
        parse("r_string: \"foo\" o_sub_test_message: { o_sub_sub_test_message: {} }");

    // All of these messages are equivalent, because all o_int are ignored.
    Message eqMessage1 =
        parse(
            "o_int: 111 r_string: \"foo\" o_sub_test_message: { o_int: 222 "
                + "o_sub_sub_test_message: { o_int: 333 r_string: \"bar\" } }");
    Message eqMessage2 =
        parse(
            "o_int: 1 r_string: \"foo\" o_sub_test_message: { o_int: 2 "
                + "o_sub_sub_test_message: { o_int: 3 r_string: \"bar\" } }");
    Message eqMessage3 =
        parse(
            "r_string: \"foo\" o_sub_test_message: { "
                + "o_sub_sub_test_message: { r_string: \"bar\" } }");
    Message eqMessage4 =
        parse(
            "o_int: 333 r_string: \"foo\" o_sub_test_message: { o_int: 111 "
                + "o_sub_sub_test_message: { o_int: 222 r_string: \"bar\" } }");

    FieldDescriptor top = getFieldDescriptor("o_int");
    FieldDescriptor middle =
        getFieldDescriptor("o_sub_test_message").getMessageType().findFieldByName("o_int");
    FieldDescriptor bottom =
        getFieldDescriptor("o_sub_test_message")
            .getMessageType()
            .findFieldByName("o_sub_sub_test_message")
            .getMessageType()
            .findFieldByName("o_int");

    ImmutableMap<String, FieldScope> fieldScopes =
        ImmutableMap.of(
            "BASIC",
            FieldScopes.ignoringFieldDescriptors(top, middle, bottom),
            "CHAINED",
            FieldScopes.ignoringFieldDescriptors(top)
                .ignoringFieldDescriptors(middle)
                .ignoringFieldDescriptors(bottom),
            "REPEATED",
            FieldScopes.ignoringFieldDescriptors(top, middle)
                .ignoringFieldDescriptors(middle, bottom));

    for (String scopeName : fieldScopes.keySet()) {
      String msg = "FieldScope(" + scopeName + ")";
      FieldScope scope = fieldScopes.get(scopeName);

      assertThat(diffMessage1).as(msg).withPartialScope(scope).isNotEqualTo(message);
      assertThat(diffMessage2).as(msg).withPartialScope(scope).isNotEqualTo(message);
      assertThat(diffMessage3).as(msg).withPartialScope(scope).isNotEqualTo(message);
      assertThat(diffMessage4).as(msg).withPartialScope(scope).isNotEqualTo(message);

      assertThat(eqMessage1).as(msg).withPartialScope(scope).isEqualTo(message);
      assertThat(eqMessage2).as(msg).withPartialScope(scope).isEqualTo(message);
      assertThat(eqMessage3).as(msg).withPartialScope(scope).isEqualTo(message);
      assertThat(eqMessage4).as(msg).withPartialScope(scope).isEqualTo(message);
    }
  }

  @Test
  public void testFromSetFields_iterables_errorForDifferentMessageTypes() {
    // Don't run this test twice.
    if (!testIsRunOnce()) {
      return;
    }

    assertThatThrownBy(
            () ->
                FieldScopes.fromSetFields(
                    TestMessage2.newBuilder().setOInt(2).build(),
                    TestMessage3.newBuilder().setOInt(2).build()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Cannot create scope from messages with different descriptors")
        .hasMessageContaining(TestMessage2.getDescriptor().getFullName())
        .hasMessageContaining(TestMessage3.getDescriptor().getFullName());
  }

  @Test
  public void testFromSetFields_iterables_errorIfDescriptorMismatchesSubject() {
    // Don't run this test twice.
    if (!testIsRunOnce()) {
      return;
    }

    Message message =
        TestMessage2.newBuilder().setOInt(1).addRString("foo").addRString("bar").build();
    Message eqMessage =
        TestMessage2.newBuilder().setOInt(1).addRString("foo").addRString("bar").build();

    assertThatThrownBy(
            () ->
                assertThat(message)
                    .withPartialScope(
                        FieldScopes.fromSetFields(
                            TestMessage3.newBuilder().setOInt(2).build(),
                            TestMessage3.newBuilder().addRString("foo").build()))
                    .isEqualTo(eqMessage))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining(
            "Message given to FieldScopes.fromSetFields() "
                + "does not have the same descriptor as the message being tested")
        .hasMessageContaining(TestMessage2.getDescriptor().getFullName())
        .hasMessageContaining(TestMessage3.getDescriptor().getFullName());
  }

  @Test
  public void testIterableFieldScopeMethodVariants_protoSubject() {
    Message message = parse("o_int: 1 r_string: \"foo\"");
    Message eqExceptInt = parse("o_int: 2 r_string: \"foo\"");

    assertThat(message).ignoringFields(listOf(getFieldNumber("o_int"))).isEqualTo(eqExceptInt);
    assertThat(message)
        .reportingMismatchesOnly()
        .ignoringFields(listOf(getFieldNumber("o_int")))
        .isEqualTo(eqExceptInt);
    assertThat(message)
        .ignoringFieldScope(FieldScopes.allowingFields(listOf(getFieldNumber("o_int"))))
        .isEqualTo(eqExceptInt);
    assertThat(message)
        .withPartialScope(FieldScopes.ignoringFields(listOf(getFieldNumber("o_int"))))
        .isEqualTo(eqExceptInt);
    assertThat(message)
        .ignoringFieldDescriptors(listOf(getFieldDescriptor("o_int")))
        .isEqualTo(eqExceptInt);
    assertThat(message)
        .reportingMismatchesOnly()
        .ignoringFieldDescriptors(listOf(getFieldDescriptor("o_int")))
        .isEqualTo(eqExceptInt);
    assertThat(message)
        .ignoringFieldScope(
            FieldScopes.allowingFieldDescriptors(listOf(getFieldDescriptor("o_int"))))
        .isEqualTo(eqExceptInt);
    assertThat(message)
        .withPartialScope(FieldScopes.ignoringFieldDescriptors(listOf(getFieldDescriptor("o_int"))))
        .isEqualTo(eqExceptInt);
  }
}
