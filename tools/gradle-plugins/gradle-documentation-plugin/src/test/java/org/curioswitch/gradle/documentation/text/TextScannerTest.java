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
package org.curioswitch.gradle.documentation.text;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.javatuples.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class TextScannerTest {

  private final List<Pair<Function<TextScanner, String>, String>>
      scannerMethodCallsWithExpectedOutputs = populateSMCWEO();

  @Test
  void parseTextCorrectly() throws IOException {
    String textWithTags = Resources.asCharSource(
        Resources.getResource("docs/text_with_tags.md"), Charsets.UTF_8).read();
    var scanner = new TextScanner(textWithTags);

    scannerMethodCallsWithExpectedOutputs.forEach(pair ->
        Assertions.assertEquals(pair.getValue1(), pair.getValue0().apply(scanner)));
  }

  @Test
  void loadTextFromFile() throws IOException, NoSuchFieldException, IllegalAccessException {
    var file = new File(Resources.getResource("docs/text_with_tags.md").getFile());
    String fileContent = Files.asCharSource(file, Charsets.UTF_8).read();
    var textScanner = new TextScanner(file);

    Field textField = TextScanner.class.getDeclaredField("text");
    textField.setAccessible(true);
    Assertions.assertEquals(fileContent, textField.get(textScanner));
  }

  private static List<Pair<Function<TextScanner, String>, String>> populateSMCWEO() {
    List<Pair<Function<TextScanner, String>, String>> result = new ArrayList<>();
    result.add(Pair.with(
        s -> s.getAllAfterLineContaining("d1s"),
        "Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineContaining("d1e"),
        "\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineContaining("wrong_tag"),
        ""));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesContaining("d1s", "d1e"),
        "Paragraph\nin tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesContaining("d1s", "wrong_tag"),
        "Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesContaining("d1e", "wrong_tag"),
        "\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesContaining("wrong_tag", "d1e"),
        ""));
    result.add(Pair.with(
        s -> s.getAllAfterLineMatching(".*d1s.*"),
        "Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineMatching(".*d1e.*"),
        "\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineMatching("d1s"),
        ""));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesMatching(".*d1s.*", ".*d1e.*"),
        "Paragraph\nin tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesMatching(".*d1s.*", ".*wrong_tag.*"),
        "Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesMatching(".*d1s.*", "d1e"),
        "Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesMatching(".*d1e.*", "wrong_tag"),
        "\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineAsserting(l -> l.contains("d1s")),
        "Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesAsserting(l -> l.contains("d1s"), l -> l.contains("d1e")),
        "Paragraph\nin tags\n"));

    return result;
  }

}
