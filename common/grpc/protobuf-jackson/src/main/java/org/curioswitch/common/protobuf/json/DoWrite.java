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

import static org.curioswitch.common.protobuf.json.CodeGenUtil.invoke;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription.ForLoadedType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackManipulation.Trivial;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveUnboxingDelegate;
import net.bytebuddy.implementation.bytecode.constant.DoubleConstant;
import net.bytebuddy.implementation.bytecode.constant.FloatConstant;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.LongConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import org.curioswitch.common.protobuf.json.LocalVariables.VariableHandle;
import org.curioswitch.common.protobuf.json.bytebuddy.Goto;
import org.curioswitch.common.protobuf.json.bytebuddy.IfEqual;
import org.curioswitch.common.protobuf.json.bytebuddy.IfFalse;
import org.curioswitch.common.protobuf.json.bytebuddy.IfIntsNotEqual;
import org.curioswitch.common.protobuf.json.bytebuddy.SetJumpTargetLabel;

/**
 * {@link ByteCodeAppender} to generate code for serializing a specific {@link Message} type. For
 * all fields, checks for field presence and writes it appropriately.
 *
 * <p>Generated code looks something like:
 *
 * <pre>{@code
 * if (message.getFieldOne() != 0) {
 *   SerializeSupport.printSignedInt32(message.getFieldOne(), gen);
 * }
 * if (message.getFieldTwoCount() != 0){
 *   SerializeSupport.printRepeatedString(message.getFieldTwoList(), gen);
 * }
 * if (message.getFieldThreeCount() != 0) {
 *   for (Map.Entry&lt;Integer, String&gt; entry : message.getFieldThreeMap()) {
 *     gen.writeFieldName(Integer.toString(SerializesSupport.normalizeInt32(entry.getKey()));
 *     SerializeSupport.printString(entry.getValue());
 *   }
 * }
 *
 * }</pre>
 */
final class DoWrite implements ByteCodeAppender, Implementation {

  private enum LocalVariable implements VariableHandle {
    message,
    gen,
    iterator,
    entry
  }

