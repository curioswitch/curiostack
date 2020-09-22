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

package org.curioswitch.scrapers.instagram.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import javax.inject.Inject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class SharedDataExtractor {

  private final ObjectMapper objectMapper;

  @Inject
  public SharedDataExtractor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public <T> T extractSharedData(AggregatedHttpResponse page, Class<T> dataType) {
    final InputStream contentStream;
    if (page.content() instanceof ByteBufHolder) {
      contentStream = new ByteBufInputStream(((ByteBufHolder) page.content()).content(), true);
    } else {
      contentStream = new ByteArrayInputStream(page.content().array());
    }

    final Document doc;
    try (contentStream) {
      doc = Jsoup.parse(contentStream, null, "https://www.instagram.com/");
    } catch (IOException e) {
      throw new UncheckedIOException("IOException from memory buffer, can't happen.", e);
    }

    var scripts = doc.getElementsByTag("script");
    return scripts.stream()
        .filter(script -> script.html().trim().startsWith("window._sharedData = "))
        .findAny()
        .map(
            element -> {
              String json = element.html().substring("window._sharedData = ".length());
              try {
                return objectMapper.readValue(json, dataType);
              } catch (IOException e) {
                throw new UncheckedIOException("Could not parse JSON.", e);
              }
            })
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No shared data on page, is this a valid Instagram page? page: " + doc));
  }
}
