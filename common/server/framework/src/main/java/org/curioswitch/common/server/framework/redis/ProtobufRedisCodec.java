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
package org.curioswitch.common.server.framework.redis;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.ToByteBufEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

class ProtobufRedisCodec<K extends Message, V extends Message>
    implements RedisCodec<K, V>, ToByteBufEncoder<K, V> {

  private final byte[] keyPrefix;
  private final K keyPrototype;
  private final V valuePrototype;

  ProtobufRedisCodec(byte[] keyPrefix, K keyPrototype, V valuePrototype) {
    this.keyPrefix = keyPrefix;
    this.keyPrototype = keyPrototype;
    this.valuePrototype = valuePrototype;
  }

  @Override
  public K decodeKey(ByteBuffer bytes) {
    return decode(bytes, keyPrototype);
  }

  @Override
  public V decodeValue(ByteBuffer bytes) {
    return decode(bytes, valuePrototype);
  }

  @Override
  public ByteBuffer encodeKey(K key) {
    ByteBuf buf = Unpooled.buffer(estimateSize(key));
    encodeKey(key, buf);
    return buf.nioBuffer();
  }

  @Override
  public void encodeKey(K key, ByteBuf target) {
    target.writeBytes(keyPrefix);
    encodeTo(key, target);
    target.writerIndex(key.getSerializedSize() + keyPrefix.length);
  }

  @Override
  public ByteBuffer encodeValue(V value) {
    ByteBuf buf = Unpooled.buffer(estimateSize(value));
    encodeValue(value, buf);
    return buf.nioBuffer();
  }

  @Override
  public void encodeValue(V value, ByteBuf target) {
    encodeTo(value, target);
    target.writerIndex(value.getSerializedSize());
  }

  @Override
  public int estimateSize(Object keyOrValue) {
    // Will overestimate for values which don't need the prefix, but can't help it.
    return ((Message) keyOrValue).getSerializedSize() + keyPrefix.length;
  }

  private static void encodeTo(Message message, ByteBuf target) {
    try {
      message.writeTo(
          CodedOutputStream.newInstance(
              target.nioBuffer(target.writerIndex(), target.writableBytes())));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not encode message.", e);
    }
  }

  private static <T extends Message> T decode(ByteBuffer bytes, T prototype) {
    Message.Builder builder = prototype.newBuilderForType();
    try {
      builder.mergeFrom(CodedInputStream.newInstance(bytes));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not decode message.", e);
    }
    @SuppressWarnings("unchecked") // T.newBuilderForType().build() returns T
    T built = (T) builder.build();
    return built;
  }
}
