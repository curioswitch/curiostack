/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
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
package org.curioswitch.gradle.documentation.template;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class TaggableTextTest {

  private final String textWithTags;
  private TaggableText taggableText;

  TaggableTextTest() throws IOException {
    textWithTags = Resources.asCharSource(
        Resources.getResource("docs/text_with_tags.md"), Charsets.UTF_8).read();
  }

  @BeforeEach
  void init() throws IOException {
    taggableText = new TaggableText(textWithTags);
  }

  @Test
  void allAfterTaggedLine() {
    Assertions.assertEquals(
        "\nParagraph after tags\n",
        taggableText.allAfterTaggedLine("d1e").toString());
  }

  @Test
  void allAfterTaggedLineWithRegex() {
    Assertions.assertEquals(
        "\nParagraph after tags\n",
        taggableText.allAfterTaggedLine(".*d1e.*", true).toString());
  }

  @Test
  void allAfterTaggedLineWithoutRegex() {
    Assertions.assertEquals(
        "\nParagraph after tags\n",
        taggableText.allAfterTaggedLine("d1e", false).toString());
  }

  @Test
  void allBetweenTaggedLines() {
    Assertions.assertEquals(
        "Paragraph\nin tags\n",
        taggableText.allBetweenTaggedLines("d1s", "d1e").toString());
  }

  @Test
  void allBetweenTaggedLinesWithRegex() {
    Assertions.assertEquals(
        "Paragraph\nin tags\n",
        taggableText.allBetweenTaggedLines(".*d1s.*", ".*d1e.*", true).toString());
  }

  @Test
  void allBetweenTaggedLinesWithoutRegex() {
    Assertions.assertEquals(
        "Paragraph\nin tags\n",
        taggableText.allBetweenTaggedLines("d1s", "d1e", false).toString());
  }

}
