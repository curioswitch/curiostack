package org.curioswitch.gradle.documentation;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class DocumentationPluginFunctionalTest {

  @Test
  void shouldRun() throws IOException {
    var testDir = new File("build/functionalTest");
    testDir.deleteOnExit();
    testDir.mkdirs();

    String testResourceDirPath = "src/test/resources";

    String buildContent = String.format("" +
            "plugins { id('org.curioswitch.gradle-documentation-plugin') }\n" +
            "documentation { templateFilePath = '%s/template.md' }",
        testResourceDirPath);
    Files.write("".getBytes(StandardCharsets.UTF_8), new File(testDir, "settings.gradle"));
    Files.write(buildContent.getBytes(StandardCharsets.UTF_8), new File(testDir, "build.gradle"));

    String expectedProcessedTemplate = Files.asCharSource(
        new File(testResourceDirPath + "/template_processed.md"), Charsets.UTF_8).read();

    GradleRunner runner = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withProjectDir(testDir);

    runner.withArguments("buildDocumentation").build();
    String processedTemplate = Files.asCharSource(
        new File(testDir, "build/documentation/documentation.md"), Charsets.UTF_8).read();
    Assertions.assertEquals(expectedProcessedTemplate, processedTemplate);
  }

}
