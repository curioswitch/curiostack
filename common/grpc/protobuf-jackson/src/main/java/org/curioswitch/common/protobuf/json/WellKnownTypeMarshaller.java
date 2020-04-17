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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.FieldMaskUtil;
import com.google.protobuf.util.Timestamps;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * A {@link TypeSpecificMarshaller} for one of the message types that has a special JSON
 * representation. Wrappers, structs, and Any.
 */
abstract class WellKnownTypeMarshaller<T extends Message> extends TypeSpecificMarshaller<T> {

  /**
   * Marshalls a primitive value with a type wrapper {@link Message}. Since the JSON for these
   * wrappers is not an object, none of these {@link TypeSpecificMarshaller}s parse or output
   * objects.
   */
  private abstract static class WrapperMarshaller<T extends Message>
      extends WellKnownTypeMarshaller<T> {

    private WrapperMarshaller(T prototype) {
      super(prototype);
    }

    @Override
    final void mergeValue(JsonParser parser, int currentDepth, Message.Builder builder)
        throws IOException {
      doMerge(parser, currentDepth, builder);
    }

    @Override
    final void writeValue(T message, JsonGenerator gen) throws IOException {
      doWrite(message, gen);
    }
  }

  static final class BoolValueMarshaller extends WrapperMarshaller<BoolValue> {

    static BoolValueMarshaller INSTANCE = new BoolValueMarshaller();

    private BoolValueMarshaller() {
      super(BoolValue.getDefaultInstance());
    }

    @Override
    protected final void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      BoolValue.Builder builder = (BoolValue.Builder) messageBuilder;
      builder.setValue(ParseSupport.parseBool(parser));
    }

