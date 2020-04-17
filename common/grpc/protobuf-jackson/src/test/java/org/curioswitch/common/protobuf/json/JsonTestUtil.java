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

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonTestProto.TestAllTypes;
import com.google.protobuf.util.JsonTestProto.TestAllTypes.NestedEnum;
import com.google.protobuf.util.JsonTestProto.TestAllTypes.NestedMessage;
import com.google.protobuf.util.JsonTestProto.TestMap;

final class JsonTestUtil {

  static TestAllTypes testAllTypesAllFields() {
    TestAllTypes.Builder builder = TestAllTypes.newBuilder();
    builder.setOptionalInt32(1234);
    builder.setOptionalInt64(1234567890123456789L);
    builder.setOptionalUint32(5678);
    builder.setOptionalUint64(2345678901234567890L);
    builder.setOptionalSint32(9012);
    builder.setOptionalSint64(3456789012345678901L);
    builder.setOptionalFixed32(3456);
    builder.setOptionalFixed64(4567890123456789012L);
    builder.setOptionalSfixed32(7890);
    builder.setOptionalSfixed64(5678901234567890123L);
    builder.setOptionalFloat(1.5f);
    builder.setOptionalDouble(1.25);
    builder.setOptionalBool(true);
    builder.setOptionalString("Hello world!");
    builder.setOptionalBytes(ByteString.copyFrom(new byte[] {0, 1, 2}));
    builder.setOptionalNestedEnum(NestedEnum.BAR);
    builder.getOptionalNestedMessageBuilder().setValue(100);

    builder.addRepeatedInt32(1234);
    builder.addRepeatedInt64(1234567890123456789L);
    builder.addRepeatedUint32(5678);
    builder.addRepeatedUint64(2345678901234567890L);
    builder.addRepeatedSint32(9012);
    builder.addRepeatedSint64(3456789012345678901L);
    builder.addRepeatedFixed32(3456);
    builder.addRepeatedFixed64(4567890123456789012L);
    builder.addRepeatedSfixed32(7890);
    builder.addRepeatedSfixed64(5678901234567890123L);
    builder.addRepeatedFloat(1.5f);
    builder.addRepeatedDouble(1.25);
    builder.addRepeatedBool(true);
    builder.addRepeatedString("Hello world!");
    builder.addRepeatedBytes(ByteString.copyFrom(new byte[] {0, 1, 2}));
    builder.addRepeatedNestedEnum(NestedEnum.BAR);
    builder.addRepeatedNestedMessageBuilder().setValue(100);

    builder.addRepeatedInt32(234);
    builder.addRepeatedInt64(234567890123456789L);
    builder.addRepeatedUint32(678);
    builder.addRepeatedUint64(345678901234567890L);
    builder.addRepeatedSint32(012);
    builder.addRepeatedSint64(456789012345678901L);
    builder.addRepeatedFixed32(456);
    builder.addRepeatedFixed64(567890123456789012L);
    builder.addRepeatedSfixed32(890);
    builder.addRepeatedSfixed64(678901234567890123L);
    builder.addRepeatedFloat(11.5f);
    builder.addRepeatedDouble(11.25);
    builder.addRepeatedBool(true);
    builder.addRepeatedString("ello world!");
    builder.addRepeatedBytes(ByteString.copyFrom(new byte[] {1, 2}));
    builder.addRepeatedNestedEnum(NestedEnum.BAZ);
    builder.addRepeatedNestedMessageBuilder().setValue(200);
    return builder.build();
  }

  static TestMap testMapAllTypes() {
    TestMap.Builder builder = TestMap.newBuilder();
    builder.putInt32ToInt32Map(1, 10);
    builder.putInt64ToInt32Map(1234567890123456789L, 10);
    builder.putUint32ToInt32Map(2, 20);
    builder.putUint64ToInt32Map(2234567890123456789L, 20);
    builder.putSint32ToInt32Map(3, 30);
    builder.putSint64ToInt32Map(3234567890123456789L, 30);
    builder.putFixed32ToInt32Map(4, 40);
    builder.putFixed64ToInt32Map(4234567890123456789L, 40);
    builder.putSfixed32ToInt32Map(5, 50);
    builder.putSfixed64ToInt32Map(5234567890123456789L, 50);
    builder.putBoolToInt32Map(false, 6);
    builder.putStringToInt32Map("Hello", 10);

    builder.putInt32ToInt64Map(1, 1234567890123456789L);
    builder.putInt32ToUint32Map(2, 20);
    builder.putInt32ToUint64Map(2, 2234567890123456789L);
    builder.putInt32ToSint32Map(3, 30);
    builder.putInt32ToSint64Map(3, 3234567890123456789L);
    builder.putInt32ToFixed32Map(4, 40);
    builder.putInt32ToFixed64Map(4, 4234567890123456789L);
    builder.putInt32ToSfixed32Map(5, 50);
    builder.putInt32ToSfixed64Map(5, 5234567890123456789L);
    builder.putInt32ToFloatMap(6, 1.5f);
    builder.putInt32ToDoubleMap(6, 1.25);
    builder.putInt32ToBoolMap(7, false);
    builder.putInt32ToStringMap(7, "World");
    builder.putInt32ToBytesMap(8, ByteString.copyFrom(new byte[] {1, 2, 3}));
    builder.putInt32ToMessageMap(8, NestedMessage.newBuilder().setValue(1234).build());
    builder.putInt32ToEnumMap(9, NestedEnum.BAR);
    return builder.build();
  }

  private JsonTestUtil() {}
}
