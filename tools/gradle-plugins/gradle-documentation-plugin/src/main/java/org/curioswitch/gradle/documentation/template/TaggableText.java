package org.curioswitch.gradle.documentation.template;

import org.curioswitch.gradle.documentation.text.TextScanner;

class TaggableText {
  private String text;

  public TaggableText(String text) {
    this.text = text;
  }

  public TaggableText allAfterTaggedLine(String tag) {
    return allAfterTaggedLine(tag, false);
  }

  public TaggableText allAfterTaggedLine(String tag, boolean useRegex) {
    var scanner = new TextScanner(text);
    this.text = useRegex
        ? scanner.getAllAfterLineMatching(tag)
        : scanner.getAllAfterLineContaining(tag);
    return this;
  }

  public TaggableText allBetweenTaggedLines(String startTag, String endTag) {
    return allBetweenTaggedLines(startTag, endTag, false);
  }

  public TaggableText allBetweenTaggedLines(String startTag, String endTag, boolean useRegex) {
    var scanner = new TextScanner(text);
    this.text = useRegex
        ? scanner.getAllBetweenLinesMatching(startTag, endTag)
        : scanner.getAllBetweenLinesContaining(startTag, endTag);
    return this;
  }

  @Override
  public String toString() {
    return text;
  }
}
