package org.curioswitch.gradle.documentation.text

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

class TextScanner {

  private String text

  TextScanner(String text) {
    this.text = text
  }

  TextScanner(File file) {
    this.text = file.text
  }

  String getAllAfterLineContaining(String textInStartLine) {
    return getAllBetweenLinesAsserting(
        { it.contains(textInStartLine) },
        { false }
    )
  }

  String getAllBetweenLinesContaining(String textInStartLine, String textInEndLine) {
    return getAllBetweenLinesAsserting(
        { it.contains(textInStartLine) },
        { it.contains(textInEndLine) }
    )
  }

  String getAllAfterLineMatching(String startLineRegex) {
    return getAllBetweenLinesAsserting(
        { it ==~ startLineRegex },
        { false }
    )
  }

  String getAllBetweenLinesMatching(String startLineRegex, String endLineRegex) {
    return getAllBetweenLinesAsserting(
        { it ==~ startLineRegex },
        { it ==~ endLineRegex }
    )
  }

  String getAllAfterLineAsserting(
      @ClosureParams(value = SimpleType, options = ['String']) Closure<Boolean> startLineAssertion) {
    return getAllBetweenLinesAsserting(startLineAssertion, { false })
  }

  String getAllBetweenLinesAsserting(
      @ClosureParams(value = SimpleType, options = ['String']) Closure<Boolean> startLineAssertion,
      @ClosureParams(value = SimpleType, options = ['String']) Closure<Boolean> endLineAssertion) {
    StringBuilder result = new StringBuilder()

    boolean startLineFound = false
    boolean startAndEndLineFound = false
    text.eachLine { String line ->
      if (startAndEndLineFound) {
        // Skipping all iterations after start and end line have been found
        return
      }

      if (startLineFound) {
        if ((startAndEndLineFound = endLineAssertion(line))) {
          return
        }

        // Include the line if it is after the start-line and it isn't itself the end-line
        result.append(line + '\n')
      } else {
        startLineFound = startLineAssertion(line)
      }
    }

    return result.toString()
  }

}
