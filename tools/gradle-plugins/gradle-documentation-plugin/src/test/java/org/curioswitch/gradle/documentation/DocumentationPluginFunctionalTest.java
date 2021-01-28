package org.curioswitch.gradle.documentation;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import org.curioswitch.gradle.testing.assertj.CurioGradleAssertions;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

class DocumentationPluginFunctionalTest {

  private final File testDir = new File("build/functionalTest");
  private static final String MIN_BUILD_GRADLE =
      "plugins { id('org.curioswitch.gradle-documentation-plugin') }";
  private final GradleRunner runner = GradleRunner.create()
      .forwardOutput()
      .withPluginClasspath()
      .withProjectDir(testDir);

  @BeforeEach
  void init() throws IOException {
    testDir.mkdirs();
    Files.touch(new File(testDir, "settings.gradle"));
  }

  @AfterEach
  void cleanup() throws IOException {
    MoreFiles.deleteRecursively(testDir.toPath());
  }

  @Test
  void buildDocumentationWithDefaultTemplate() throws IOException {
    List.of("template.md", "docs/text_with_tags.md").forEach(relativePath -> {
      File source = new File(Resources.getResource(relativePath).getFile());
      File target = new File(testDir, relativePath.replace("template", "documentation"));
      try {
        target.getParentFile().mkdirs();
        Files.copy(source, target);
      } catch (IOException e) { throw new RuntimeException(e); }
    });

    Files.write(MIN_BUILD_GRADLE.getBytes(StandardCharsets.UTF_8), new File(testDir, "build.gradle"));

    runner.withArguments("buildDocumentation").build();

    var builtDocsFile = new File(testDir, "build/documentation/documentation.md");
    Assertions.assertTrue(builtDocsFile.isFile());

    String builtDocsText = Files.asCharSource(builtDocsFile, Charsets.UTF_8).read();
    String expectedProcessedTemplate = Resources.asCharSource(
        Resources.getResource("template_processed.md"), Charsets.UTF_8).read();
    Assertions.assertEquals(expectedProcessedTemplate, builtDocsText);
  }

  @Test
  void buildDocumentationWithMissingDefaultTemplate() throws IOException {
    Files.write(MIN_BUILD_GRADLE.getBytes(StandardCharsets.UTF_8), new File(testDir, "build.gradle"));

    CurioGradleAssertions
        .assertThat(runner.withArguments("buildDocumentation"))
        .fails()
        .outputContains("Cannot find file " + testDir.getAbsolutePath() + "/documentation.md");
  }

  @Test
  void buildDocumentationWithCustomTemplateFilePath() throws IOException {
    String buildContent = String.format(MIN_BUILD_GRADLE +
            "\ndocumentation { templateFilePath = '%s' }",
        Resources.getResource("template.md").getPath());
    Files.write(buildContent.getBytes(StandardCharsets.UTF_8), new File(testDir, "build.gradle"));

    runner.withArguments("buildDocumentation").build();

    var builtDocsFile = new File(testDir, "build/documentation/documentation.md");
    Assertions.assertTrue(builtDocsFile.isFile());

    String builtDocsText = Files.asCharSource(builtDocsFile, Charsets.UTF_8).read();
    String expectedProcessedTemplate = Resources.asCharSource(
        Resources.getResource("template_processed.md"), Charsets.UTF_8).read();
    Assertions.assertEquals(expectedProcessedTemplate, builtDocsText);
  }

}
