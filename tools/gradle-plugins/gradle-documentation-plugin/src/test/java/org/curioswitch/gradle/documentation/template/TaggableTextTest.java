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
        "\nPragraph after tags\n",
        taggableText.allAfterTaggedLine("d1e").toString());
  }

  @Test
  void allAfterTaggedLineWithRegex() {
    Assertions.assertEquals(
        "\nPragraph after tags\n",
        taggableText.allAfterTaggedLine(".*d1e.*", true).toString());
  }

  @Test
  void allAfterTaggedLineWithoutRegex() {
    Assertions.assertEquals(
        "\nPragraph after tags\n",
        taggableText.allAfterTaggedLine("d1e", false).toString());
  }

  @Test
  void allBetweenTaggedLines() {
    Assertions.assertEquals(
        "Paragrpah\nin tags\n",
        taggableText.allBetweenTaggedLines("d1s", "d1e").toString());
  }

  @Test
  void allBetweenTaggedLinesWithRegex() {
    Assertions.assertEquals(
        "Paragrpah\nin tags\n",
        taggableText.allBetweenTaggedLines(".*d1s.*", ".*d1e.*", true).toString());
  }

  @Test
  void allBetweenTaggedLinesWithoutRegex() {
    Assertions.assertEquals(
        "Paragrpah\nin tags\n",
        taggableText.allBetweenTaggedLines("d1s", "d1e", false).toString());
  }

}
