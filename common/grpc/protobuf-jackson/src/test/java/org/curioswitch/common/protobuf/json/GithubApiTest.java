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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.Printer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.curioswitch.common.protobuf.json.GithubApi.SearchResponse;
import org.junit.Test;

public class GithubApiTest {

  private static final Parser UPSTREAM_PARSER = JsonFormat.parser();
  private static final Printer UPSTREAM_PRINTER =
      JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace();

  private static final byte[] SEARCH_RESPONSE_JSON;
  private static final SearchResponse SEARCH_RESPONSE;

  static {
    try {
      SEARCH_RESPONSE_JSON =
          Resources.toByteArray(Resources.getResource("github_search_response.json"));

      SearchResponse.Builder builder = SearchResponse.newBuilder();
      UPSTREAM_PARSER.merge(new String(SEARCH_RESPONSE_JSON, StandardCharsets.UTF_8), builder);
      SEARCH_RESPONSE = builder.build();
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  @Test
  public void parse() {
    assertThat(SEARCH_RESPONSE).isNotNull();
  }
}
