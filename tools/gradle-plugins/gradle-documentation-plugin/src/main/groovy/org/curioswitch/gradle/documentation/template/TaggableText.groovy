package org.curioswitch.gradle.documentation.template

import org.curioswitch.gradle.documentation.text.TextScanner

class TaggableText {
  private String text

  TaggableText(String text) {
    this.text = text
  }

  TaggableText allAfterTaggedLine(String tag, useRegex = false) {
    def scanner = new TextScanner(text)
    this.text = useRegex ?
        scanner.getAllAfterLineMatching(tag) :
        scanner.getAllAfterLineContaining(tag)
    return this
  }

  TaggableText allBetweenTaggedLines(String startTag, String endTag, useRegex = false) {
    def scanner = new TextScanner(text)
    this.text = useRegex ?
        scanner.getAllBetweenLinesMatching(startTag, endTag) :
        scanner.getAllBetweenLinesContaining(startTag, endTag)
    return this
  }

  @Override
  String toString() {
    return text
  }
}
