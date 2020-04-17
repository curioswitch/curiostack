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
package org.curioswitch.common.protobuf.json;

import static org.curioswitch.common.protobuf.json.CodeGenUtil.invoke;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import com.google.protobuf.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription.ForLoadedType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import org.curioswitch.common.protobuf.json.LocalVariables.VariableHandle;
import org.curioswitch.common.protobuf.json.bytebuddy.Goto;
import org.curioswitch.common.protobuf.json.bytebuddy.IfEqual;
import org.curioswitch.common.protobuf.json.bytebuddy.IfRefsEqual;
import org.curioswitch.common.protobuf.json.bytebuddy.IfRefsNotEqual;
import org.curioswitch.common.protobuf.json.bytebuddy.IfTrue;
import org.curioswitch.common.protobuf.json.bytebuddy.SetJumpTargetLabel;

/**
 * {@link ByteCodeAppender} to generate code for parsing a specific {@link Message} type. Iterates
 * through the {@link JsonParser}, checking field names and dispatching to field's parsing logic.
 *
 * <p>Generated code looks something like:
 *
 * <pre>{@code
 * int setFieldBits0 = 0;  // Each bitset can support 32 field numbers.
 * int setFieldBits1 = 1;
 *
 * while (!ParseSupport.checkObjectEnd(parser.nextValue()) {
 *   String fieldName = parser.getCurrentName();
 *   // JsonParser returns interned field names, which works well with our class constants.
 *   if (fieldName == "fieldOne" || fieldName == "field_one") {
 *     setFieldBits0 = ParseSupport.throwIfFieldAlreadyWritten(
 *         setFieldBits0, 0x1 << (1 % 32 - 1), "fieldOne");
 *     builder.setFieldOne(ParseSupport.parseInt32(parser));
 *   } else if (fieldName == "fieldTwo" || fieldName == "field_two") {
 *     setFieldBits0 = ParseSupport.throwIfFieldAlreadyWritten(
 *         setFieldBits0, 0x1 << (2 % 32 - 1), "fieldTwo");
 *     ParseSupport.checkArrayStart();
 *     while (!ParseSupport.checkArrayEnd()) {
 *       builder.addFieldTwo(ParseSupport.parseString(parser));
 *     }
 *   } else if (fieldName == "fieldThirtyThree" || fieldName == "field_thirty) {
 *     setFieldBits1 = ParseSupport.throwIfFieldAlreadyWritten(
 *         setFieldBits1, 0x1 << (33 % 32 - 1), "fieldThirtyThree");
 *     ParseSupport.checkObjectStart();
 *     while (!ParseSupport.checkObjectEnd(parser.nextToken())) {
 *       builder.putFieldThirtyThree(
 *           ParseSupport.parseUnsignedInt64(parser), ParseSupport.parseString(parser));
 *     }
 *   }
 * }
 *
 * }</pre>
 */
final class DoParse implements ByteCodeAppender, Implementation {

  // We dynamically compute stack variables for duplicate field presence, so can't use an enum.
  private static class LocalVariable implements VariableHandle {
    private static final LocalVariable parser = new LocalVariable("parser");
    private static final LocalVariable currentDepth = new LocalVariable("currentDepth");
    private static final LocalVariable messageBuilder = new LocalVariable("messageBuilder");
    private static final LocalVariable builder = new LocalVariable("builder");
    private static final LocalVariable fieldName = new LocalVariable("fieldName");
    private static final LocalVariable intvalue = new LocalVariable("intValue");
    private static final LocalVariable intMapKey = new LocalVariable("intMapKey");
    private static final LocalVariable longMapKey = new LocalVariable("longMapKey");
    private static final LocalVariable boolMapKey = new LocalVariable("boolMapKey");
    private static final LocalVariable stringMapKey = new LocalVariable("stringMapKey");

    private final String name;

    private LocalVariable(String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }

    static LocalVariable[] values() {
      return new LocalVariable[] {
        parser,
        currentDepth,
        messageBuilder,
        builder,
        fieldName,
        intvalue,
        intMapKey,
        longMapKey,
        boolMapKey,
        stringMapKey
      };
    }
  }

