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

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.AnyMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.BoolValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.BytesValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.DoubleValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.DurationMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.FieldMaskMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.FloatValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.Int32ValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.Int64ValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.ListValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.StringValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.StructMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.TimestampMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.UInt32ValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.UInt64ValueMarshaller;
import org.curioswitch.common.protobuf.json.WellKnownTypeMarshaller.ValueMarshaller;

/**
 * A marshaller of pre-registered {@link Message} types. Specific bytecode for marshalling the
 * {@link Message} will be generated as a subclass of {@link TypeSpecificMarshaller} and used for
 * optimal serializing and parsing of JSON for protobufs. Use {@link #builder()} for setting up the
 * {@link MessageMarshaller} and registering types.
 *
 * <p>For example:
 *
 * <pre>{@code
 * MessageMarshaller marshaller = MessageMarshaller.builder()
 *     .omittingInsignificantWhitespace(true)
 *     .register(MyRequest.getDefaultInstance())
 *     .register(MyResponse.getDefaultInstance())
 *     .build();
 *
 * MyRequest.Builder requestBuilder = MyRequest.newBuilder();
 * marshaller.mergeValue(json, requestBuilder);
 *
 * MyResponse response = handle(requestBuilder.build());
 * return marshaller.writeValueAsBytes(response);
 *
 * }</pre>
 */
public class MessageMarshaller {

  /**
   * Returns a new {@link Builder} for registering {@link Message} types for use in a {@link
   * MessageMarshaller} as well as setting various serialization and parsing options.
   */
  public static Builder builder() {
    return new Builder();
  }

