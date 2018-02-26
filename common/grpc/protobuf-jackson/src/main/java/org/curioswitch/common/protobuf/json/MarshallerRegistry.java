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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.util.Map;

/**
 * A registry for looking up {@link TypeSpecificMarshaller} for a given protobuf {@link Descriptor}
 * or full name of the protobuf type.
 */
final class MarshallerRegistry {

  // Optimize for the common case of finding a serializer by Descriptor, which hashes much faster
  // than String. We create a map from String as well for use when resolving by type variableName
  // for serialization of Any. Iterating over the descriptors instead of creating a parallel map
  // would be reasonable too, but the memory usage should be tiny.
  private final Map<Descriptor, TypeSpecificMarshaller<?>> descriptorRegistry;
  private final Map<String, TypeSpecificMarshaller<?>> typeNameRegistry;

  MarshallerRegistry(Map<Descriptor, TypeSpecificMarshaller<?>> descriptorRegistry) {
    this.descriptorRegistry = ImmutableMap.copyOf(descriptorRegistry);
    ImmutableMap.Builder<String, TypeSpecificMarshaller<?>> typeNameRegistry =
        ImmutableMap.builder();
    for (Map.Entry<Descriptor, TypeSpecificMarshaller<?>> entry : descriptorRegistry.entrySet()) {
      typeNameRegistry.put(entry.getKey().getFullName(), entry.getValue());
    }
    this.typeNameRegistry = typeNameRegistry.build();
  }

  /**
   * Returns the {@link TypeSpecificMarshaller} that can marshall protobufs with the same type as
   * {@code prototype}.
   */
  TypeSpecificMarshaller<?> findForPrototype(Message prototype) {
    TypeSpecificMarshaller<?> marshaller = descriptorRegistry.get(prototype.getDescriptorForType());
    if (marshaller == null) {
      throw new IllegalArgumentException(
          "Could not find marshaller for type: "
              + prototype.getDescriptorForType()
              + ". Has it been registered?");
    }
    return marshaller;
  }

  /**
   * Returns the {@link TypeSpecificMarshaller} that can marshall protobufs with type url {@code
   * typeUrl}.
   */
  // Used by Any.
  TypeSpecificMarshaller<?> findByTypeUrl(String typeUrl) throws InvalidProtocolBufferException {
    String typeName = getTypeName(typeUrl);
    TypeSpecificMarshaller<?> marshaller = typeNameRegistry.get(typeName);
    if (marshaller == null) {
      throw new InvalidProtocolBufferException("Cannot find type for url: " + typeUrl);
    }
    return marshaller;
  }

  private static String getTypeName(String typeUrl) throws InvalidProtocolBufferException {
    String[] parts = typeUrl.split("/");
    if (parts.length == 1) {
      throw new InvalidProtocolBufferException("Invalid type url found: " + typeUrl);
    }
    return parts[parts.length - 1];
  }
}