  private static final StackManipulation Parser_getCurrentName;
  private static final StackManipulation Parser_currentToken;
  private static final StackManipulation Parser_nextValue;
  private static final StackManipulation Parser_nextToken;

  private static final StackManipulation ParseSupport_parseArrayStart;
  private static final StackManipulation ParseSupport_checkArrayEnd;
  private static final StackManipulation ParseSupport_parseObjectStart;
  private static final StackManipulation ParseSupport_checkObjectEnd;
  private static final StackManipulation ParseSupport_checkNull;
  private static final StackManipulation ParseSupport_throwIfRepeatedNull;
  private static final StackManipulation ParseSupport_throwIfFieldAlreadyWritten;
  private static final StackManipulation ParseSupport_throwIfOneofAlreadyWritten;
  private static final StackManipulation ParseSupport_throwIfUnknownField;

  private static final StackManipulation ParseSupport_parseInt32;
  private static final StackManipulation ParseSupport_parseInt64;
  private static final StackManipulation ParseSupport_parseUint32;
  private static final StackManipulation ParseSupport_parseUint64;
  private static final StackManipulation ParseSupport_parseBool;
  private static final StackManipulation ParseSupport_parseFloat;
  private static final StackManipulation ParseSupport_parseDouble;
  private static final StackManipulation ParseSupport_parseString;
  private static final StackManipulation ParseSupport_parseBytes;
  private static final StackManipulation ParseSupport_parseEnum;
  private static final StackManipulation ParseSupport_mapUnknownEnumValue;
  private static final StackManipulation ParseSupport_parseMessage;

  static {
    try {
      Parser_getCurrentName = invoke(JsonParser.class.getDeclaredMethod("getCurrentName"));
      Parser_currentToken = invoke(JsonParser.class.getDeclaredMethod("currentToken"));
      Parser_nextValue = invoke(JsonParser.class.getDeclaredMethod("nextValue"));
      Parser_nextToken = invoke(JsonParser.class.getDeclaredMethod("nextToken"));

      ParseSupport_parseArrayStart =
          invoke(ParseSupport.class.getDeclaredMethod("parseArrayStart", JsonParser.class));
      ParseSupport_checkArrayEnd =
          invoke(ParseSupport.class.getDeclaredMethod("checkArrayEnd", JsonParser.class));
      ParseSupport_parseObjectStart =
          invoke(ParseSupport.class.getDeclaredMethod("parseObjectStart", JsonParser.class));
      ParseSupport_checkObjectEnd =
          invoke(ParseSupport.class.getDeclaredMethod("checkObjectEnd", JsonToken.class));
      ParseSupport_checkNull =
          invoke(ParseSupport.class.getDeclaredMethod("checkNull", JsonParser.class));
      ParseSupport_throwIfRepeatedNull =
          invoke(
              ParseSupport.class.getDeclaredMethod("throwIfRepeatedValueNull", JsonParser.class));
      ParseSupport_throwIfFieldAlreadyWritten =
          invoke(
              ParseSupport.class.getDeclaredMethod(
                  "throwIfFieldAlreadyWritten", int.class, int.class, String.class));
      ParseSupport_throwIfOneofAlreadyWritten =
          invoke(
              ParseSupport.class.getDeclaredMethod(
                  "throwIfOneofAlreadyWritten",
                  JsonParser.class,
                  Object.class,
                  String.class,
                  boolean.class));
      ParseSupport_throwIfUnknownField =
          invoke(
              ParseSupport.class.getDeclaredMethod(
                  "throwIfUnknownField", String.class, String.class));

      ParseSupport_parseInt32 =
          invoke(ParseSupport.class.getDeclaredMethod("parseInt32", JsonParser.class));
      ParseSupport_parseInt64 =
          invoke(ParseSupport.class.getDeclaredMethod("parseInt64", JsonParser.class));
      ParseSupport_parseUint32 =
          invoke(ParseSupport.class.getDeclaredMethod("parseUInt32", JsonParser.class));
      ParseSupport_parseUint64 =
          invoke(ParseSupport.class.getDeclaredMethod("parseUInt64", JsonParser.class));
      ParseSupport_parseBool =
          invoke(ParseSupport.class.getDeclaredMethod("parseBool", JsonParser.class));
      ParseSupport_parseFloat =
          invoke(ParseSupport.class.getDeclaredMethod("parseFloat", JsonParser.class));
      ParseSupport_parseDouble =
          invoke(ParseSupport.class.getDeclaredMethod("parseDouble", JsonParser.class));
      ParseSupport_parseString =
          invoke(ParseSupport.class.getDeclaredMethod("parseString", JsonParser.class));
      ParseSupport_parseBytes =
          invoke(ParseSupport.class.getDeclaredMethod("parseBytes", JsonParser.class));
      ParseSupport_parseEnum =
          invoke(
              ParseSupport.class.getDeclaredMethod(
                  "parseEnum", JsonParser.class, EnumDescriptor.class, boolean.class));
      ParseSupport_mapUnknownEnumValue =
          invoke(ParseSupport.class.getDeclaredMethod("mapUnknownEnumValue", int.class));
      ParseSupport_parseMessage =
          invoke(
              ParseSupport.class.getDeclaredMethod(
                  "parseMessage", JsonParser.class, TypeSpecificMarshaller.class, int.class));
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Could not find expected method.", e);
    }
  }