    @Override
    protected final void doWrite(BoolValue message, JsonGenerator gen) throws IOException {
      SerializeSupport.printBool(message.getValue(), gen);
    }
  }

  static final class Int32ValueMarshaller extends WrapperMarshaller<Int32Value> {

    static Int32ValueMarshaller INSTANCE = new Int32ValueMarshaller();

    private Int32ValueMarshaller() {
      super(Int32Value.getDefaultInstance());
    }

    @Override
    protected final void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      Int32Value.Builder builder = (Int32Value.Builder) messageBuilder;
      builder.setValue(ParseSupport.parseInt32(parser));
    }

    @Override
    protected final void doWrite(Int32Value message, JsonGenerator gen) throws IOException {
      SerializeSupport.printSignedInt32(message.getValue(), gen);
    }
  }

  static final class UInt32ValueMarshaller extends WrapperMarshaller<UInt32Value> {

    static UInt32ValueMarshaller INSTANCE = new UInt32ValueMarshaller();

    UInt32ValueMarshaller() {
      super(UInt32Value.getDefaultInstance());
    }

    @Override
    protected final void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      UInt32Value.Builder builder = (UInt32Value.Builder) messageBuilder;
      builder.setValue(ParseSupport.parseUInt32(parser));
    }

    @Override
    protected final void doWrite(UInt32Value message, JsonGenerator gen) throws IOException {
      SerializeSupport.printUnsignedInt32(message.getValue(), gen);
    }
  }

  static final class Int64ValueMarshaller extends WrapperMarshaller<Int64Value> {

    static Int64ValueMarshaller INSTANCE = new Int64ValueMarshaller();

    Int64ValueMarshaller() {
      super(Int64Value.getDefaultInstance());
    }

    @Override
    protected final void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      Int64Value.Builder builder = (Int64Value.Builder) messageBuilder;
      builder.setValue(ParseSupport.parseInt64(parser));
    }

    @Override
    protected final void doWrite(Int64Value message, JsonGenerator gen) throws IOException {
      SerializeSupport.printSignedInt64(message.getValue(), gen);
    }
  }

  static final class UInt64ValueMarshaller extends WrapperMarshaller<UInt64Value> {

    static UInt64ValueMarshaller INSTANCE = new UInt64ValueMarshaller();

    UInt64ValueMarshaller() {
      super(UInt64Value.getDefaultInstance());
    }

    @Override
    protected final void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      UInt64Value.Builder builder = (UInt64Value.Builder) messageBuilder;
      builder.setValue(ParseSupport.parseUInt64(parser));
    }

    @Override
    protected final void doWrite(UInt64Value message, JsonGenerator gen) throws IOException {
      SerializeSupport.printUnsignedInt64(message.getValue(), gen);
    }
  }

  static final class StringValueMarshaller extends WrapperMarshaller<StringValue> {

    static StringValueMarshaller INSTANCE = new StringValueMarshaller();

    StringValueMarshaller() {
      super(StringValue.getDefaultInstance());
    }

    @Override
    protected final void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      StringValue.Builder builder = (StringValue.Builder) messageBuilder;
      builder.setValue(ParseSupport.parseString(parser));
    }

    @Override
    protected final void doWrite(StringValue message, JsonGenerator gen) throws IOException {
      SerializeSupport.printString(message.getValue(), gen);
    }
  }

  static final class BytesValueMarshaller extends WrapperMarshaller<BytesValue> {

    static BytesValueMarshaller INSTANCE = new BytesValueMarshaller();

    BytesValueMarshaller() {
      super(BytesValue.getDefaultInstance());
    }

    @Override
    protected final void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      BytesValue.Builder b = (BytesValue.Builder) messageBuilder;
      b.setValue(ParseSupport.parseBytes(parser));
    }

    @Override
    protected final void doWrite(BytesValue message, JsonGenerator gen) throws IOException {
      SerializeSupport.printBytes(message.getValue(), gen);
    }
  }

  static final class FloatValueMarshaller extends WrapperMarshaller<FloatValue> {

    static FloatValueMarshaller INSTANCE = new FloatValueMarshaller();

    FloatValueMarshaller() {
      super(FloatValue.getDefaultInstance());
    }

    @Override
    protected final void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      FloatValue.Builder builder = (FloatValue.Builder) messageBuilder;
      builder.setValue(ParseSupport.parseFloat(parser));
    }

    @Override
    protected final void doWrite(FloatValue message, JsonGenerator gen) throws IOException {
      SerializeSupport.printFloat(message.getValue(), gen);
    }
  }

  static final class DoubleValueMarshaller extends WrapperMarshaller<DoubleValue> {

    static DoubleValueMarshaller INSTANCE = new DoubleValueMarshaller();

    DoubleValueMarshaller() {
      super(DoubleValue.getDefaultInstance());
    }

    @Override
    protected final void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      DoubleValue.Builder builder = (DoubleValue.Builder) messageBuilder;
      builder.setValue(ParseSupport.parseDouble(parser));
    }

    @Override
    protected final void doWrite(DoubleValue message, JsonGenerator gen) throws IOException {
      SerializeSupport.printDouble(message.getValue(), gen);
    }
  }

  static final class TimestampMarshaller extends WrapperMarshaller<Timestamp> {

    static TimestampMarshaller INSTANCE = new TimestampMarshaller();

    TimestampMarshaller() {
      super(Timestamp.getDefaultInstance());
    }

    @Override
    public void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      Timestamp.Builder builder = (Timestamp.Builder) messageBuilder;
      try {
        builder.mergeFrom(Timestamps.parse(ParseSupport.parseString(parser)));
      } catch (ParseException e) {
        throw new InvalidProtocolBufferException(
            "Failed to readValue timestamp: " + parser.getText());
      }
    }

    @Override
    public void doWrite(Timestamp message, JsonGenerator gen) throws IOException {
      gen.writeString(Timestamps.toString(message));
    }
  }

  static final class DurationMarshaller extends WrapperMarshaller<Duration> {

    static DurationMarshaller INSTANCE = new DurationMarshaller();

    DurationMarshaller() {
      super(Duration.getDefaultInstance());
    }

    @Override
    public void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      Duration.Builder builder = (Duration.Builder) messageBuilder;
      try {
        builder.mergeFrom(Durations.parse(ParseSupport.parseString(parser)));
      } catch (ParseException e) {
        throw new InvalidProtocolBufferException(
            "Failed to readValue duration: " + parser.getText());
      }
    }

    @Override
    public void doWrite(Duration message, JsonGenerator gen) throws IOException {
      gen.writeString(Durations.toString(message));
    }
  }

  static final class FieldMaskMarshaller extends WrapperMarshaller<FieldMask> {

    static FieldMaskMarshaller INSTANCE = new FieldMaskMarshaller();

    FieldMaskMarshaller() {
      super(FieldMask.getDefaultInstance());
    }

    @Override
    public void doMerge(JsonParser parser, int unused, Message.Builder messageBuilder)
        throws IOException {
      FieldMask.Builder builder = (FieldMask.Builder) messageBuilder;
      builder.mergeFrom(FieldMaskUtil.fromJsonString(ParseSupport.parseString(parser)));
    }

    @Override
    public void doWrite(FieldMask message, JsonGenerator gen) throws IOException {
      gen.writeString(FieldMaskUtil.toJsonString(message));
    }
  }

  static final class StructMarshaller extends WellKnownTypeMarshaller<Struct> {

    static StructMarshaller INSTANCE = new StructMarshaller();

    StructMarshaller() {
      super(Struct.getDefaultInstance());
    }

    @Override
    public void doMerge(JsonParser parser, int currentDepth, Message.Builder messageBuilder)
        throws IOException {
      Struct.Builder builder = (Struct.Builder) messageBuilder;
      while (parser.nextValue() != JsonToken.END_OBJECT) {
        builder.putFields(
            parser.getCurrentName(), ValueMarshaller.INSTANCE.readValue(parser, currentDepth + 1));
      }
    }

    @Override
    protected void doWrite(Struct message, JsonGenerator gen) throws IOException {
      for (Map.Entry<String, Value> entry : message.getFieldsMap().entrySet()) {
        gen.writeFieldName(entry.getKey());
        ValueMarshaller.INSTANCE.writeValue(entry.getValue(), gen);
      }
    }
  }

  static final class ValueMarshaller extends WrapperMarshaller<Value> {

    static ValueMarshaller INSTANCE = new ValueMarshaller();

    ValueMarshaller() {
      super(Value.getDefaultInstance());
    }

    @Override
    public void doMerge(JsonParser parser, int currentDepth, Message.Builder messageBuilder)
        throws IOException {
      Value.Builder builder = (Value.Builder) messageBuilder;
      JsonToken token = parser.currentToken();
      if (token.isBoolean()) {
        builder.setBoolValue(ParseSupport.parseBool(parser));
      } else if (token.isNumeric()) {
        builder.setNumberValue(ParseSupport.parseDouble(parser));
      } else if (token == JsonToken.VALUE_NULL) {
        builder.setNullValue(NullValue.NULL_VALUE);
      } else if (token.isScalarValue()) {
        builder.setStringValue(ParseSupport.parseString(parser));
      } else if (token == JsonToken.START_OBJECT) {
        Struct.Builder structBuilder = builder.getStructValueBuilder();
        StructMarshaller.INSTANCE.mergeValue(parser, currentDepth + 1, structBuilder);
      } else if (token == JsonToken.START_ARRAY) {
        ListValue.Builder listValueBuilder = builder.getListValueBuilder();
        ListValueMarshaller.INSTANCE.mergeValue(parser, currentDepth + 1, listValueBuilder);
      } else {
        throw new IllegalStateException("Unexpected json data: " + parser.getText());
      }
    }

    @Override
    public void doWrite(Value message, JsonGenerator gen) throws IOException {
      switch (message.getKindCase()) {
        case NULL_VALUE:
          SerializeSupport.printNull(0, gen);
          break;
        case NUMBER_VALUE:
          SerializeSupport.printDouble(message.getNumberValue(), gen);
          break;
        case STRING_VALUE:
          SerializeSupport.printString(message.getStringValue(), gen);
          break;
        case BOOL_VALUE:
          SerializeSupport.printBool(message.getBoolValue(), gen);
          break;
        case STRUCT_VALUE:
          StructMarshaller.INSTANCE.writeValue(message.getStructValue(), gen);
          break;
        case LIST_VALUE:
          ListValueMarshaller.INSTANCE.writeValue(message.getListValue(), gen);
          break;
        case KIND_NOT_SET:
          SerializeSupport.printNull(0, gen);
          break;
      }
    }
  }

  static final class ListValueMarshaller extends WrapperMarshaller<ListValue> {

    static ListValueMarshaller INSTANCE = new ListValueMarshaller();

    ListValueMarshaller() {
      super(ListValue.getDefaultInstance());
    }

    @Override
    public void doMerge(JsonParser parser, int currentDepth, Message.Builder messageBuilder)
        throws IOException {
      JsonToken token = parser.currentToken();
      if (token != JsonToken.START_ARRAY) {
        throw new InvalidProtocolBufferException("Expect an array but found: " + parser.getText());
      }
      ListValue.Builder builder = (ListValue.Builder) messageBuilder;
      while (parser.nextValue() != JsonToken.END_ARRAY) {
        Value.Builder valueBuilder = builder.addValuesBuilder();
        ValueMarshaller.INSTANCE.mergeValue(parser, currentDepth + 1, valueBuilder);
      }
    }

    @Override
    public void doWrite(ListValue message, JsonGenerator gen) throws IOException {
      List<Value> values = message.getValuesList();
      int numElements = values.size();
      gen.writeStartArray(numElements);
      for (int i = 0; i < numElements; i++) {
        ValueMarshaller.INSTANCE.writeValue(values.get(i), gen);
      }
      gen.writeEndArray();
    }
  }

  static final class AnyMarshaller extends WellKnownTypeMarshaller<Any> {

    // As Any needs to be created before other marshallers, but needs the definition of other
    // marshallers to unpack, this is set lazily.
    private MarshallerRegistry marshallerRegistry;

    AnyMarshaller() {
      super(Any.getDefaultInstance());
    }

    void setMarshallerRegistry(MarshallerRegistry marshallerRegistry) {
      checkNotNull(marshallerRegistry, "marshallerRegistry");
      checkState(this.marshallerRegistry == null, "marshallerRegistry has already been set.");
      this.marshallerRegistry = marshallerRegistry;
    }

    @Override
    public void doMerge(JsonParser parser, int currentDepth, Message.Builder messageBuilder)
        throws IOException {
      JsonToken token = parser.nextValue();
      if (token == JsonToken.END_OBJECT) {
        return;
      }
      Any.Builder builder = (Any.Builder) messageBuilder;
      if (!parser.getCurrentName().equals("@type")) {
        throw new InvalidProtocolBufferException(
            "MessageMarshaller requires @type to must be the "
                + "first field of an Any. If you need to support @type in any location, use "
                + "upstream JsonFormat. Found: "
                + parser.getText());
      }
      String typeUrl = ParseSupport.parseString(parser);
      TypeSpecificMarshaller<?> contentMarshaller = marshallerRegistry.findByTypeUrl(typeUrl);
      builder.setTypeUrl(typeUrl);
      if (contentMarshaller instanceof WellKnownTypeMarshaller) {
        parser.nextValue();
        if (parser.getCurrentName().equals("value")) {
          builder.setValue(contentMarshaller.readValue(parser, currentDepth).toByteString());
        }
        // Well-known types will not finish parsing the current object (they don't readValue
        // objects),
        // so we close it here.
        if (parser.nextValue() != JsonToken.END_OBJECT) {
          throw new InvalidProtocolBufferException(
              "Expected end of object, got: " + parser.getText());
        }
      } else {
        builder.setValue(
            contentMarshaller
                .parseRemainingFieldsOfObjectAsMessage(parser, currentDepth + 1)
                .toByteString());
      }
    }

    @Override
    public void writeValue(Any message, JsonGenerator gen) throws IOException {
      if (message.equals(Any.getDefaultInstance())) {
        // Note: empty Any is not indented the same way as an empty message, this is likely an
        // upstream bug.
        gen.writeRaw(": {}");
        return;
      }
      gen.writeStartObject();
      String typeUrl = message.getTypeUrl();
      TypeSpecificMarshaller<?> serializer = marshallerRegistry.findByTypeUrl(typeUrl);
      gen.writeFieldName("@type");
      gen.writeString(typeUrl);
      if (serializer instanceof WellKnownTypeMarshaller) {
        gen.writeFieldName("value");
        serializer.writeValue(message.getValue(), gen);
      } else {
        serializer.doWrite(message.getValue(), gen);
      }
      gen.writeEndObject();
    }
  }

  private WellKnownTypeMarshaller(T prototype) {
    super(prototype);
  }
}
