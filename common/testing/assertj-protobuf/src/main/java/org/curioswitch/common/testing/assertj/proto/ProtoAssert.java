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

public class ProtoAssert<ACTUAL extends Message, SELF extends ProtoAssert<ACTUAL, SELF>> extends AbstractAssert<SELF, ACTUAL>
    implements ProtoFluentAssertion<SELF> {

  public static <T extends Message> ProtoAssert<T, ?> assertThat(T actual) {
    return new ProtoAssert<>(actual, FluentEqualityConfig.defaultInstance());
  }

  private final FluentEqualityConfig config;

  protected ProtoAssert(ACTUAL actual, FluentEqualityConfig config) {
    super(actual, ProtoAssert.class);
    this.config = config;
  }

  @Override
  public SELF ignoringFieldAbsence() {
    return usingConfig(config.ignoringFieldAbsence());
  }

  @Override
  public SELF ignoringFieldAbsenceOfFields(int firstFieldNumber, int... rest) {
    return usingConfig(
        config.ignoringFieldAbsenceOfFields(FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public SELF ignoringFieldAbsenceOfFields(Iterable<Integer> fieldNumbers) {
    return usingConfig(config.ignoringFieldAbsenceOfFields(fieldNumbers));
  }

  @Override
  public SELF ignoringFieldAbsenceOfFieldDescriptors(
      FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return usingConfig(
        config.ignoringFieldAbsenceOfFieldDescriptors(Lists.asList(firstFieldDescriptor, rest)));
  }

  @Override
  public SELF ignoringFieldAbsenceOfFieldDescriptors(
      Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.ignoringFieldAbsenceOfFieldDescriptors(fieldDescriptors));
  }

  @Override
  public SELF ignoringRepeatedFieldOrder() {
    return usingConfig(config.ignoringRepeatedFieldOrder());
  }

  @Override
  public SELF ignoringRepeatedFieldOrderOfFields(int firstFieldNumber, int... rest) {
    return usingConfig(
        config.ignoringRepeatedFieldOrderOfFields(FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public SELF ignoringRepeatedFieldOrderOfFields(Iterable<Integer> fieldNumbers) {
    return usingConfig(config.ignoringRepeatedFieldOrderOfFields(fieldNumbers));
  }

  @Override
  public SELF ignoringRepeatedFieldOrderOfFieldDescriptors(
      FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return usingConfig(
        config.ignoringRepeatedFieldOrderOfFieldDescriptors(
            Lists.asList(firstFieldDescriptor, rest)));
  }

  @Override
  public SELF ignoringRepeatedFieldOrderOfFieldDescriptors(
      Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.ignoringRepeatedFieldOrderOfFieldDescriptors(fieldDescriptors));
  }

  @Override
  public SELF ignoringExtraRepeatedFieldElements() {
    return usingConfig(config.ignoringExtraRepeatedFieldElements());
  }

  @Override
  public SELF ignoringExtraRepeatedFieldElementsOfFields(
      int firstFieldNumber, int... rest) {
    return usingConfig(
        config.ignoringExtraRepeatedFieldElementsOfFields(
            FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public SELF ignoringExtraRepeatedFieldElementsOfFields(
      Iterable<Integer> fieldNumbers) {
    return usingConfig(config.ignoringExtraRepeatedFieldElementsOfFields(fieldNumbers));
  }

  @Override
  public SELF ignoringExtraRepeatedFieldElementsOfFieldDescriptors(
      FieldDescriptor first, FieldDescriptor... rest) {
    return usingConfig(
        config.ignoringExtraRepeatedFieldElementsOfFieldDescriptors(Lists.asList(first, rest)));
  }

  @Override
  public SELF ignoringExtraRepeatedFieldElementsOfFieldDescriptors(
      Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(
        config.ignoringExtraRepeatedFieldElementsOfFieldDescriptors(fieldDescriptors));
  }

  @Override
  public SELF usingDoubleTolerance(double tolerance) {
    return usingConfig(config.usingDoubleTolerance(tolerance));
  }

  @Override
  public SELF usingDoubleToleranceForFields(
      double tolerance, int firstFieldNumber, int... rest) {
    return usingConfig(
        config.usingDoubleToleranceForFields(
            tolerance, FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public SELF usingDoubleToleranceForFields(
      double tolerance, Iterable<Integer> fieldNumbers) {
    return usingConfig(config.usingDoubleToleranceForFields(tolerance, fieldNumbers));
  }

  @Override
  public SELF usingDoubleToleranceForFieldDescriptors(
      double tolerance, FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return usingConfig(
        config.usingDoubleToleranceForFieldDescriptors(
            tolerance, Lists.asList(firstFieldDescriptor, rest)));
  }

  @Override
  public SELF usingDoubleToleranceForFieldDescriptors(
      double tolerance, Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.usingDoubleToleranceForFieldDescriptors(tolerance, fieldDescriptors));
  }

  @Override
  public SELF usingFloatTolerance(float tolerance) {
    return usingConfig(config.usingFloatTolerance(tolerance));
  }

  @Override
  public SELF usingFloatToleranceForFields(
      float tolerance, int firstFieldNumber, int... rest) {
    return usingConfig(
        config.usingFloatToleranceForFields(
            tolerance, FieldScopeUtil.asList(firstFieldNumber, rest)));
  }

  @Override
  public SELF usingFloatToleranceForFields(
      float tolerance, Iterable<Integer> fieldNumbers) {
    return usingConfig(config.usingFloatToleranceForFields(tolerance, fieldNumbers));
  }

  @Override
  public SELF usingFloatToleranceForFieldDescriptors(
      float tolerance, FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return usingConfig(
        config.usingFloatToleranceForFieldDescriptors(
            tolerance, Lists.asList(firstFieldDescriptor, rest)));
  }

  @Override
  public SELF usingFloatToleranceForFieldDescriptors(
      float tolerance, Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.usingFloatToleranceForFieldDescriptors(tolerance, fieldDescriptors));
  }

  @Override
  public SELF comparingExpectedFieldsOnly() {
    return usingConfig(config.comparingExpectedFieldsOnly());
  }

  @Override
  public SELF withPartialScope(FieldScope fieldScope) {
    return usingConfig(config.withPartialScope(checkNotNull(fieldScope, "fieldScope")));
  }

  @Override
  public SELF ignoringFields(int firstFieldNumber, int... rest) {
    return ignoringFields(FieldScopeUtil.asList(firstFieldNumber, rest));
  }

  @Override
  public SELF ignoringFields(Iterable<Integer> fieldNumbers) {
    return usingConfig(config.ignoringFields(fieldNumbers));
  }

  @Override
  public SELF ignoringFieldDescriptors(
      FieldDescriptor firstFieldDescriptor, FieldDescriptor... rest) {
    return ignoringFieldDescriptors(Lists.asList(firstFieldDescriptor, rest));
  }

  @Override
  public SELF ignoringFieldDescriptors(Iterable<FieldDescriptor> fieldDescriptors) {
    return usingConfig(config.ignoringFieldDescriptors(fieldDescriptors));
  }

  @Override
  public SELF ignoringFieldScope(FieldScope fieldScope) {
    return usingConfig(config.ignoringFieldScope(checkNotNull(fieldScope, "fieldScope")));
  }

  @Override
  public SELF reportingMismatchesOnly() {
    return usingConfig(config.reportingMismatchesOnly());
  }

  @Override
  public SELF isEqualTo(@NullableDecl Object expected) {
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

    return self();
  }

  @Override
  public SELF isNotEqualTo(@NullableDecl Object expected) {
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

    return self();
  }

  /** Checks whether the subject is a {@link Message} with no fields set. */
  public SELF isEqualToDefaultInstance() {
    if (actual == null) {
      failWithMessage("Not true that <%s> is a default proto instance. It is null.", actual);
    } else if (!actual.equals(actual.getDefaultInstanceForType())) {
      failWithMessage("Not true that <%s> is a default proto instance. It has set values.", actual);
    }

    return self();
  }

  /**
   * ProtoAssert<ACTUAL> whether the subject is not equivalent to a {@link Message} with no fields
   * set.
   */
  public SELF isNotEqualToDefaultInstance() {
    if (actual != null && actual.equals(actual.getDefaultInstanceForType())) {
      failWithMessage(
          "Not true that (%s) %s is not a default proto instance. It has no set values.",
          actual.getClass().getName(), actual);
    }

    return self();
  }

  public SELF hasAllRequiredFields() {
    if (!actual.isInitialized()) {
      failWithMessage(
          "Not true that <%s> has all required fields set. Missing: %s",
          actual, actual.findInitializationErrors());
    }

    return self();
  }

  protected SELF usingConfig(FluentEqualityConfig newConfig) {
    ProtoAssert<ACTUAL, ?> newAssert = new ProtoAssert<>(actual, newConfig);
    if (info.hasDescription()) {
      newAssert.info.description(info.description());
    }

    @SuppressWarnings("unchecked")
    SELF s = (SELF) newAssert;
    return s;
  }

  private SELF self() {
    @SuppressWarnings("unchecked")
    SELF self = (SELF) this;
    return self;
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