  private final Message prototype;
  private final Class<? extends Message.Builder> builderClass;
  private final Descriptor descriptor;
  private final boolean ignoringUnknownFields;

  DoParse(Message prototype, boolean ignoringUnknownFields) {
    this.prototype = prototype;
    builderClass = prototype.newBuilderForType().getClass();
    descriptor = prototype.getDescriptorForType();
    this.ignoringUnknownFields = ignoringUnknownFields;
  }

  @Override
  public Size apply(
      MethodVisitor methodVisitor,
      Context implementationContext,
      MethodDescription instrumentedMethod) {

    Map<String, FieldDescription> fieldsByName = CodeGenUtil.fieldsByName(implementationContext);

    List<FieldDescriptor> sortedFields = CodeGenUtil.sorted(descriptor.getFields());

    // Fields stored in order of number, so to get the highest number we take the last field.
    FieldDescriptor lastField = Iterables.getLast(sortedFields, null);
    // Field numbers are 1 or greater so we will allocate the same size as the last field number
    // and subtract 1 when keeping track.
    int lastFieldNumber = lastField != null ? lastField.getNumber() : 0;

    int numFieldPresenceWords = lastFieldNumber / Integer.SIZE;
    if (lastFieldNumber % Integer.SIZE > 0) {
      numFieldPresenceWords++;
    }

    List<LocalVariable> fieldPresenceVars = new ArrayList<>();
    for (int i = 0; i < numFieldPresenceWords; i++) {
      fieldPresenceVars.add(new LocalVariable("setFieldBits" + i));
    }

    LocalVariables.Builder<LocalVariable> localsBuilder =
        LocalVariables.builderForMethod(
                instrumentedMethod,
                ObjectArrays.concat(
                    LocalVariable.values(),
                    Iterables.toArray(fieldPresenceVars, LocalVariable.class),
                    LocalVariable.class))
            .add(builderClass, LocalVariable.builder)
            .add(String.class, LocalVariable.fieldName)
            .add(int.class, LocalVariable.intvalue)
            .add(int.class, LocalVariable.intMapKey)
            .add(long.class, LocalVariable.longMapKey)
            .add(boolean.class, LocalVariable.boolMapKey)
            .add(String.class, LocalVariable.stringMapKey);
    for (LocalVariable fieldPresenceVar : fieldPresenceVars) {
      localsBuilder.add(int.class, fieldPresenceVar);
    }
    LocalVariables<LocalVariable> locals = localsBuilder.build();

    List<StackManipulation> stackManipulations = new ArrayList<>();
    Label beforeReadField = new Label();
    Label finished = new Label();

    // Initialize local variables for this method, e.g.,
    // void doMerge(JsonParser parser, int currentDepth, Message.Builder messageBuilder) {
    //   T builder = (T) messageBuilder;
    //   String fieldName = null;
    //   Object fieldNumberUnboxed = null;
    //   int fieldNumber = 0;
    //   int setFieldBits0 = 0;
    //   int setFieldBits1 = 0;
    //   ...
    stackManipulations.addAll(
        Arrays.asList(
            locals.initialize(),
            locals.load(LocalVariable.messageBuilder),
            TypeCasting.to(new ForLoadedType(builderClass)),
            locals.store(LocalVariable.builder)));

    // Begins the loop that reads fields from the JSON, reading the field name and computing its
    // hash, e.g.,
    // ...
    // while (parser.nextValue() != ParseSupport.checkObjectEnd) {
    //   fieldName = parser.getCurrentName();
    //   fieldNumberUnboxed = FIELD_NUMBERS.get(fieldName):
    //   if (fieldNumberUnboxed == null) {
    //     ParseSupport.throwIfUnknownField(fieldName, descriptor.getFullName());
    //     return
    //   }
    //   fieldNumber = (int) fieldNumberUnboxed;
    //   ...
    stackManipulations.addAll(
        Arrays.asList(
            new SetJumpTargetLabel(beforeReadField),
            locals.load(LocalVariable.parser),
            Parser_nextValue,
            ParseSupport_checkObjectEnd,
            new IfTrue(finished),
            locals.load(LocalVariable.parser),
            Parser_getCurrentName,
            locals.store(LocalVariable.fieldName)));

    for (FieldDescriptor f : sortedFields) {
      Label afterNameCheck = new Label();
      Label afterField = new Label();
      ProtoFieldInfo field = new ProtoFieldInfo(f, prototype);

      int fieldNumberZeroBased = field.descriptor().getNumber() - 1;
      LocalVariable fieldPresenceVar = fieldPresenceVars.get(fieldNumberZeroBased / Integer.SIZE);
      int fieldPreserveVarBitIndex = fieldNumberZeroBased % Integer.SIZE;

      // if-statement for checking whether the current JSON field name matches this field, e.g.,
      // if (fieldName == String.intern(field.getName())
      //     || fieldName == String.intern(field.getJsonName()) {
      //      ...
      // } else if ...
      stackManipulations.addAll(
          Arrays.asList(
              locals.load(LocalVariable.fieldName),
              new TextConstant(field.descriptor().getJsonName()),
              new IfRefsEqual(afterNameCheck),
              locals.load(LocalVariable.fieldName),
              new TextConstant(field.descriptor().getName()),
              new IfRefsNotEqual(afterField),
              new SetJumpTargetLabel(afterNameCheck)));

      // Check whether we have already seen this field in the JSON, which is not allowed. e.g.,
      // setFieldBitsN = ParseSupport.throwIfFieldAlreadyWritten(
      //   setFieldBitsN, 0x1 << field.getNumber() % 8);
      stackManipulations.addAll(
          Arrays.asList(
              locals.load(fieldPresenceVar),
              IntegerConstant.forValue(0x1 << fieldPreserveVarBitIndex),
              new TextConstant(field.descriptor().getFullName()),
              ParseSupport_throwIfFieldAlreadyWritten,
              locals.store(fieldPresenceVar)));

      // If the field is in a oneof, check if any field in the oneof was already set, which is not
      // allowed. e.g.,
      // ParseSupport.throwIfOneofAlreadyWritten(builder.getSomeOneofCase(), field.getFullName());
      if (field.isInOneof()) {
        try {
          stackManipulations.addAll(
              Arrays.asList(
                  locals.load(LocalVariable.parser),
                  locals.load(LocalVariable.builder),
                  invoke(builderClass.getDeclaredMethod(field.getOneOfCaseMethodName())),
                  new TextConstant(field.descriptor().getFullName()),
                  IntegerConstant.forValue(mustSkipNull(field.descriptor())),
                  ParseSupport_throwIfOneofAlreadyWritten));
        } catch (NoSuchMethodException e) {
          throw new IllegalStateException("Could not find oneof case method.", e);
        }
      }

      // For fields where null just means the default value, add a check to continue the field loop
      // if the value is null. e.g.,
      // if (ParseSupport.checkNull(parser)) {
      //   continue;
      // }
      if (mustSkipNull(field.descriptor())) {
        stackManipulations.addAll(
            Arrays.asList(
                locals.load(LocalVariable.parser),
                ParseSupport_checkNull,
                new IfTrue(beforeReadField)));
      }

      StackManipulation setValue = setFieldValue(field, beforeReadField, locals, fieldsByName);
      stackManipulations.add(setValue);
      stackManipulations.add(new SetJumpTargetLabel(afterField));
    }
    if (ignoringUnknownFields) {
      // If we found no corresponding field number, jump back to the beginning of the while loop.
      stackManipulations.add(new Goto(beforeReadField));
    } else {
      // If we found no corresponding field number, throw an exception.
      stackManipulations.addAll(
          Arrays.asList(
              locals.load(LocalVariable.fieldName),
              new TextConstant(descriptor.getFullName()),
              ParseSupport_throwIfUnknownField));
    }
    // End of the field processing while loop.
    stackManipulations.add(new SetJumpTargetLabel(finished));
    stackManipulations.add(MethodReturn.VOID);
    StackManipulation.Size operandStackSize =
        new StackManipulation.Compound(stackManipulations)
            .apply(methodVisitor, implementationContext);
    return new Size(operandStackSize.getMaximalSize(), locals.stackSize());
  }

