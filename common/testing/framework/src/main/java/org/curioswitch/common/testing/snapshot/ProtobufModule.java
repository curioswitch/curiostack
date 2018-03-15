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

package org.curioswitch.common.testing.snapshot;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.protobuf.Message;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.curioswitch.common.protobuf.json.MessageMarshaller;

class ProtobufModule extends Module {

  private static final Set<Message> KNOWN_MESSAGES = new HashSet<>();

  private static final MessageMarshaller.Builder MESSAGE_MARSHALLER_BUILDER =
      MessageMarshaller.builder().omittingInsignificantWhitespace(false);

  @Nullable private static MessageMarshaller CURRENT_MARSHALLER = null;

  @SuppressWarnings("ReturnMissingNullable")
  private static MessageMarshaller getMarshaller(Message prototype) {
    if (KNOWN_MESSAGES.contains(prototype)) {
      checkNotNull(CURRENT_MARSHALLER);
      return CURRENT_MARSHALLER;
    }
    synchronized (ProtobufModule.class) {
      if (KNOWN_MESSAGES.contains(prototype)) {
        checkNotNull(CURRENT_MARSHALLER);
        return CURRENT_MARSHALLER;
      }
      KNOWN_MESSAGES.add(prototype);
      CURRENT_MARSHALLER = MESSAGE_MARSHALLER_BUILDER.register(prototype).build();
      return CURRENT_MARSHALLER;
    }
  }

  private static class MessageDeserializer extends StdDeserializer<Message> {

    MessageDeserializer() {
      super(Message.class);
    }

    MessageDeserializer(Class<?> cls) {
      super(cls);
    }

    @Override
    public Message deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      @SuppressWarnings("unchecked")
      Class<? extends Message> cast = (Class<? extends Message>) handledType();
      final Message prototype;
      try {
        prototype = (Message) cast.getMethod("getDefaultInstance").invoke(null);
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new IllegalStateException("Could not instantiate message protoype, can't happen.", e);
      }
      Message.Builder builder = prototype.toBuilder();
      getMarshaller(prototype).mergeValue(p, builder);
      return builder.build();
    }
  }

  private static class MessageSerializer extends StdSerializer<Message> {

    MessageSerializer() {
      super(Message.class);
    }

    @Override
    public void serialize(Message value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      getMarshaller(value.getDefaultInstanceForType()).writeValue(value, gen);
    }
  }

  private static class ProtobufSerializers extends Serializers.Base {

    @Override
    @Nullable
    public JsonSerializer<?> findSerializer(
        SerializationConfig config, JavaType type, BeanDescription beanDesc) {
      if (Message.class.isAssignableFrom(type.getRawClass())) {
        return new MessageSerializer();
      }
      return null;
    }
  }

  private static class ProtobufDeserializers extends Deserializers.Base {

    @Override
    @Nullable
    public JsonDeserializer<?> findBeanDeserializer(
        JavaType type, DeserializationConfig config, BeanDescription beanDesc) {
      if (Message.class.isAssignableFrom(type.getRawClass())) {
        return new MessageDeserializer(type.getRawClass());
      }
      return null;
    }
  }

  @Override
  public String getModuleName() {
    return "ProtbufModule";
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addSerializers(new ProtobufSerializers());
    context.addDeserializers(new ProtobufDeserializers());
  }
}
