package org.curioswitch.gradle.documentation.template;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class TemplateProcessorTest {

  @Test
  void shouldProcessTemplate() throws IOException, ClassNotFoundException {
    var processedTemplateFile = new File(getClass().getResource("/template_processed.md").getFile());
    String processedTemplateText = Files.asCharSource(processedTemplateFile, Charsets.UTF_8).read();

    var templateProcessor = new TemplateProcessor(
        getClass().getResource("/template.md").getPath());
    Assertions.assertEquals(processedTemplateText, templateProcessor.process());
  }
}
