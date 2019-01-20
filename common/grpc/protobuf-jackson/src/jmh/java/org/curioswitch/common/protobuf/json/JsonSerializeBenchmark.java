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

import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import com.google.protobuf.util.JsonTestProto.TestAllTypes;
import java.nio.charset.StandardCharsets;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

public class JsonSerializeBenchmark {

  private static final Printer PRINTER = JsonFormat.printer();

  private static final TestAllTypes MESSAGE = JsonTestUtil.testAllTypesAllFields();

  private static final MessageMarshaller SERIALIZER =
      MessageMarshaller.builder().register(TestAllTypes.getDefaultInstance()).build();

  @Benchmark
  public void upstreamJson(Blackhole bh) throws Exception {
    bh.consume(PRINTER.print(MESSAGE).getBytes(StandardCharsets.UTF_8));
  }

  @Benchmark
  public void codegenJson(Blackhole bh) throws Exception {
    bh.consume(SERIALIZER.writeValueAsBytes(MESSAGE));
  }

  @Benchmark
  public void toBytes(Blackhole bh) throws Exception {
    bh.consume(MESSAGE.toByteArray());
  }
}