  /**
   * Returns the {@link StackManipulation} for setting the value of a field. This will be all the
   * elements for a repeated field.
   *
   * @param info description of the field to set.
   * @param beforeReadField jump target for before reading a field, used once this field is
   *     completed being set.
   * @param locals the method local variables
   * @param fieldsByName the instance fields
   */
  private StackManipulation setFieldValue(
      ProtoFieldInfo info,
      Label beforeReadField,
      LocalVariables<LocalVariable> locals,
      Map<String, FieldDescription> fieldsByName) {
    if (info.isMapField()) {
      return setMapFieldValue(info, beforeReadField, locals, fieldsByName);
    } else {
      final StackManipulation setConcreteValue = invoke(info.setValueMethod());
      final StackManipulation setSingleValue;
      if (info.valueJavaType() == JavaType.MESSAGE) {
        setSingleValue =
            new StackManipulation.Compound(
                TypeCasting.to(new ForLoadedType(info.javaClass())), setConcreteValue);
      } else if (info.valueType() == Type.ENUM && !info.isRepeated()) {
        // For non-repeated enums, we treat unknown as the default value.
        setSingleValue =
            new StackManipulation.Compound(ParseSupport_mapUnknownEnumValue, setConcreteValue);
      } else {
        setSingleValue = setConcreteValue;
      }
      if (info.descriptor().isRepeated()) {
        return setRepeatedFieldValue(info, beforeReadField, locals, fieldsByName, setSingleValue);
      } else {
        // Set a singular value, e.g.,
        // builder.setFoo(readValue());
        return new StackManipulation.Compound(
            locals.load(LocalVariable.builder),
            locals.load(LocalVariable.parser),
            readValue(info, fieldsByName, locals),
            setSingleValue,
            Removal.SINGLE,
            new Goto(beforeReadField));
      }
    }
  }

