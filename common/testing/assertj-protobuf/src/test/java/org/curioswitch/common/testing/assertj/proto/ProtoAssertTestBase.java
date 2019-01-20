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

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import com.google.protobuf.UnknownFieldSet;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Base class for testing {@link ProtoAssert} methods. */
public class ProtoAssertTestBase {
  // Type information for subclasses.
  static enum TestType {
    IMMUTABLE_PROTO2(TestMessage2.getDefaultInstance()),
    PROTO3(TestMessage3.getDefaultInstance());

    private final Message defaultInstance;

    TestType(Message defaultInstance) {
      this.defaultInstance = defaultInstance;
    }

    public Message defaultInstance() {
      return defaultInstance;
    }

    public boolean isProto3() {
      return this == PROTO3;
    }
  }

  private static final TextFormat.Parser PARSER =
      TextFormat.Parser.newBuilder()
          .setSingularOverwritePolicy(
              TextFormat.Parser.SingularOverwritePolicy.FORBID_SINGULAR_OVERWRITES)
          .build();

  // For Parameterized testing.
  protected static Collection<Object[]> parameters() {
    ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
    for (TestType testType : TestType.values()) {
      builder.add(new Object[] {testType});
    }
    return builder.build();
  }

  private final Message defaultInstance;
  private final boolean isProto3;

  protected ProtoAssertTestBase(TestType testType) {
    this.defaultInstance = testType.defaultInstance();
    this.isProto3 = testType.isProto3();
  }

  protected final Message fromUnknownFields(UnknownFieldSet unknownFieldSet)
      throws InvalidProtocolBufferException {
    return defaultInstance.getParserForType().parseFrom(unknownFieldSet.toByteArray());
  }

  protected final String fullMessageName() {
    return defaultInstance.getDescriptorForType().getFullName();
  }

  protected final FieldDescriptor getFieldDescriptor(String fieldName) {
    FieldDescriptor fieldDescriptor =
        defaultInstance.getDescriptorForType().findFieldByName(fieldName);
    checkArgument(fieldDescriptor != null, "No field named %s.", fieldName);
    return fieldDescriptor;
  }

  protected final int getFieldNumber(String fieldName) {
    return getFieldDescriptor(fieldName).getNumber();
  }

  protected Message parse(String textProto) {
    try {
      Message.Builder builder = defaultInstance.toBuilder();
      PARSER.merge(textProto, builder);
      return builder.build();
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  protected final Message parsePartial(String textProto) {
    try {
      Message.Builder builder = defaultInstance.toBuilder();
      PARSER.merge(textProto, builder);
      return builder.buildPartial();
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  protected final boolean isProto3() {
    return isProto3;
  }

  /**
   * Some tests don't vary across the different proto types, and should only be run once.
   *
   * <p>This method returns true for exactly one {@link TestType}, and false for all the others, and
   * so can be used to ensure tests are only run once.
   */
  protected final boolean testIsRunOnce() {
    return isProto3;
  }

  protected final <T extends Throwable> Consumer<T> isEqualToFailureMessage() {
    return expectFailureMatches(
        "Not true that messages compare equal\\.\\s*"
            + "(Differences were found:\\n.*|No differences were reported\\..*)");
  }

  protected final <T extends Throwable> Consumer<T> isNotEqualToFailureMessage() {
    return expectFailureMatches(
        "Not true that messages compare not equal\\.\\s*"
            + "(Only ignorable differences were found:\\n.*|"
            + "No differences were found\\..*)");
  }

  /**
   * Expects the current failure message to match the provided regex, using {@code Pattern.DOTALL}
   * to match newlines.
   */
  protected final <T extends Throwable> Consumer<T> expectFailureMatches(String regex) {
    return t -> assertThat(t.getMessage()).matches(Pattern.compile(regex, Pattern.DOTALL));
  }

  protected static final <T> ImmutableList<T> listOf(T... elements) {
    return ImmutableList.copyOf(elements);
  }

  protected static final <T> T[] arrayOf(T... elements) {
    return elements;
  }

  @SuppressWarnings("unchecked")
  protected static final <K, V> ImmutableMap<K, V> mapOf(K k0, V v0, Object... rest) {
    checkArgument(rest.length % 2 == 0, "Uneven args: %s", rest.length);

    ImmutableMap.Builder<K, V> builder = new ImmutableMap.Builder<>();
    builder.put(k0, v0);
    for (int i = 0; i < rest.length; i += 2) {
      builder.put((K) rest[i], (V) rest[i + 1]);
    }
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  protected static final <K, V> ImmutableMultimap<K, V> multimapOf(K k0, V v0, Object... rest) {
    checkArgument(rest.length % 2 == 0, "Uneven args: %s", rest.length);

    ImmutableMultimap.Builder<K, V> builder = new ImmutableMultimap.Builder<>();
    builder.put(k0, v0);
    for (int i = 0; i < rest.length; i += 2) {
      builder.put((K) rest[i], (V) rest[i + 1]);
    }
    return builder.build();
  }
}
