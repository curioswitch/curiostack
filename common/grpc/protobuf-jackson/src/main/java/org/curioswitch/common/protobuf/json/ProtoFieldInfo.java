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
package org.curioswitch.common.protobuf.json;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.CharMatcher;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/**
 * A wrapper of a {@link FieldDescriptor} to provide additional information like protobuf generated
 * code naming conventions and value type information.
 */
class ProtoFieldInfo {

  private static final CharMatcher DIGITS_ASCII = CharMatcher.inRange('0', '9');
  private static final CharMatcher LOWERCASE_ASCII = CharMatcher.inRange('a', 'z');
  private static final CharMatcher UPPERCASE_ASCII = CharMatcher.inRange('A', 'Z');

  private final FieldDescriptor field;
  private final Message containingPrototype;
  private final Class<? extends Message.Builder> builderClass;

  private final String camelCaseName;

  @Nullable private final ProtoFieldInfo mapKeyField;
  @Nullable private final ProtoFieldInfo mapValueField;

  ProtoFieldInfo(FieldDescriptor field, Message containingPrototype) {
    this.field = checkNotNull(field, "field");
    this.containingPrototype = checkNotNull(containingPrototype, "containingPrototype");
    builderClass = containingPrototype.newBuilderForType().getClass();

    camelCaseName = underscoresToUpperCamelCase(field.getName());

    if (field.isMapField()) {
      Descriptor mapType = field.getMessageType();
      mapKeyField = new ProtoFieldInfo(mapType.findFieldByName("key"), containingPrototype);
      mapValueField = new ProtoFieldInfo(mapType.findFieldByName("value"), containingPrototype);
    } else {
      mapKeyField = null;
      mapValueField = null;
    }
  }

  /** Returns the raw {@link FieldDescriptor} for this field. */
  FieldDescriptor descriptor() {
    return field;
  }

  /** Returns whether this is a map field. */
  @EnsuresNonNullIf(
      expression = {"mapKeyField", "mapValueField"},
      result = true)
  boolean isMapField() {
    return field.isMapField();
  }

  /**
   * Returns whether this is a repeated field. Note, map fields are also considered repeated fields.
   */
  boolean isRepeated() {
    return field.isRepeated();
  }

  /** Returns the {@link ProtoFieldInfo} of the key for this map field. */
  @Nullable
  ProtoFieldInfo mapKeyField() {
    checkState(isMapField(), "Not a map field: %s", field);
    return mapKeyField;
  }

  /**
   * Returns the {@link ProtoFieldInfo} describing the actual value of this field, which for map
   * fields is the map's value.
   */
  ProtoFieldInfo valueField() {
    return mapValueField != null ? mapValueField : this;
  }

  /**
   * Returns the {@link Type} of the actual value of this field, which for map fields is the type of
   * the map's value.
   */
  FieldDescriptor.Type valueType() {
    return valueField().descriptor().getType();
  }

  /**
   * Returns the {@link JavaType} of the actual value of this field, which for map fields is the
   * type of the map's value.
   */
  FieldDescriptor.JavaType valueJavaType() {
    return valueField().descriptor().getJavaType();
  }

  /**
   * Returns a prototype {@link Message} for the value of this field. For maps, it will be for the
   * value field of the map, otherwise it is for the field itself.
   */
  Message valuePrototype() {
    Message nestedPrototype =
        containingPrototype.newBuilderForType().newBuilderForField(field).buildPartial();
    if (isMapField()) {
      // newBuilderForField will give us the Message corresponding to the map with key and value,
      // but we want the marshaller for the value itself.
      nestedPrototype = (Message) nestedPrototype.getField(mapValueField.descriptor());
    }
    return nestedPrototype;
  }