  private static final Converter<String, String> TO_CAMEL_CASE =
      CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL);

  private static final StackManipulation JsonGenerator_writeFieldName_SerializableString;
  private static final StackManipulation JsonGenerator_writeFieldName_String;
  private static final StackManipulation JsonGenerator_writeStartObject;
  private static final StackManipulation JsonGenerator_writeEndObject;

  private static final StackManipulation Object_equals;

  private static final StackManipulation EnumLite_getNumber;

  private static final StackManipulation Map_entrySet;
  private static final StackManipulation Set_iterator;
  private static final StackManipulation Iterator_hasNext;
  private static final StackManipulation Iterator_next;
  private static final StackManipulation Map_Entry_getKey;
  private static final StackManipulation Map_Entry_getValue;

  private static final StackManipulation Integer_toString;
  private static final StackManipulation Long_toString;
  private static final StackManipulation Boolean_toString;

  private static final StackManipulation SerializeSupport_printRepeatedSignedInt32;
  private static final StackManipulation SerializeSupport_printSignedInt32;
  private static final StackManipulation SerializeSupport_printRepeatedSignedInt64;
  private static final StackManipulation SerializeSupport_printSignedInt64;
  private static final StackManipulation SerializeSupport_printRepeatedBool;
  private static final StackManipulation SerializeSupport_printBool;
  private static final StackManipulation SerializeSupport_printRepeatedFloat;
  private static final StackManipulation SerializeSupport_printFloat;
  private static final StackManipulation SerializeSupport_printRepeatedDouble;
  private static final StackManipulation SerializeSupport_printDouble;
  private static final StackManipulation SerializeSupport_printRepeatedUnsignedInt32;
  private static final StackManipulation SerializeSupport_printUnsignedInt32;
  private static final StackManipulation SerializeSupport_printRepeatedUnsignedInt64;
  private static final StackManipulation SerializeSupport_printUnsignedInt64;
  private static final StackManipulation SerializeSupport_printRepeatedString;
  private static final StackManipulation SerializeSupport_printString;
  private static final StackManipulation SerializeSupport_printRepeatedBytes;
  private static final StackManipulation SerializeSupport_printBytes;
  private static final StackManipulation SerializeSupport_printRepeatedNull;
  private static final StackManipulation SerializeSupport_printNull;
  private static final StackManipulation SerializeSupport_printRepeatedEnum;
  private static final StackManipulation SerializeSupport_printEnum;
  private static final StackManipulation SerializeSupport_printRepeatedMessage;
  private static final StackManipulation SerializeSupport_printMessage;

  private static final StackManipulation SerializeSupport_normalizeUnsignedInt32;
  private static final StackManipulation SerializeSupport_normalizeUnsignedInt64;

  static {
    try {
      JsonGenerator_writeFieldName_SerializableString =
          invoke(JsonGenerator.class.getDeclaredMethod("writeFieldName", SerializableString.class));
      JsonGenerator_writeFieldName_String =
          invoke(JsonGenerator.class.getDeclaredMethod("writeFieldName", String.class));
      JsonGenerator_writeStartObject =
          invoke(JsonGenerator.class.getDeclaredMethod("writeStartObject"));
      JsonGenerator_writeEndObject =
          invoke(JsonGenerator.class.getDeclaredMethod("writeEndObject"));

      Object_equals = invoke(Object.class.getDeclaredMethod("equals", Object.class));

      EnumLite_getNumber = invoke(EnumLite.class.getDeclaredMethod("getNumber"));

      Integer_toString = invoke(Integer.class.getDeclaredMethod("toString", int.class));
      Long_toString = invoke(Long.class.getDeclaredMethod("toString", long.class));
      Boolean_toString = invoke(Boolean.class.getDeclaredMethod("toString", boolean.class));

      Map_entrySet = invoke(Map.class.getDeclaredMethod("entrySet"));
      Set_iterator = invoke(Set.class.getDeclaredMethod("iterator"));
      Iterator_hasNext = invoke(Iterator.class.getDeclaredMethod("hasNext"));
      Iterator_next = invoke(Iterator.class.getDeclaredMethod("next"));
      Map_Entry_getKey = invoke(Entry.class.getDeclaredMethod("getKey"));
      Map_Entry_getValue = invoke(Entry.class.getDeclaredMethod("getValue"));

      SerializeSupport_printRepeatedSignedInt32 =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedSignedInt32", List.class, JsonGenerator.class));
      SerializeSupport_printSignedInt32 =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printSignedInt32", int.class, JsonGenerator.class));
      SerializeSupport_printRepeatedSignedInt64 =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedSignedInt64", List.class, JsonGenerator.class));
      SerializeSupport_printSignedInt64 =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printSignedInt64", long.class, JsonGenerator.class));
      SerializeSupport_printRepeatedBool =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedBool", List.class, JsonGenerator.class));
      SerializeSupport_printBool =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printBool", boolean.class, JsonGenerator.class));
      SerializeSupport_printRepeatedFloat =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedFloat", List.class, JsonGenerator.class));
      SerializeSupport_printFloat =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printFloat", float.class, JsonGenerator.class));
      SerializeSupport_printRepeatedDouble =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedDouble", List.class, JsonGenerator.class));
      SerializeSupport_printDouble =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printDouble", double.class, JsonGenerator.class));
      SerializeSupport_printRepeatedUnsignedInt32 =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedUnsignedInt32", List.class, JsonGenerator.class));
      SerializeSupport_printUnsignedInt32 =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printUnsignedInt32", int.class, JsonGenerator.class));
      SerializeSupport_printRepeatedUnsignedInt64 =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedUnsignedInt64", List.class, JsonGenerator.class));
      SerializeSupport_printUnsignedInt64 =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printUnsignedInt64", long.class, JsonGenerator.class));
      SerializeSupport_printRepeatedString =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedString", List.class, JsonGenerator.class));
      SerializeSupport_printString =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printString", String.class, JsonGenerator.class));
      SerializeSupport_printRepeatedBytes =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedBytes", List.class, JsonGenerator.class));
      SerializeSupport_printBytes =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printBytes", ByteString.class, JsonGenerator.class));
      SerializeSupport_printRepeatedNull =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedNull", List.class, JsonGenerator.class));
      SerializeSupport_printNull =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printNull", int.class, JsonGenerator.class));
      SerializeSupport_printRepeatedEnum =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedEnum", List.class, JsonGenerator.class, EnumDescriptor.class));
      SerializeSupport_printEnum =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printEnum", int.class, JsonGenerator.class, EnumDescriptor.class));
      SerializeSupport_printRepeatedMessage =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printRepeatedMessage",
                  List.class,
                  JsonGenerator.class,
                  TypeSpecificMarshaller.class));
      SerializeSupport_printMessage =
          invoke(
              SerializeSupport.class.getDeclaredMethod(
                  "printMessage",
                  Message.class,
                  JsonGenerator.class,
                  TypeSpecificMarshaller.class));

      SerializeSupport_normalizeUnsignedInt32 =
          invoke(SerializeSupport.class.getDeclaredMethod("normalizeUnsignedInt32", int.class));
      SerializeSupport_normalizeUnsignedInt64 =
          invoke(SerializeSupport.class.getDeclaredMethod("normalizeUnsignedInt64", long.class));
    } catch (NoSuchMethodException e) {
      throw new Error(e);
    }
  }

  private final Message prototype;
  private final Class<? extends Message> messageClass;
  private final Descriptor descriptor;
  private final boolean includeDefaults;

  DoWrite(Message prototype, boolean includeDefaults) {
    this.prototype = prototype;
    this.messageClass = prototype.getClass();
    this.descriptor = prototype.getDescriptorForType();
    this.includeDefaults = includeDefaults;
  }

  @Override
  public Size apply(
      MethodVisitor methodVisitor,
      Context implementationContext,
      MethodDescription instrumentedMethod) {
    Map<String, FieldDescription> fieldsByName = CodeGenUtil.fieldsByName(implementationContext);

    List<StackManipulation> stackManipulations = new ArrayList<>();

    final StackManipulation getDefaultInstance;
    try {
      getDefaultInstance = invoke(messageClass.getDeclaredMethod("getDefaultInstance"));
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Could not find getDefaultInstance on a Message class.");
    }

    LocalVariables<LocalVariable> locals =
        LocalVariables.builderForMethod(instrumentedMethod, LocalVariable.values())
            .add(Iterator.class, LocalVariable.iterator)
            .add(Map.Entry.class, LocalVariable.entry)
            .build();

    stackManipulations.add(locals.initialize());

    // We output serialization code for each field, with an accompanying presence-check if-statement
    // based on the includeDefaults parameter.
    for (FieldDescriptor f : descriptor.getFields()) {
      ProtoFieldInfo field = new ProtoFieldInfo(f, prototype);

      StackManipulation getValue =
          new StackManipulation.Compound(
              locals.load(LocalVariable.message), invoke(field.getValueMethod()));

      Label afterSerializeField = new Label();

      // If includeDefaults is false, or for repeated fields, we check whether the value is default
      // and skip otherwise.
      //
      // e.g.,
      // if (message.getFoo() == field.getDefaultValue()) {
      //   ...
      // }
      // if (message.getBarCount() != 0) {
      //   ...
      // }
      if (!includeDefaults
          // Only print one-of fields if they're actually set (the default of a one-of is an empty
          // one-of).
          || field.isInOneof()
          // Always skip empty optional message fields. If not we will recurse indefinitely if
          // a message has itself as a sub-field.
          || (field.descriptor().isOptional() && field.valueJavaType() == JavaType.MESSAGE)) {
        stackManipulations.add(
            checkDefaultValue(field, locals, getValue, getDefaultInstance, afterSerializeField));
      }
      stackManipulations.addAll(
          Arrays.asList(
              locals.load(LocalVariable.gen),
              FieldAccess.forField(
                      fieldsByName.get(CodeGenUtil.fieldNameForSerializedFieldName(field)))
                  .read(),
              JsonGenerator_writeFieldName_SerializableString));

      // Serializes a map field by iterating over entries, encoding the key and value as
      // appropriate. Map keys are always strings. There are too many combinations of field to value
      // for maps, so we generate the code to iterate over them, unlike normal repeated fields.
      //
      // e.g.,
      // gen.writeStartObject();
      // for (Map.Entry<K, V> entry : message.getFooMap().entrySet()) {
      //    String key = keyToString(entry.getKey());
      //    gen.writeFieldName(key);
      //    SerializeSupport.printFoo(gen, entry.getValue());
      // }
      // gen.writeEndObject();
      if (field.isMapField()) {
        final StackManipulation keyToString;
        switch (field.mapKeyField().descriptor().getType()) {
          case INT32:
          case SINT32:
          case SFIXED32:
            keyToString = Integer_toString;
            break;
          case INT64:
          case SINT64:
          case SFIXED64:
            keyToString = Long_toString;
            break;
          case BOOL:
            keyToString = Boolean_toString;
            break;
          case UINT32:
          case FIXED32:
            keyToString =
                new StackManipulation.Compound(
                    SerializeSupport_normalizeUnsignedInt32, Long_toString);
            break;
          case UINT64:
          case FIXED64:
            keyToString = SerializeSupport_normalizeUnsignedInt64;
            break;
          case STRING:
            keyToString = Trivial.INSTANCE;
            break;
          default:
            throw new IllegalStateException(
                "Unexpected map key type: " + field.mapKeyField().descriptor().getType());
        }
        final Label loopStart = new Label();
        final Label loopEnd = new Label();
        final StackManipulation printValue = printValue(fieldsByName, field);
        final StackManipulation printMapFieldValue =
            new StackManipulation.Compound(
                locals.load(LocalVariable.gen),
                JsonGenerator_writeStartObject,
                getValue,
                Map_entrySet,
                Set_iterator,
                locals.store(LocalVariable.iterator),
                new SetJumpTargetLabel(loopStart),
                locals.load(LocalVariable.iterator),
                Iterator_hasNext,
                new IfFalse(loopEnd),
                locals.load(LocalVariable.iterator),
                Iterator_next,
                TypeCasting.to(new ForLoadedType(Entry.class)),
                locals.store(LocalVariable.entry),
                locals.load(LocalVariable.gen),
                locals.load(LocalVariable.entry),
                Map_Entry_getKey,
                unbox(field.mapKeyField()),
                keyToString,
                JsonGenerator_writeFieldName_String,
                locals.load(LocalVariable.entry),
                Map_Entry_getValue,
                unbox(field.valueField()),
                locals.load(LocalVariable.gen),
                printValue,
                new Goto(loopStart),
                new SetJumpTargetLabel(loopEnd),
                locals.load(LocalVariable.gen),
                JsonGenerator_writeEndObject);
        stackManipulations.add(printMapFieldValue);
      } else {
        // Simply calls the SerializeSupport method that prints out this field. Any iteration will
        // be handled there.
        //
        // e.g.,
        // SerializeSupport.printUnsignedInt32(message.getFoo());
        // SerializeSupport.printRepeatedString(message.getBar());
        final StackManipulation printValue = printValue(fieldsByName, field);
        stackManipulations.addAll(
            Arrays.asList(getValue, locals.load(LocalVariable.gen), printValue));
      }
      stackManipulations.add(new SetJumpTargetLabel(afterSerializeField));
    }
    stackManipulations.add(MethodReturn.VOID);
    StackManipulation.Size operandStackSize =
        new StackManipulation.Compound(stackManipulations)
            .apply(methodVisitor, implementationContext);
    return new Size(operandStackSize.getMaximalSize(), locals.stackSize());
  }

  /**
   * Retrurns a {@link StackManipulation} that checks whether the value in the message is a default
   * value, and if so jumps to after serialization to skip the serialization of the default value.
   * e.g.,
   *
   * <pre>{code
   *   if (message.getFoo() != field.getDefaultValue) {
   *     ...
   *   }
   *   // afterSerializeField
   * }</pre>
   */
  private StackManipulation checkDefaultValue(
      ProtoFieldInfo info,
      LocalVariables<LocalVariable> locals,
      StackManipulation getValue,
      StackManipulation getDefaultInstance,
      Label afterSerializeField) {
    if (info.isInOneof()) {
      // For one-ofs, we can just check the set field number directly.
      return new StackManipulation.Compound(
          locals.load(LocalVariable.message),
          CodeGenUtil.invoke(info.oneOfCaseMethod()),
          EnumLite_getNumber,
          IntegerConstant.forValue(info.descriptor().getNumber()),
          new IfIntsNotEqual(afterSerializeField));
    } else if (!info.isRepeated()) {
      switch (info.valueJavaType()) {
        case INT:
          return checkPrimitiveDefault(
              getValue,
              IntegerConstant.forValue((int) info.descriptor().getDefaultValue()),
              int.class,
              afterSerializeField);
        case LONG:
          return checkPrimitiveDefault(
              getValue,
              LongConstant.forValue((long) info.descriptor().getDefaultValue()),
              long.class,
              afterSerializeField);
        case FLOAT:
          return checkPrimitiveDefault(
              getValue,
              FloatConstant.forValue((float) info.descriptor().getDefaultValue()),
              float.class,
              afterSerializeField);
        case DOUBLE:
          return checkPrimitiveDefault(
              getValue,
              DoubleConstant.forValue((double) info.descriptor().getDefaultValue()),
              double.class,
              afterSerializeField);
        case BOOLEAN:
          return checkPrimitiveDefault(
              getValue,
              IntegerConstant.forValue((boolean) info.descriptor().getDefaultValue()),
              boolean.class,
              afterSerializeField);
        case ENUM:
          return checkPrimitiveDefault(
              getValue,
              IntegerConstant.forValue(
                  ((EnumValueDescriptor) info.descriptor().getDefaultValue()).getNumber()),
              int.class,
              afterSerializeField);
        case STRING:
          return new StackManipulation.Compound(
              getValue,
              new TextConstant((String) info.descriptor().getDefaultValue()),
              Object_equals,
              new IfEqual(Object.class, afterSerializeField));
        case BYTE_STRING:
          // We'll use the default instance to get the default value for types that can't be
          // loaded into the constant pool. Since it's a constant, the somewhat indirect reference
          // should get inlined and be the same as a class constant.
          return new StackManipulation.Compound(
              getValue,
              getDefaultInstance,
              invoke(info.getValueMethod()),
              Object_equals,
              new IfEqual(Object.class, afterSerializeField));
        case MESSAGE:
          return new StackManipulation.Compound(
              locals.load(LocalVariable.message),
              invoke(info.hasValueMethod()),
              new IfFalse(afterSerializeField));
        default:
          throw new IllegalStateException("Unknown JavaType: " + info.valueJavaType());
      }
    } else {
      return new StackManipulation.Compound(
          locals.load(LocalVariable.message),
          CodeGenUtil.invoke(info.repeatedValueCountMethod()),
          new IfFalse(afterSerializeField));
    }
  }

  private StackManipulation unbox(ProtoFieldInfo field) {
    switch (field.valueJavaType()) {
      case INT:
      case ENUM:
        return new StackManipulation.Compound(
            TypeCasting.to(new ForLoadedType(Integer.class)), PrimitiveUnboxingDelegate.INTEGER);
      case LONG:
        return new StackManipulation.Compound(
            TypeCasting.to(new ForLoadedType(Long.class)), PrimitiveUnboxingDelegate.LONG);
      case BOOLEAN:
        return new StackManipulation.Compound(
            TypeCasting.to(new ForLoadedType(Boolean.class)), PrimitiveUnboxingDelegate.BOOLEAN);
      case FLOAT:
        return new StackManipulation.Compound(
            TypeCasting.to(new ForLoadedType(Float.class)), PrimitiveUnboxingDelegate.FLOAT);
      case DOUBLE:
        return new StackManipulation.Compound(
            TypeCasting.to(new ForLoadedType(Double.class)), PrimitiveUnboxingDelegate.DOUBLE);
      case STRING:
        return TypeCasting.to(new ForLoadedType(String.class));
      case BYTE_STRING:
        return TypeCasting.to(new ForLoadedType(ByteString.class));
      case MESSAGE:
        return TypeCasting.to(new ForLoadedType(Message.class));
      default:
        throw new IllegalStateException("Unknown field type.");
    }
  }

  private StackManipulation printValue(
      Map<String, FieldDescription> fieldsByName, ProtoFieldInfo info) {
    boolean repeated = !info.isMapField() && info.isRepeated();
    switch (info.valueType()) {
      case INT32:
      case SINT32:
      case SFIXED32:
        return repeated
            ? SerializeSupport_printRepeatedSignedInt32
            : SerializeSupport_printSignedInt32;
      case INT64:
      case SINT64:
      case SFIXED64:
        return repeated
            ? SerializeSupport_printRepeatedSignedInt64
            : SerializeSupport_printSignedInt64;
      case BOOL:
        return repeated ? SerializeSupport_printRepeatedBool : SerializeSupport_printBool;
      case FLOAT:
        return repeated ? SerializeSupport_printRepeatedFloat : SerializeSupport_printFloat;
      case DOUBLE:
        return repeated ? SerializeSupport_printRepeatedDouble : SerializeSupport_printDouble;
      case UINT32:
      case FIXED32:
        return repeated
            ? SerializeSupport_printRepeatedUnsignedInt32
            : SerializeSupport_printUnsignedInt32;
      case UINT64:
      case FIXED64:
        return repeated
            ? SerializeSupport_printRepeatedUnsignedInt64
            : SerializeSupport_printUnsignedInt64;
      case STRING:
        return repeated ? SerializeSupport_printRepeatedString : SerializeSupport_printString;
      case BYTES:
        return repeated ? SerializeSupport_printRepeatedBytes : SerializeSupport_printBytes;
      case ENUM:
        // Special-case google.protobuf.NullValue (it's an Enum).
        if (info.valueField().descriptor().getEnumType().equals(NullValue.getDescriptor())) {
          return repeated ? SerializeSupport_printRepeatedNull : SerializeSupport_printNull;
        } else {
          return new StackManipulation.Compound(
              CodeGenUtil.getEnumDescriptor(info),
              repeated ? SerializeSupport_printRepeatedEnum : SerializeSupport_printEnum);
        }
      case MESSAGE:
      case GROUP:
        return new StackManipulation.Compound(
            FieldAccess.forField(
                    fieldsByName.get(
                        CodeGenUtil.fieldNameForNestedMarshaller(
                            info.valueField().descriptor().getMessageType())))
                .read(),
            repeated ? SerializeSupport_printRepeatedMessage : SerializeSupport_printMessage);
      default:
        throw new IllegalStateException("Unknown field type.");
    }
  }

  private StackManipulation checkPrimitiveDefault(
      StackManipulation getValue,
      StackManipulation loadDefault,
      Class<?> variableType,
      Label afterPrint) {
    return new StackManipulation.Compound(
        getValue, loadDefault, new IfEqual(variableType, afterPrint));
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
