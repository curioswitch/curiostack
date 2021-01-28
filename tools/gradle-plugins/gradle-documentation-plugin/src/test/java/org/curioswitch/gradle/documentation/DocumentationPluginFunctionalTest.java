package org.curioswitch.gradle.documentation;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class DocumentationPluginFunctionalTest {

  @Test
  void runTasksCorrectly() throws IOException {
    String buildContent = String.format("" +
            "plugins { id('org.curioswitch.gradle-documentation-plugin') }\n" +
            "documentation { templateFilePath = '%s' }",
        Resources.getResource("template.md").getPath());

    var testDir = new File("build/functionalTest");
    testDir.mkdirs();
    Files.touch(new File(testDir, "settings.gradle"));
    Files.write(buildContent.getBytes(StandardCharsets.UTF_8), new File(testDir, "build.gradle"));

    GradleRunner runner = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withProjectDir(testDir);

    runner.withArguments("buildDocumentation").build();
    var documentationFile = new File(testDir, "build/documentation/documentation.md");
    Assertions.assertTrue(documentationFile.isFile());

    String documentationText = Files.asCharSource(documentationFile, Charsets.UTF_8).read();
    String expectedProcessedTemplate = Resources.asCharSource(
        Resources.getResource("template_processed.md"), Charsets.UTF_8).read();
    Assertions.assertEquals(expectedProcessedTemplate, documentationText);

    MoreFiles.deleteRecursively(testDir.toPath());
  }

}