  /**
   * Returns the method to get the value for the field within its message. The message must already
   * be on the execution stack. For map fields, this will be the method that returns a {@link
   * java.util.Map} and for repeated fields it will be the method that returns a {@link List}.
   */
  Method getValueMethod() {
    StringBuilder methodName = new StringBuilder().append("get").append(camelCaseName);
    if (valueJavaType() == JavaType.ENUM) {
      methodName.append("Value");
    }
    if (isMapField()) {
      methodName.append("Map");
    } else if (field.isRepeated()) {
      methodName.append("List");
    }
    try {
      return containingPrototype.getClass().getDeclaredMethod(methodName.toString());
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Could not find generated getter method.", e);
    }
  }

  /**
   * Returns the getter for the currently set value of this field's oneof. Must only be called for
   * oneof fields, which can be checked using {@link #isInOneof()}.
   */
  Method oneOfCaseMethod() {
    checkState(isInOneof(), "field is not in a oneof");
    String methodName =
        "get" + underscoresToUpperCamelCase(field.getContainingOneof().getName()) + "Case";
    try {
      return containingPrototype.getClass().getDeclaredMethod(methodName);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Could not find generated oneof case method.", e);
    }
  }

  /**
   * Returns the method to determine whether the message has a value for this field. Only valid for
   * message types for proto3 messages, valid for all fields otherwise.
   */
  Method hasValueMethod() {
    String methodName = "has" + camelCaseName;
    try {
      return containingPrototype.getClass().getDeclaredMethod(methodName);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Could not find generated has method.", e);
    }
  }

  /**
   * Returns the {@link Method} that returns the current count of a repeated field. Must only be
   * called for repeated fields, which can be checked with {@link #isRepeated()}.
   */
  Method repeatedValueCountMethod() {
    String methodName = "get" + camelCaseName + "Count";
    try {
      return containingPrototype.getClass().getDeclaredMethod(methodName);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Could not find repeated field count method.", e);
    }
  }

  /**
   * Returns the {@link Method} that sets a single value of the field. For repeated and map fields,
   * this is the add or put method that only take an individual element;
   */
  Method setValueMethod() {
    StringBuilder setter = new StringBuilder();
    final Class<?>[] args;
    if (isMapField()) {
      setter.append("put");
      args = new Class<?>[] {mapKeyField.javaClass(), javaClass()};
    } else {
      args = new Class<?>[] {javaClass()};
      if (field.isRepeated()) {
        setter.append("add");
      } else {
        setter.append("set");
      }
    }
    setter.append(camelCaseName);
    if (valueType() == Type.ENUM) {
      setter.append("Value");
    }
    try {
      return builderClass.getDeclaredMethod(setter.toString(), args);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Could not find setter.", e);
    }
  }

  /** Returns whether this field is in a oneof. */
  boolean isInOneof() {
    return field.getContainingOneof() != null;
  }

  /**
   * Returns the getter for the currently set value of this field's oneof. Must only be called for
   * oneof fields, which can be checked using {@link #isInOneof()}.
   */
  String getOneOfCaseMethodName() {
    checkState(isInOneof(), "field is not in a oneof");
    return "get" + underscoresToUpperCamelCase(field.getContainingOneof().getName()) + "Case";
  }

