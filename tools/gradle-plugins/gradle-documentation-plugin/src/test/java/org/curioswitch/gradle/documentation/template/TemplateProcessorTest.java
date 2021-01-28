package org.curioswitch.gradle.documentation.template;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TemplateProcessorTest {

  @Test
  void processTemplate() throws IOException, ClassNotFoundException {
    String processedTemplateText = Resources.asCharSource(
        Resources.getResource("template_processed.md"), Charsets.UTF_8).read();

    var templateProcessor = new TemplateProcessor(
        getClass().getResource("/template.md").getPath());
    Assertions.assertEquals(processedTemplateText, templateProcessor.process());
  }
}