  /**
   * Returns the {@link StackManipulation} for setting the value of a normal repeated field.
   *
   * <p>Roughly equivalent to:
   *
   * <pre>{@code
   * ParseSupport.parseArrayStart(parser);
   * while (!ParseSupport.checkArrayEnd(parser)) {
   *   builder.addFoo(readValue());
   * }
   * }</pre>
   */
  private StackManipulation setRepeatedFieldValue(
      ProtoFieldInfo info,
      Label beforeReadField,
      LocalVariables<LocalVariable> locals,
      Map<String, FieldDescription> fieldsByName,
      StackManipulation setSingleValue) {
    Label arrayStart = new Label();

    StackManipulation.Compound beforeRead =
        new StackManipulation.Compound(
            locals.load(LocalVariable.parser),
            ParseSupport_parseArrayStart,
            new SetJumpTargetLabel(arrayStart),
            locals.load(LocalVariable.parser),
            ParseSupport_throwIfRepeatedNull,
            locals.load(LocalVariable.parser),
            ParseSupport_checkArrayEnd,
            new IfTrue(beforeReadField));

    Label afterSet = new Label();

    StackManipulation.Compound setValueAndPrepareForNext =
        new StackManipulation.Compound(
            setSingleValue,
            Removal.SINGLE,
            new SetJumpTargetLabel(afterSet),
            locals.load(LocalVariable.parser),
            Parser_nextValue,
            Removal.SINGLE,
            new Goto(arrayStart));

    if (info.valueType() == Type.ENUM) {
      // We special-case enum since we may need to skip unknown values.
      return new StackManipulation.Compound(
          beforeRead,
          locals.load(LocalVariable.parser),
          readValue(info, fieldsByName, locals),
          locals.store(LocalVariable.intvalue),
          locals.load(LocalVariable.intvalue),
          IntegerConstant.forValue(-1),
          new IfEqual(int.class, afterSet),
          locals.load(LocalVariable.builder),
          locals.load(LocalVariable.intvalue),
          setValueAndPrepareForNext);
    } else {
      return new StackManipulation.Compound(
          beforeRead,
          locals.load(LocalVariable.builder),
          locals.load(LocalVariable.parser),
          readValue(info, fieldsByName, locals),
          setValueAndPrepareForNext);
    }
  }

