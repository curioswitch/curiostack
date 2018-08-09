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

package org.curioswitch.common.testing.assertj.proto;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.assertj.core.api.AbstractAssert;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class ProtoAssert<ACTUAL extends Message> extends AbstractAssert<ProtoAssert<ACTUAL>, ACTUAL>
    implements ProtoFluentAssertion {

  public static <T extends Message> ProtoAssert<T> assertThat(T actual) {
    return new ProtoAssert<>(actual, FluentEqualityConfig.defaultInstance());
  }

  private final FluentEqualityConfig config;

  public ProtoAssert(ACTUAL actual, FluentEqualityConfig config) {
    super(actual, ProtoAssert.class);
    this.config = config;
  }

  @Override
  public ProtoFluentAssertion ignoringFieldAbsence() {
    return usingConfig(config.ignoringFieldAbsence());
  }

  @Override
  public ProtoFluentAssertion ignoringFieldAbsenceOfFields(int firstFieldNumber, int... rest) {
    return usingConfig(
        config.ignoringFieldAbsenceOfFields(FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public ProtoFluentAssertion ignoringFieldAbsenceOfFields(Iterable<Integer> fieldNumbers) {
    return usingConfig(config.ignoringFieldAbsenceOfFields(fieldNumbers));
  }

  @Override
  public ProtoFluentAssertion ignoringFieldAbsenceOfFieldDescriptors(
      FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return usingConfig(
        config.ignoringFieldAbsenceOfFieldDescriptors(Lists.asList(firstFieldDescriptor, rest)));
  }

  @Override
  public ProtoFluentAssertion ignoringFieldAbsenceOfFieldDescriptors(
      Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.ignoringFieldAbsenceOfFieldDescriptors(fieldDescriptors));
  }

  @Override
  public ProtoFluentAssertion ignoringRepeatedFieldOrder() {
    return usingConfig(config.ignoringRepeatedFieldOrder());
  }

  @Override
  public ProtoFluentAssertion ignoringRepeatedFieldOrderOfFields(
      int firstFieldNumber, int... rest) {
    return usingConfig(
        config.ignoringRepeatedFieldOrderOfFields(FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public ProtoFluentAssertion ignoringRepeatedFieldOrderOfFields(Iterable<Integer> fieldNumbers) {
    return usingConfig(config.ignoringRepeatedFieldOrderOfFields(fieldNumbers));
  }

  @Override
  public ProtoFluentAssertion ignoringRepeatedFieldOrderOfFieldDescriptors(
      FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return usingConfig(
        config.ignoringRepeatedFieldOrderOfFieldDescriptors(
            Lists.asList(firstFieldDescriptor, rest)));
  }

  @Override
  public ProtoFluentAssertion ignoringRepeatedFieldOrderOfFieldDescriptors(
      Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.ignoringRepeatedFieldOrderOfFieldDescriptors(fieldDescriptors));
  }

  @Override
  public ProtoFluentAssertion ignoringExtraRepeatedFieldElements() {
    return usingConfig(config.ignoringExtraRepeatedFieldElements());
  }

  @Override
  public ProtoFluentAssertion ignoringExtraRepeatedFieldElementsOfFields(
      int firstFieldNumber, int... rest) {
    return usingConfig(
        config.ignoringExtraRepeatedFieldElementsOfFields(
            FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public ProtoFluentAssertion ignoringExtraRepeatedFieldElementsOfFields(
      Iterable<Integer> fieldNumbers) {
    return usingConfig(config.ignoringExtraRepeatedFieldElementsOfFields(fieldNumbers));
  }

  @Override
  public ProtoFluentAssertion ignoringExtraRepeatedFieldElementsOfFieldDescriptors(
      FieldDescriptor first, FieldDescriptor... rest) {
    return usingConfig(
        config.ignoringExtraRepeatedFieldElementsOfFieldDescriptors(Lists.asList(first, rest)));
  }

  @Override
  public ProtoFluentAssertion ignoringExtraRepeatedFieldElementsOfFieldDescriptors(
      Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(
        config.ignoringExtraRepeatedFieldElementsOfFieldDescriptors(fieldDescriptors));
  }

  @Override
  public ProtoFluentAssertion usingDoubleTolerance(double tolerance) {
    return usingConfig(config.usingDoubleTolerance(tolerance));
  }

  @Override
  public ProtoFluentAssertion usingDoubleToleranceForFields(
      double tolerance, int firstFieldNumber, int... rest) {
    return usingConfig(
        config.usingDoubleToleranceForFields(
            tolerance, FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public ProtoFluentAssertion usingDoubleToleranceForFields(
      double tolerance, Iterable<Integer> fieldNumbers) {
    return usingConfig(config.usingDoubleToleranceForFields(tolerance, fieldNumbers));
  }

  @Override
  public ProtoFluentAssertion usingDoubleToleranceForFieldDescriptors(
      double tolerance, FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return usingConfig(
        config.usingDoubleToleranceForFieldDescriptors(
            tolerance, Lists.asList(firstFieldDescriptor, rest)));
  }

  @Override
  public ProtoFluentAssertion usingDoubleToleranceForFieldDescriptors(
      double tolerance, Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.usingDoubleToleranceForFieldDescriptors(tolerance, fieldDescriptors));
  }

  @Override
  public ProtoFluentAssertion usingFloatTolerance(float tolerance) {
    return usingConfig(config.usingFloatTolerance(tolerance));
  }

  @Override
  public ProtoFluentAssertion usingFloatToleranceForFields(
      float tolerance, int firstFieldNumber, int... rest) {
    return usingConfig(
        config.usingFloatToleranceForFields(
            tolerance, FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public ProtoFluentAssertion usingFloatToleranceForFields(
      float tolerance, Iterable<Integer> fieldNumbers) {
    return usingConfig(config.usingFloatToleranceForFields(tolerance, fieldNumbers));
  }

  @Override
  public ProtoFluentAssertion usingFloatToleranceForFieldDescriptors(
      float tolerance, FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return usingConfig(
        config.usingFloatToleranceForFieldDescriptors(
            tolerance, Lists.asList(firstFieldDescriptor, rest)));
  }

  @Override
  public ProtoFluentAssertion usingFloatToleranceForFieldDescriptors(
      float tolerance, Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.usingFloatToleranceForFieldDescriptors(tolerance, fieldDescriptors));
  }

  @Override
  public ProtoFluentAssertion comparingExpectedFieldsOnly() {
    return usingConfig(config.comparingExpectedFieldsOnly());
  }

  @Override
  public ProtoFluentAssertion withPartialScope(FieldScope fieldScope) {
    return usingConfig(config.withPartialScope(checkNotNull(fieldScope, "fieldScope")));
  }

  @Override
  public ProtoFluentAssertion ignoringFields(int firstFieldNumber, int... rest) {
    return ignoringFields(FieldScopeUtil.asList(firstFieldNumber, rest));
  }

  @Override
  public ProtoFluentAssertion ignoringFields(Iterable<Integer> fieldNumbers) {
    return usingConfig(config.ignoringFields(fieldNumbers));
  }

  @Override
  public ProtoFluentAssertion ignoringFieldDescriptors(
      FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return ignoringFieldDescriptors(Lists.asList(firstFieldDescriptor, rest));
  }

  @Override
  public ProtoFluentAssertion ignoringFieldDescriptors(Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.ignoringFieldDescriptors(fieldDescriptors));
  }

  @Override
  public ProtoFluentAssertion ignoringFieldScope(FieldScope fieldScope) {
    return usingConfig(config.ignoringFieldScope(checkNotNull(fieldScope, "fieldScope")));
  }

  @Override
  public ProtoFluentAssertion reportingMismatchesOnly() {
    return usingConfig(config.reportingMismatchesOnly());
  }

  @Override
  public ProtoAssert<ACTUAL> isEqualTo(@NullableDecl Object expected) {
    if (notMessagesWithSameDescriptor(actual, expected)) {
      return super.isEqualTo(expected);
    } else {
      DiffResult diffResult =
          makeDifferencer((Message) expected).diffMessages(actual, (Message) expected);
      if (!diffResult.isMatched()) {
        failWithMessage(
            failureMessage(true) + "\n" + diffResult.printToString(config.reportMismatchesOnly()));
      }
    }

    return this;
  }

  @Override
  public ProtoAssert<ACTUAL> isNotEqualTo(@NullableDecl Object expected) {
    if (notMessagesWithSameDescriptor(actual, expected)) {
      return super.isNotEqualTo(expected);
    } else {
      DiffResult diffResult =
          makeDifferencer((Message) expected).diffMessages(actual, (Message) expected);
      if (diffResult.isMatched()) {
        failWithMessage(
            failureMessage(false) + "\n" + diffResult.printToString(config.reportMismatchesOnly()));
      }
    }

    return this;
  }

  /** Checks whether the subject is a {@link Message} with no fields set. */
  public ProtoAssert<ACTUAL> isEqualToDefaultInstance() {
    if (actual == null) {
      failWithMessage("Not true that <%s> is a default proto instance. It is null.", actual);
    } else if (!actual.equals(actual.getDefaultInstanceForType())) {
      failWithMessage("Not true that <%s> is a default proto instance. It has set values.", actual);
    }

    return this;
  }

  /** ProtoAssert<ACTUAL> whether the subject is not equivalent to a {@link Message} with no fields set. */
  public ProtoAssert<ACTUAL> isNotEqualToDefaultInstance() {
    if (actual != null && actual.equals(actual.getDefaultInstanceForType())) {
      failWithMessage(
          "Not true that (%s) %s is not a default proto instance. It has no set values.",
          actual.getClass().getName(), actual);
    }

    return this;
  }

  public ProtoAssert<ACTUAL> hasAllRequiredFields() {
    if (!actual.isInitialized()) {
      failWithMessage(
          "Not true that <%s> has all required fields set. Missing: %s",
          actual, actual.findInitializationErrors());
    }

    return this;
  }

  private ProtoAssert<ACTUAL> usingConfig(FluentEqualityConfig newConfig) {
    ProtoAssert<ACTUAL> newAssert = new ProtoAssert<>(actual, newConfig);
    if (info.hasDescription()) {
      newAssert.info.description(info.description());
    }
    return newAssert;
  }

  private static boolean notMessagesWithSameDescriptor(
      @NullableDecl Message actual, @NullableDecl Object expected) {
    if (actual != null && expected instanceof Message) {
      return actual.getDescriptorForType() != ((Message) expected).getDescriptorForType();
    }
    return true;
  }

  private ProtoTruthMessageDifferencer makeDifferencer(Message expected) {
    return config
        .withExpectedMessages(ImmutableList.of(expected))
        .toMessageDifferencer(actual.getDescriptorForType());
  }

  private static String failureMessage(boolean expectedEqual) {
    StringBuilder rawMessage =
        new StringBuilder()
            .append("Not true that ")
            .append("messages compare ")
            .append(expectedEqual ? "" : "not ")
            .append("equal. ");
    return rawMessage.toString();
  }
}
