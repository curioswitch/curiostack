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

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors.Descriptor;
import java.lang.reflect.Method;
import java.util.Map;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation.Context;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;

/**
 * Utilities for code generation of protobufs, to simplify code or to unify logic between the byte
 * buddy builder and the generated instrumentation.
 */
final class CodeGenUtil {

  /**
   * Returns the name of the java field storing a {@link TypeSpecificMarshaller} for the given
   * descriptor.
   */
  static String fieldNameForNestedMarshaller(Descriptor descriptor) {
    return "MARSHALLER_" + descriptor.getFullName().replace('.', '_');
  }

  /**
   * Returns the name of the java field storing the pre-serialized version of the protobuf field
   * name.
   */
  static String fieldNameForSerializedFieldName(ProtoFieldInfo field) {
    return "FIELD_NAME_" + field.descriptor().getNumber();
  }

  /** Returns a {@link StackManipulation} that invokes the given {@link Method}. */
  static StackManipulation invoke(Method method) {
    return MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(method));
  }

  /** Returns a {@link Map} of names to class / instance fields. */
  static Map<String, FieldDescription> fieldsByName(Context implementationContext) {
    ImmutableMap.Builder<String, FieldDescription> map = ImmutableMap.builder();
    for (FieldDescription field : implementationContext.getInstrumentedType().getDeclaredFields()) {
      map.put(field.getName(), field);
    }
    return map.build();
  }

  /**
   * Returns a {@link StackManipulation} that returns the {@link
   * com.google.protobuf.Descriptors.EnumDescriptor} for the given enum field.
   */
  static StackManipulation getEnumDescriptor(ProtoFieldInfo info) {
    Class<?> clz = info.enumClass();
    final MethodDescription.ForLoadedMethod getDescriptor;
    try {
      getDescriptor = new MethodDescription.ForLoadedMethod(clz.getDeclaredMethod("getDescriptor"));
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Not an enum class: " + clz, e);
    }
    return MethodInvocation.invoke(getDescriptor);
  }

  private CodeGenUtil() {}
}