  /**
   * Returns the {@link StackManipulation} for setting the value of a map field.
   *
   * <p>Roughly equivalent to:
   *
   * <pre>{@code
   * ParseSupport.parseObjectStart(parser);
   * while (!ParseSupport.checkObjectEnd(parser.currentToken())) {
   *   builder.putFoo(readKey(), readValue());
   * }
   * }</pre>
   */
  private StackManipulation setMapFieldValue(
      ProtoFieldInfo info,
      Label beforeReadField,
      LocalVariables<LocalVariable> locals,
      Map<String, FieldDescription> fieldsByName) {
    final StackManipulation setConcreteValue = invoke(info.setValueMethod());
    final StackManipulation setMapEntry;
    if (info.valueJavaType() == JavaType.MESSAGE) {
      setMapEntry =
          new StackManipulation.Compound(
              TypeCasting.to(new ForLoadedType(info.javaClass())), setConcreteValue);
    } else {
      setMapEntry = setConcreteValue;
    }
    Label mapStart = new Label();
    Label afterSet = new Label();

    StackManipulation.Compound beforeReadKey =
        new StackManipulation.Compound(
            locals.load(LocalVariable.parser),
            ParseSupport_parseObjectStart,
            new SetJumpTargetLabel(mapStart),
            locals.load(LocalVariable.parser),
            Parser_currentToken,
            ParseSupport_checkObjectEnd,
            new IfTrue(beforeReadField));

    StackManipulation.Compound setValueAndPrepareForNext =
        new StackManipulation.Compound(
            setMapEntry,
            Removal.SINGLE,
            new SetJumpTargetLabel(afterSet),
            locals.load(LocalVariable.parser),
            Parser_nextToken,
            Removal.SINGLE,
            new Goto(mapStart));

    if (info.valueType() == Type.ENUM) {
      // We special-case enum since we may need to skip unknown values.
      final LocalVariable keyVar;
      switch (info.mapKeyField().valueJavaType()) {
        case INT:
          keyVar = LocalVariable.intMapKey;
          break;
        case LONG:
          keyVar = LocalVariable.longMapKey;
          break;
        case BOOLEAN:
          keyVar = LocalVariable.boolMapKey;
          break;
        case STRING:
          keyVar = LocalVariable.stringMapKey;
          break;
        default:
          throw new IllegalArgumentException("Invalid map key type");
      }

      return new StackManipulation.Compound(
          beforeReadKey,
          locals.load(LocalVariable.parser),
          readValue(info.mapKeyField(), fieldsByName, locals),
          locals.store(keyVar),
          locals.load(LocalVariable.parser),
          Parser_nextToken,
          Removal.SINGLE,
          locals.load(LocalVariable.parser),
          readValue(info, fieldsByName, locals),
          locals.store(LocalVariable.intvalue),
          locals.load(LocalVariable.intvalue),
          IntegerConstant.forValue(-1),
          new IfEqual(int.class, afterSet),
          locals.load(LocalVariable.builder),
          locals.load(keyVar),
          locals.load(LocalVariable.intvalue),
          setValueAndPrepareForNext);
    } else {
      return new StackManipulation.Compound(
          beforeReadKey,
          locals.load(LocalVariable.builder),
          locals.load(LocalVariable.parser),
          readValue(info.mapKeyField(), fieldsByName, locals),
          locals.load(LocalVariable.parser),
          Parser_nextToken,
          Removal.SINGLE,
          locals.load(LocalVariable.parser),
          readValue(info, fieldsByName, locals),
          setValueAndPrepareForNext);
    }
  }

