/**
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

import com.google.common.io.Resources;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.Printer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.curioswitch.common.protobuf.json.GithubApi.SearchResponse;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

public class GithubApiBenchmark {

  private static final MessageMarshaller MARSHALLER =
      MessageMarshaller.builder()
          .register(SearchResponse.getDefaultInstance())
          .omittingInsignificantWhitespace(true)
          .preservingProtoFieldNames(true)
          .build();

  private static final Parser UPSTREAM_PARSER = JsonFormat.parser();
  private static final Printer UPSTREAM_PRINTER =
      JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace();

  private static final String SEARCH_RESPONSE_JSON;
  private static final SearchResponse SEARCH_RESPONSE;

  private static final byte[] SEARCH_RESPONSE_PROTOBUF_BINARY;

  static {
    try {
      SEARCH_RESPONSE_JSON =
          Resources.toString(
              Resources.getResource("github_search_response.json"), StandardCharsets.UTF_8);

      SearchResponse.Builder builder = SearchResponse.newBuilder();
      UPSTREAM_PARSER.merge(SEARCH_RESPONSE_JSON, builder);
      SEARCH_RESPONSE = builder.build();
      SEARCH_RESPONSE_PROTOBUF_BINARY = SEARCH_RESPONSE.toByteArray();
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  @Benchmark
  public void marshallerParseString(Blackhole bh) throws Exception {
    SearchResponse.Builder builder = SearchResponse.newBuilder();
    MARSHALLER.mergeValue(SEARCH_RESPONSE_JSON, builder);
    bh.consume(builder);
  }

  @Benchmark
  public void marshallerWriteString(Blackhole bh) throws Exception {
    bh.consume(MARSHALLER.writeValueAsString(SEARCH_RESPONSE));
  }

  @Benchmark
  public void upstreamParseString(Blackhole bh) throws Exception {
    SearchResponse.Builder builder = SearchResponse.newBuilder();
    UPSTREAM_PARSER.merge(SEARCH_RESPONSE_JSON, builder);
    bh.consume(builder);
  }

  @Benchmark
  public void upstreamWriteString(Blackhole bh) throws Exception {
    bh.consume(UPSTREAM_PRINTER.print(SEARCH_RESPONSE));
  }

  @Benchmark
  public void protobufParseBytes(Blackhole bh) throws Exception {
    SearchResponse.Builder builder = SearchResponse.newBuilder();
    builder.mergeFrom(SEARCH_RESPONSE_PROTOBUF_BINARY);
    bh.consume(builder);
  }

  @Benchmark
  public void protobufToBytes(Blackhole bh) throws Exception {
    bh.consume(SEARCH_RESPONSE.toByteArray());
  }
}
