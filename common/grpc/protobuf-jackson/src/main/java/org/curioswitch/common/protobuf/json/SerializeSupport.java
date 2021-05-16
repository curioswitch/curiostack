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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Message;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class SerializeSupport {

  // The implementations of the repeated members is all almost the same, so it may make sense to
  // codegen them. However, codegen of loops is complicated and more shared code should make it
  // slightly easier for the JVM to optimize. Anyways, the maintenance cost is low since it's
  // highly unlikely additional types will ever be added.
  public static void printRepeatedSignedInt32(List<Integer> values, JsonGenerator gen)
      throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printSignedInt32(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printSignedInt32(int value, JsonGenerator gen) throws IOException {
    gen.writeNumber(value);
  }

  public static void printRepeatedSignedInt64(List<Long> values, JsonGenerator gen)
      throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printSignedInt64(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printSignedInt64(long value, JsonGenerator gen) throws IOException {
    gen.writeString(Long.toString(value));
  }

  public static void printRepeatedUnsignedInt32(List<Integer> values, JsonGenerator gen)
      throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printUnsignedInt32(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printUnsignedInt32(int value, JsonGenerator gen) throws IOException {
    gen.writeNumber(normalizeUnsignedInt32(value));
  }

  public static long normalizeUnsignedInt32(int value) {
    return value >= 0 ? value : value & 0x00000000FFFFFFFFL;
  }

  public static void printRepeatedUnsignedInt64(List<Long> values, JsonGenerator gen)
      throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printUnsignedInt64(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printUnsignedInt64(long value, JsonGenerator gen) throws IOException {
    gen.writeString(normalizeUnsignedInt64(value));
  }

  public static String normalizeUnsignedInt64(long value) {
    return value >= 0
        ? Long.toString(value)
        // Pull off the most-significant bit so that BigInteger doesn't think
        // the number is negative, then set it again using setBit().
        : BigInteger.valueOf(value & Long.MAX_VALUE).setBit(Long.SIZE - 1).toString();
  }

  public static void printRepeatedBool(List<Boolean> values, JsonGenerator gen) throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printBool(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printBool(boolean value, JsonGenerator gen) throws IOException {
    gen.writeBoolean(value);
  }

  public static void printRepeatedFloat(List<Float> values, JsonGenerator gen) throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printFloat(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printFloat(float value, JsonGenerator gen) throws IOException {
    gen.writeNumber(value);
  }

  public static void printRepeatedDouble(List<Double> values, JsonGenerator gen)
      throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printDouble(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printDouble(double value, JsonGenerator gen) throws IOException {
    gen.writeNumber(value);
  }

  public static void printRepeatedString(List<String> values, JsonGenerator gen)
      throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printString(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printString(String value, JsonGenerator gen) throws IOException {
    gen.writeString(value);
  }

  public static void printRepeatedBytes(List<ByteString> values, JsonGenerator gen)
      throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printBytes(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printBytes(ByteString value, JsonGenerator gen) throws IOException {
    gen.writeBinary(value.toByteArray());
  }

  // Note: I hope no one ever actually calls this method...
  public static void printRepeatedNull(List<Integer> values, JsonGenerator gen) throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printNull(values.get(i), gen);
    }
    gen.writeEndArray();
  }

  public static void printNull(int unused, JsonGenerator gen) throws IOException {
    gen.writeNull();
  }

  public static void printRepeatedEnum(
      List<Integer> values, JsonGenerator gen, EnumDescriptor descriptor) throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printEnum(values.get(i), gen, descriptor);
    }
    gen.writeEndArray();
  }

  public static void printEnum(int value, JsonGenerator gen, EnumDescriptor descriptor)
      throws IOException {
    EnumValueDescriptor valueDescriptor = descriptor.findValueByNumber(value);
    if (valueDescriptor == null) {
      gen.writeNumber(value);
    } else {
      gen.writeString(valueDescriptor.getName());
    }
  }

  public static void printEnum(EnumValueDescriptor value, JsonGenerator gen) throws IOException {
    if (value.getIndex() == -1) {
      gen.writeString(Integer.toString(value.getNumber()));
    } else {
      gen.writeString(value.getName());
    }
  }

  public static <T extends Message> void printRepeatedMessage(
      List<T> values, JsonGenerator gen, TypeSpecificMarshaller<T> serializer) throws IOException {
    int numElements = values.size();
    gen.writeStartArray(numElements);
    for (int i = 0; i < numElements; i++) {
      printMessage(values.get(i), gen, serializer);
    }
    gen.writeEndArray();
  }

  public static <T extends Message> void printMessage(
      T value, JsonGenerator gen, TypeSpecificMarshaller<T> serializer) throws IOException {
    serializer.writeValue(value, gen);
  }

  public static SerializedString serializeString(String name) {
    SerializedString s = new SerializedString(name);
    // Eagerly compute encodings.
    s.asQuotedChars();
    s.asQuotedUTF8();
    s.asUnquotedUTF8();
    return s;
  }

  @SuppressWarnings("UnnecessaryLambda")
  private static final Comparator<Entry<String, ?>> STRING_KEY_COMPARATOR =
      (o1, o2) -> {
        ByteString s1 = ByteString.copyFromUtf8(o1.getKey());
        ByteString s2 = ByteString.copyFromUtf8(o2.getKey());
        return ByteString.unsignedLexicographicalComparator().compare(s1, s2);
      };

  public static Iterator<? extends Entry> mapIterator(
      Map<?, ?> map, boolean sortingMapKeys, boolean stringKey) {
    if (!sortingMapKeys) {
      return map.entrySet().iterator();
    }

    Comparator cmp = stringKey ? STRING_KEY_COMPARATOR : Map.Entry.comparingByKey();
    List<Entry<?, ?>> sorted = new ArrayList<>(map.entrySet());
    sorted.sort(cmp);
    return sorted.iterator();
  }

  private SerializeSupport() {}
}