  /**
   * Determine the {@link Enum} class corresponding to this field's value. Because {@link Enum}
   * classes themselves are generated, we must introspect the prototype for determining the concrete
   * class. Due to type erasure, for repeated types we actually use protobuf reflection to add a
   * value to the container and retrieve it to determine the concrete type at runtime.
   */
  Class<?> enumClass() {
    Class<? extends Message> messageClass = containingPrototype.getClass();
    if (!field.isRepeated()) {
      return getEnumAsClassMethod().getReturnType();
    }
    if (isMapField()) {
      checkArgument(
          valueJavaType() == JavaType.ENUM,
          "Trying to determine enum class of non-enum type: %s",
          field);
      Message msgWithEnumValue =
          containingPrototype
              .newBuilderForType()
              .addRepeatedField(
                  field,
                  containingPrototype
                      .newBuilderForType()
                      .newBuilderForField(field)
                      .setField(
                          mapKeyField.descriptor(), mapKeyField.descriptor().getDefaultValue())
                      .setField(
                          mapValueField.descriptor(), mapValueField.descriptor().getDefaultValue())
                      .build())
              .build();
      try {
        return messageClass
            .getDeclaredMethod(
                getMapValueOrThrowMethodName(),
                new ProtoFieldInfo(mapKeyField.descriptor(), containingPrototype).javaClass())
            .invoke(msgWithEnumValue, mapKeyField.descriptor().getDefaultValue())
            .getClass();
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new IllegalStateException("Could not find or invoke map item getter.", e);
      }
    }

    // Repeated field.
    // Enums always have at least one value, so we can call getValues().get(0) without checking.
    Message msgWithEnumValue =
        containingPrototype
            .newBuilderForType()
            .addRepeatedField(
                valueField().descriptor(),
                valueField().descriptor().getEnumType().getValues().get(0))
            .build();
    try {
      return ((List<?>) getEnumAsClassMethod().invoke(msgWithEnumValue)).get(0).getClass();
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Could not invoke enum getter for determining type.", e);
    }
  }

  /**
   * Return the Java {@link Class} that corresponds to the value of this field. Generally used for
   * method resolution and casting generics.
   */
  Class<?> javaClass() {
    if (isMapField() && valueJavaType() == JavaType.MESSAGE) {
      Message mapEntry = containingPrototype.newBuilderForType().newBuilderForField(field).build();
      return mapEntry.getField(mapEntry.getDescriptorForType().findFieldByName("value")).getClass();
    }
    switch (valueJavaType()) {
      case INT:
        return int.class;
      case LONG:
        return long.class;
      case FLOAT:
        return float.class;
      case DOUBLE:
        return double.class;
      case BOOLEAN:
        return boolean.class;
      case STRING:
        return String.class;
      case BYTE_STRING:
        return ByteString.class;
      case ENUM:
        return int.class;
      case MESSAGE:
        return containingPrototype
            .newBuilderForType()
            .newBuilderForField(valueField().descriptor())
            .build()
            .getClass();
      default:
        throw new IllegalArgumentException("Unknown field type: " + valueJavaType());
    }
  }

  /**
   * Returns the name of the method that returns the value of a map field. Must only be called for
   * map fields, which can be checked using {@link #isMapField()}.
   */
  private String getMapValueOrThrowMethodName() {
    checkState(isMapField(), "field is not a map");
    return "get" + camelCaseName + "OrThrow";
  }

  /**
   * Returns the {@link Method} that returns the value for this enum field within the message. Used
   * for introspection of the concrete Java type of an enum.
   */
  private Method getEnumAsClassMethod() {
    String getter = "get" + camelCaseName;
    if (field.isMapField()) {
      getter += "Map";
    } else if (field.isRepeated()) {
      getter += "List";
    }
    try {
      return containingPrototype.getClass().getDeclaredMethod(getter);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Could not find getter for enum field.", e);
    }
  }

  // Guava's CaseFormat does not handle non-snake-case field names the same as protobuf compiler,
  // so we just directly port the compiler's code from here:
  // https://github.com/google/protobuf/blob/2f4489a3e504e0a4aaffee69b551c6acc9e08374/src/google/protobuf/compiler/cpp/cpp_helpers.cc#L108
  private static String underscoresToUpperCamelCase(String input) {
    boolean capitalizeNextLetter = true;
    StringBuilder result = new StringBuilder(input.length());
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (LOWERCASE_ASCII.matches(c)) {
        if (capitalizeNextLetter) {
          result.append((char) (c + ('A' - 'a')));
        } else {
          result.append(c);
        }
        capitalizeNextLetter = false;
      } else if (UPPERCASE_ASCII.matches(c)) {
        // Capital letters are left as-is.
        result.append(c);
        capitalizeNextLetter = false;
      } else if (DIGITS_ASCII.matches(c)) {
        result.append(c);
        capitalizeNextLetter = true;
      } else {
        capitalizeNextLetter = true;
      }
    }
    return result.toString();
  }
}
