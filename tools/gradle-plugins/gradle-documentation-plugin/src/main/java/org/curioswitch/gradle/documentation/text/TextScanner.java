package org.curioswitch.gradle.documentation.text;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

public class TextScanner {
  private final String text;

  public TextScanner(String text) {
    this.text = text;
  }

  public TextScanner(File file) throws IOException {
    this.text = Files.asCharSource(file, Charsets.UTF_8).read();
  }

  public String getAllAfterLineContaining(String textInStartLine) {
    return getAllBetweenLinesAsserting(
        line -> line.contains(textInStartLine),
        line -> false
    );
  }

  public String getAllBetweenLinesContaining(String textInStartLine, String textInEndLine) {
    return getAllBetweenLinesAsserting(
        line -> line.contains(textInStartLine),
        line -> line.contains(textInEndLine)
    );
  }

  public String getAllAfterLineMatching(String startLineRegex) {
    return getAllBetweenLinesAsserting(
        line -> line.matches(startLineRegex),
        line -> false
    );
  }

  public String getAllBetweenLinesMatching(String startLineRegex, String endLineRegex) {
    return getAllBetweenLinesAsserting(
        line -> line.matches(startLineRegex),
        line -> line.matches(endLineRegex)
    );
  }

  public String getAllAfterLineAsserting(Function<String, Boolean> startLineAssertion) {
    return getAllBetweenLinesAsserting(startLineAssertion, line -> false);
  }

  public String getAllBetweenLinesAsserting(
      Function<String, Boolean> startLineAssertion, Function<String, Boolean> endLineAssertion) {
    StringBuilder result = new StringBuilder();

    var context = new Object() {
      boolean startLineFound = false;
      boolean startAndEndLineFound = false;
    };
    text.lines().forEach(line -> {
      if (context.startAndEndLineFound) {
        // Skipping all iterations after start and end line have been found
        return;
      }

      if (context.startLineFound) {
        if ((context.startAndEndLineFound = endLineAssertion.apply(line))) {
          return;
        }

        // Include the line if it is after the start-line and it isn't itself the end-line
        result.append(line).append('\n');
      } else {
        context.startLineFound = startLineAssertion.apply(line);
      }
    });

    return result.toString();
  }

}
