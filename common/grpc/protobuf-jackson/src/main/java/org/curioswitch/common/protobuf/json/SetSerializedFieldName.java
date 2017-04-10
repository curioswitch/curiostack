/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

import java.util.Map;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.jar.asm.MethodVisitor;

/**
 * Sets the static field with the given name to a SerializedString containing the protobuf field's
 * name, for efficient serializing, e.g.,
 *
 * <pre>{@code
 * private static final FIELD_NAME_1 = SerializeSupport.serializeString("fieldFoo");
 * private static final FIELD_NAME_3 = SerializeSupport.serializeString("fieldBar");
 *
 * }</pre>
 */
class SetSerializedFieldName implements ByteCodeAppender, Implementation {

  private static final StackManipulation SerializeSupport_serializeString;

  static {
    try {
      SerializeSupport_serializeString =
          CodeGenUtil.invoke(
              SerializeSupport.class.getDeclaredMethod("serializeString", String.class));
    } catch (NoSuchMethodException e) {
      throw new Error(e);
    }
  }

  private final String fieldName;
  private final String unserializedFieldValue;

  SetSerializedFieldName(String fieldName, String unserializedFieldValue) {
    this.fieldName = fieldName;
    this.unserializedFieldValue = unserializedFieldValue;
  }

  @Override
  public Size apply(
      MethodVisitor methodVisitor,
      Context implementationContext,
      MethodDescription instrumentedMethod) {
    Map<String, FieldDescription> fieldsByName = CodeGenUtil.fieldsByName(implementationContext);
    StackManipulation.Size operandStackSize =
        new StackManipulation.Compound(
                new TextConstant(unserializedFieldValue),
                SerializeSupport_serializeString,
                FieldAccess.forField(fieldsByName.get(fieldName)).write())
            .apply(methodVisitor, implementationContext);
    return new Size(operandStackSize.getMaximalSize(), instrumentedMethod.getStackSize());
  }

  @Override
  public ByteCodeAppender appender(Target implementationTarget) {
    return this;
  }

  @Override
  public InstrumentedType prepare(InstrumentedType instrumentedType) {
    return instrumentedType;
  }
}
