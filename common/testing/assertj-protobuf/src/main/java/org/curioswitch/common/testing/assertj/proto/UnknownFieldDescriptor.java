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

// Includes work from:
/*
 * Copyright (c) 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.curioswitch.common.testing.assertj.proto;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.WireFormat;
import java.util.List;

/** Convenience class encapsulating type information for unknown fields. */
@AutoValue
abstract class UnknownFieldDescriptor {

  enum Type {
    VARINT(WireFormat.WIRETYPE_VARINT) {
      @Override
      public List<?> getValues(UnknownFieldSet.Field field) {
        return field.getVarintList();
      }
    },
    FIXED32(WireFormat.WIRETYPE_FIXED32) {
      @Override
      public List<?> getValues(UnknownFieldSet.Field field) {
        return field.getFixed32List();
      }
    },
    FIXED64(WireFormat.WIRETYPE_FIXED64) {
      @Override
      public List<?> getValues(UnknownFieldSet.Field field) {
        return field.getFixed64List();
      }
    },
    LENGTH_DELIMITED(WireFormat.WIRETYPE_LENGTH_DELIMITED) {
      @Override
      public List<?> getValues(UnknownFieldSet.Field field) {
        return field.getLengthDelimitedList();
      }
    },
    GROUP(WireFormat.WIRETYPE_START_GROUP) {
      @Override
      public List<?> getValues(UnknownFieldSet.Field field) {
        return field.getGroupList();
      }
    };

    private static final ImmutableList<Type> TYPES = ImmutableList.copyOf(values());

    static ImmutableList<Type> all() {
      return TYPES;
    }

    private final int wireType;

    Type(int wireType) {
      this.wireType = wireType;
    }

    /** Returns the corresponding values from the given field. */
    abstract List<?> getValues(UnknownFieldSet.Field field);

    /** Returns the {@link WireFormat} constant for this field type. */
    final int wireType() {
      return wireType;
    }
  }

  static UnknownFieldDescriptor create(int fieldNumber, Type type) {
    return new AutoValue_UnknownFieldDescriptor(fieldNumber, type);
  }

  abstract int fieldNumber();

  abstract Type type();

  static ImmutableList<UnknownFieldDescriptor> descriptors(
      int fieldNumber, UnknownFieldSet.Field field) {
    ImmutableList.Builder<UnknownFieldDescriptor> builder = ImmutableList.builder();
    for (Type type : Type.all()) {
      if (!type.getValues(field).isEmpty()) {
        builder.add(create(fieldNumber, type));
      }
    }
    return builder.build();
  }
}
