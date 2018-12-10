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
package org.curioswitch.common.server.framework.mapper;

import com.google.auto.service.AutoService;
import com.google.protobuf.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.curioswitch.common.server.framework.util.ProtoDurations;
import org.curioswitch.common.server.framework.util.ProtoTimestamps;
import org.simpleflatmapper.converter.AbstractConverterFactoryProducer;
import org.simpleflatmapper.converter.ConverterFactory;
import org.simpleflatmapper.converter.ConverterFactoryProducer;
import org.simpleflatmapper.util.Consumer;

/**
 * {@link AbstractConverterFactoryProducer} to convert Java8 {@link LocalDateTime} and {@link
 * java.sql.Timestamp} to {@link Timestamp}.
 */
@AutoService(ConverterFactoryProducer.class)
public class ProtobufConverterFactoryProducer extends AbstractConverterFactoryProducer {

  @Override
  public void produce(Consumer<? super ConverterFactory<?, ?>> consumer) {
    constantConverter(
        consumer,
        java.sql.Timestamp.class,
        Timestamp.class,
        t -> {
          if (t == null) {
            return null;
          }
          return ProtoTimestamps.fromLocalDateTime(t.toLocalDateTime(), ZoneOffset.UTC);
        });
    constantConverter(
        consumer,
        LocalDateTime.class,
        Timestamp.class,
        ldt -> {
          if (ldt == null) {
            return null;
          }
          return ProtoTimestamps.fromLocalDateTime(ldt, ZoneOffset.UTC);
        });
    constantConverter(
        consumer,
        Instant.class,
        Timestamp.class,
        i -> {
          if (i == null) {
            return null;
          }
          return ProtoTimestamps.fromInstant(i);
        });

    constantConverter(
        consumer,
        Duration.class,
        com.google.protobuf.Duration.class,
        d -> {
          if (d == null) {
            return null;
          }
          return ProtoDurations.fromJavaTime(d);
        });
  }
}
