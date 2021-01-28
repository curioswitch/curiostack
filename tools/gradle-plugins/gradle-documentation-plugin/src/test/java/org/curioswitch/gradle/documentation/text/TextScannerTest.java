package org.curioswitch.gradle.documentation.text;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
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
  void shouldParseTextCorrectly() throws IOException {
    var textWithTagsFile = new File(getClass().getResource("/docs/text_with_tags.md").getFile());
    String textWithTags = Files.asCharSource(textWithTagsFile, Charsets.UTF_8).read();
    var scanner = new TextScanner(textWithTags);

    scannerMethodCallsWithExpectedOutputs.forEach(pair ->
        Assertions.assertEquals(pair.getValue1(), pair.getValue0().apply(scanner)));
  }

  @Test
  void shouldLoadTextFromFile() throws IOException, NoSuchFieldException, IllegalAccessException {
    var file = new File(getClass().getResource("/docs/text_with_tags.md").getFile());
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
        "Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineContaining("d1e"),
        "\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineContaining("wrong_tag"),
        ""));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesContaining("d1s", "d1e"),
        "Paragrpah\nin tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesContaining("d1s", "wrong_tag"),
        "Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesContaining("d1e", "wrong_tag"),
        "\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesContaining("wrong_tag", "d1e"),
        ""));
    result.add(Pair.with(
        s -> s.getAllAfterLineMatching(".*d1s.*"),
        "Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineMatching(".*d1e.*"),
        "\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineMatching("d1s"),
        ""));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesMatching(".*d1s.*", ".*d1e.*"),
        "Paragrpah\nin tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesMatching(".*d1s.*", ".*wrong_tag.*"),
        "Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesMatching(".*d1s.*", "d1e"),
        "Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesMatching(".*d1e.*", "wrong_tag"),
        "\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllAfterLineAsserting(l -> l.contains("d1s")),
        "Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n"));
    result.add(Pair.with(
        s -> s.getAllBetweenLinesAsserting(l -> l.contains("d1s"), l -> l.contains("d1e")),
        "Paragrpah\nin tags\n"));

    return result;
  }

}