  @Override
  public ByteCodeAppender appender(Target implementationTarget) {
    return this;
  }

  @Override
  public InstrumentedType prepare(InstrumentedType instrumentedType) {
    return instrumentedType;
  }

  /**
   * Returns the {@link StackManipulation} for reading the JSON encoded value for the field. Just
   * dispatches to {@link ParseSupport} based on the field type.
   */
  private StackManipulation readValue(
      ProtoFieldInfo field,
      Map<String, FieldDescription> fieldsByName,
      LocalVariables<LocalVariable> locals) {
    switch (field.valueType()) {
      case INT32:
      case SINT32:
      case SFIXED32:
        return ParseSupport_parseInt32;
      case INT64:
      case SINT64:
      case SFIXED64:
        return ParseSupport_parseInt64;
      case BOOL:
        return ParseSupport_parseBool;
      case FLOAT:
        return ParseSupport_parseFloat;
      case DOUBLE:
        return ParseSupport_parseDouble;
      case UINT32:
      case FIXED32:
        return ParseSupport_parseUint32;
      case UINT64:
      case FIXED64:
        return ParseSupport_parseUint64;
      case STRING:
        return ParseSupport_parseString;
      case BYTES:
        return ParseSupport_parseBytes;
      case ENUM:
        return new StackManipulation.Compound(
            CodeGenUtil.getEnumDescriptor(field),
            IntegerConstant.forValue(ignoringUnknownFields),
            ParseSupport_parseEnum);
      case MESSAGE:
      case GROUP:
        return new StackManipulation.Compound(
            FieldAccess.forField(
                    fieldsByName.get(
                        CodeGenUtil.fieldNameForNestedMarshaller(
                            field.valueField().descriptor().getMessageType())))
                .read(),
            locals.load(LocalVariable.currentDepth),
            ParseSupport_parseMessage);
      default:
        throw new IllegalStateException("Unknown field type: " + field.valueType());
    }
  }

  /**
   * Determines whether we skip processing of the field if it is null. We usually skip null values
   * in the JSON to treat them as default, but must actually process the null for {@link Value} and
   * {@link NullValue} because it means their value must be set.
   */
  private static boolean mustSkipNull(FieldDescriptor field) {
    if (field.isRepeated()) {
      return true;
    }
    if (field.getJavaType() == JavaType.MESSAGE
        && field.getMessageType() == Value.getDescriptor()) {
      return false;
    }
    if (field.getJavaType() == JavaType.ENUM && field.getEnumType() == NullValue.getDescriptor()) {
      return false;
    }
    return true;
  }
}