  private final JsonFactory jsonFactory =
      new JsonFactory().enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES).enable(Feature.ALLOW_COMMENTS);

  @Nullable private final PrettyPrinter prettyPrinter;

  private final MarshallerRegistry registry;

  private MessageMarshaller(@Nullable PrettyPrinter prettyPrinter, MarshallerRegistry registry) {
    this.prettyPrinter = prettyPrinter;
    this.registry = registry;
  }

  /**
   * Merges the JSON UTF-8 bytes into the provided {@link Message.Builder}.
   *
   * @throws InvalidProtocolBufferException if the input is not valid JSON format or there are
   *     unknown fields in the input.
   */
  public void mergeValue(byte[] json, Message.Builder builder) throws IOException {
    checkNotNull(json, "json");
    checkNotNull(builder, "builder");
    JsonParser parser = jsonFactory.createParser(json);
    mergeValue(parser, builder);
  }

  /**
   * Merges the JSON {@link String} into the provided {@link Message.Builder}.
   *
   * @throws InvalidProtocolBufferException if the input is not valid JSON format or there are
   *     unknown fields in the input.
   */
  public void mergeValue(String json, Message.Builder builder) throws IOException {
    checkNotNull(json, "json");
    checkNotNull(builder, "builder");
    JsonParser parser = jsonFactory.createParser(json);
    mergeValue(parser, builder);
  }

  /**
   * Merges the JSON bytes inside the provided {@link InputStream} into the provided {@link
   * Message.Builder}. Will not close the {@link InputStream}.
   *
   * @throws InvalidProtocolBufferException if the input is not valid JSON format or there are
   *     unknown fields in the input.
   */
  public void mergeValue(InputStream json, Message.Builder builder) throws IOException {
    checkNotNull(json, "json");
    checkNotNull(builder, "builder");
    JsonParser parser = jsonFactory.createParser(json);
    mergeValue(parser, builder);
  }

  /**
   * Merges the content inside the {@link JsonParser} into the provided {@link Message.Builder}.
   *
   * @throws InvalidProtocolBufferException if the input is not valid JSON format or there are
   *     unknown fields in the input.
   */
  public void mergeValue(JsonParser jsonParser, Message.Builder builder) throws IOException {
    checkNotNull(jsonParser, "jsonParser");
    checkNotNull(builder, "builder");
    TypeSpecificMarshaller<?> parser =
        registry.findForPrototype(builder.getDefaultInstanceForType());
    try {
      parser.mergeValue(jsonParser, 0, builder);
    } catch (InvalidProtocolBufferException e) {
      throw e;
    } catch (IOException e) {
      throw new InvalidProtocolBufferException(e);
    } finally {
      jsonParser.close();
    }
  }

  /**
   * Converts a {@link Message} into JSON as UTF-8 encoded bytes.
   *
   * @throws InvalidProtocolBufferException if there are unknown Any types in the message.
   */
  public <T extends Message> byte[] writeValueAsBytes(T message) throws IOException {
    checkNotNull(message, "message");
    ByteArrayBuilder builder = new ByteArrayBuilder(jsonFactory._getBufferRecycler());
    JsonGenerator gen = jsonFactory.createGenerator(builder);
    writeValue(message, gen);
    return builder.toByteArray();
  }

  /**
   * Converts a {@link Message} into a JSON {@link String}.
   *
   * @throws InvalidProtocolBufferException if there are unknown Any types in the message.
   */
  public <T extends Message> String writeValueAsString(T message) throws IOException {
    checkNotNull(message, "message");
    SegmentedStringWriter sw = new SegmentedStringWriter(jsonFactory._getBufferRecycler());
    JsonGenerator gen = jsonFactory.createGenerator(sw);
    writeValue(message, gen);
    return sw.getAndClear();
  }

  /**
   * Converts a {@link Message} into JSON, writing to the provided {@link OutputStream}. Does not
   * close the {@link OutputStream}.
   */
  public <T extends Message> void writeValue(T message, OutputStream out) throws IOException {
    checkNotNull(message, "message");
    checkNotNull(out, "out");
    JsonGenerator gen = jsonFactory.createGenerator(out);
    writeValue(message, gen);
  }

  /**
   * Converts a {@link Message} into a JSON, writing to the provided {@link JsonGenerator}.
   *
   * @throws InvalidProtocolBufferException if there are unknown Any types in the message.
   */
  public <T extends Message> void writeValue(T message, JsonGenerator gen) throws IOException {
    checkNotNull(message, "message");
    checkNotNull(gen, "gen");
    // TypeSpecificMarshaller for T.prototype is TypeSpecificMarshaller<T>
    @SuppressWarnings("unchecked")
    TypeSpecificMarshaller<T> serializer =
        (TypeSpecificMarshaller<T>) registry.findForPrototype(message.getDefaultInstanceForType());
    if (prettyPrinter != null) {
      gen.setPrettyPrinter(prettyPrinter);
    }
    try {
      serializer.writeValue(message, gen);
    } catch (InvalidProtocolBufferException e) {
      throw e;
    } catch (IOException e) {
      throw new InvalidProtocolBufferException(e);
    } finally {
      gen.close();
    }
  }

  /**
   * A {@link Builder} of {@link MessageMarshaller}s, allows registering {@link Message} types to
   * marshall and set options.
   */
  public static final class Builder {

    private boolean includingDefaultValueFields;
    private boolean preservingProtoFieldNames;
    private boolean omittingInsignificantWhitespace;
    private boolean ignoringUnknownFields;

    private final List<Message> prototypes = new ArrayList<>();

    /**
     * Registers the type of the provided {@link Message} for use with the created {@link
     * MessageMarshaller}. While any instance of the type to register can be used, this will
     * commonly be called with {@code getDefaultInstance()} on the type to register.
     *
     * <p>The provided {@link Message} and all nested {@link Message} types reachable from this one
     * will be registered and available for marshalling. For clarity, it's generally a good idea to
     * explicitly register any {@link Message} that you will pass to methods of {@link
     * MessageMarshaller} even if they are already registered as a nested {@link Message}.
     */
    public Builder register(Message prototype) {
      checkNotNull(prototype, "prototype");
      prototypes.add(prototype.getDefaultInstanceForType());
      return this;
    }

    /**
     * Registers the provided {@link Message} type for use with the created {@link
     * MessageMarshaller}. While any instance of the type to register can be used, this will
     * commonly be called with {@code getDefaultInstance()} on the type to register.
     *
     * <p>The provided {@link Message} and all nested {@link Message} types reachable from this one
     * will be registered and available for marshalling. For clarity, it's generally a good idea to
     * explicitly register any {@link Message} that you will pass to methods of {@link
     * MessageMarshaller} even if they are already registered as a nested {@link Message}.
     */
    public Builder register(Class<? extends Message> messageClass) {
      checkNotNull(messageClass, "messageClass");
      try {
        return register(
            (Message) messageClass.getDeclaredMethod("getDefaultInstance").invoke(null));
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new IllegalStateException(
            "No getDefaultInstance method on a Message class, this can never happen.", e);
      }
    }

    /**
     * Set whether unset fields will be serialized with their default values. Empty repeated fields
     * and map fields will be printed as well. The new Printer clones all other configurations from
     * the current.
     */
    public Builder includingDefaultValueFields(boolean includingDefaultValueFields) {
      this.includingDefaultValueFields = includingDefaultValueFields;
      return this;
    }

    /**
     * Set whether field names should use the original name in the .proto file instead of converting
     * to lowerCamelCase when serializing messages. When set, the json_name annotation will be
     * ignored.
     */
    public Builder preservingProtoFieldNames(boolean preservingProtoFieldNames) {
      this.preservingProtoFieldNames = preservingProtoFieldNames;
      return this;
    }

    /**
     * Whether the serialized JSON output will omit all insignificant whitespace. Insignificant
     * whitespace is defined by the JSON spec as whitespace that appear between JSON structural
     * elements:
     *
     * <pre>
     * ws = *(
     * %x20 /              ; Space
     * %x09 /              ; Horizontal tab
     * %x0A /              ; Line feed or New line
     * %x0D )              ; Carriage return
     * </pre>
     *
     * See <a href="https://tools.ietf.org/html/rfc7159">https://tools.ietf.org/html/rfc7159</a>
     */
    public Builder omittingInsignificantWhitespace(boolean omittingInsignificantWhitespace) {
      this.omittingInsignificantWhitespace = omittingInsignificantWhitespace;
      return this;
    }

    /**
     * Sets whether unknown fields should be allowed when parsing JSON input. When not set, an
     * exception will be thrown when encountering unknown fields.
     */
    public Builder ignoringUnknownFields(boolean ignoringUnknownFields) {
      this.ignoringUnknownFields = ignoringUnknownFields;
      return this;
    }

    /**
     * Returns the built {@link MessageMarshaller}, generating {@link TypeSpecificMarshaller} for
     * all registered {@link Message} types. Any {@link Message} types that have not been registered
     * will not be usable with the returned {@link MessageMarshaller}.
     */
    public MessageMarshaller build() {
      Map<Descriptor, TypeSpecificMarshaller<?>> builtParsers = new HashMap<>();
      addStandardParser(BoolValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(Int32ValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(UInt32ValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(Int64ValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(UInt64ValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(StringValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(BytesValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(FloatValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(DoubleValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(TimestampMarshaller.INSTANCE, builtParsers);
      addStandardParser(DurationMarshaller.INSTANCE, builtParsers);
      addStandardParser(FieldMaskMarshaller.INSTANCE, builtParsers);
      addStandardParser(StructMarshaller.INSTANCE, builtParsers);
      addStandardParser(ValueMarshaller.INSTANCE, builtParsers);
      addStandardParser(ListValueMarshaller.INSTANCE, builtParsers);

      AnyMarshaller anyParser = new AnyMarshaller();
      addStandardParser(anyParser, builtParsers);

      for (Message prototype : prototypes) {
        TypeSpecificMarshaller.buildAndAdd(
            prototype,
            includingDefaultValueFields,
            preservingProtoFieldNames,
            ignoringUnknownFields,
            builtParsers);
      }

      MarshallerRegistry registry = new MarshallerRegistry(builtParsers);
      anyParser.setMarshallerRegistry(registry);

      return new MessageMarshaller(
          omittingInsignificantWhitespace ? null : new MessagePrettyPrinter(), registry);
    }

    private static <T extends Message> void addStandardParser(
        TypeSpecificMarshaller<T> marshaller,
        Map<Descriptor, TypeSpecificMarshaller<?>> marshallers) {
      marshallers.put(marshaller.getDescriptorForMarshalledType(), marshaller);
    }

    private Builder() {}
  }

  private static class MessagePrettyPrinter extends DefaultPrettyPrinter {

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
      jg.writeRaw(": ");
    }

    @Override
    public void writeEndObject(JsonGenerator jg, int nrOfEntries) throws IOException {
      if (!_objectIndenter.isInline()) {
        --_nesting;
      }
      _objectIndenter.writeIndentation(jg, _nesting);
      jg.writeRaw('}');
    }

    @Override
    public void beforeArrayValues(JsonGenerator jg) throws IOException {}

    @Override
    public void writeEndArray(JsonGenerator gen, int nrOfValues) throws IOException {
      gen.writeRaw(']');
    }
  }
}
