/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.curioswitch.gradle.plugins.curiostack;

import static org.curioswitch.gradle.testing.assertj.CurioGradleAssertions.assertThat;

import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.curioswitch.gradle.plugins.curiostack.tasks.UpdateIntelliJSdksTask;
import org.curioswitch.gradle.testing.GradleTempDirectories;
import org.curioswitch.gradle.testing.ResourceProjects;
import org.gradle.testkit.runner.GradleRunner;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DisabledIfEnvironmentVariable(named = "CLOUDBUILD_BUILD_ID", matches = ".+")
class CuriostackRootPluginTest {

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class KitchenSink {

    private Path projectDir;

    @BeforeAll
    void copyProject() {
      projectDir =
          ResourceProjects.fromResources("test-projects/gradle-curiostack-plugin/kitchen-sink");
    }

    @Test
    void updatesWrapper() throws Exception {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments("wrapper")
                  .withPluginClasspath())
          .builds()
          .tasksDidSucceed(":wrapper", ":curioUpdateWrapper");

      assertThat(Files.readAllLines(projectDir.resolve("gradlew")))
          .contains(". ./gradle/get-jdk.sh");
      assertThat(projectDir.resolve("gradle/get-jdk.sh"))
          .hasContent(
              Resources.toString(
                  Resources.getResource("gradle-wrapper/rendered-get-jdk.sh"),
                  StandardCharsets.UTF_8));
    }

    @Test
    void pomNotManaged() throws Exception {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":java-library1:generatePomFileForMavenPublication")
                  .withPluginClasspath().withDebug(true))
          .builds()
          .tasksDidSucceed(":java-library1:generatePomFileForMavenPublication");

      var soup =
          Jsoup.parse(
              Files.readString(
                  projectDir.resolve("java-library1/build/publications/maven/pom-default.xml")));
      assertThat(soup.select("dependencyManagement")).isEmpty();
      assertThat(soup.select("dependency"))
          .hasSize(3)
          .allSatisfy(
              dependency -> {
                assertThat(dependency.selectFirst("groupId").text()).isNotEmpty();
                assertThat(dependency.selectFirst("artifactId").text()).isNotEmpty();
                assertThat(dependency.selectFirst("version").text()).isNotEmpty();
              });
    }

    @Test
    // This test is slow since it downloads a file, just run locally for now.
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void terraformInstallsKubectl() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":terraform:terraformInit", "--stacktrace")
                  .withPluginClasspath())
          .builds()
          .tasksDidSucceed(":gcloudInstallComponents", ":terraform:terraformInit");
    }

    @Test
    void terraformConvertsConfigs() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":terraform:terraformConvertConfigs", "--stacktrace")
                  .withPluginClasspath())
          .builds()
          .tasksDidSucceed(":terraform:terraformConvertConfigs");

      assertThat(projectDir.resolve("terraform/build/terraform/dummy.tf.json")).exists();
      assertThat(projectDir.resolve("terraform/build/terraform/dummy2.tf")).exists();
    }

    @Test
    // This test is slow since it runs yarn, just run locally for now.
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void checksResolutions() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":checkNodeResolutions")
                  .withPluginClasspath())
          .builds()
          .tasksDidSucceed(":checkNodeResolutions");
    }

    @Test
    // This test is slow since it runs yarn, just run locally for now.
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void updatesResolutions() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":checkNodeResolutions")
                  .withPluginClasspath())
          .fails()
          .tasksDidRun(":checkNodeResolutions");

      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":updateNodeResolutions")
                  .withPluginClasspath())
          .builds()
          .tasksDidSucceed(":updateNodeResolutions");
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  // This test is slow since it downloads a file, just run locally for now.
  @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
  class SetupOpenJdk8 {

    private Path projectDir;

    @BeforeAll
    void copyProject() {
      projectDir =
          ResourceProjects.fromResources("test-projects/gradle-curiostack-plugin/kitchen-sink");
    }

    @Test
    void downloadsTools() throws Exception {
      Path gradleUserHome = GradleTempDirectories.create("tools-home");
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments("toolsSetupOpenjdk")
                  .withPluginClasspath()
                  .withTestKitDir(gradleUserHome.toFile()))
          .builds()
          .tasksDidSucceed(":toolsSetupOpenjdk");
      assertThat(gradleUserHome.resolve("curiostack/openjdk")).exists();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  // Temporarily disable to investigate build failures.
  @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
  class ConfiguresIntelliJProject {

    private Path projectDir;

    @BeforeAll
    void copyProject() {
      projectDir =
          ResourceProjects.fromResources("test-projects/gradle-curiostack-plugin/kitchen-sink");
    }

    @Test
    void normal() throws Exception {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(
                      "idea", "-xtoolsDownloadOpenjdk", "-x" + UpdateIntelliJSdksTask.NAME)
                  .withPluginClasspath())
          .builds()
          .tasksDidSucceed(":idea");

      final Path projectFile;
      try (var s = Files.list(projectDir)) {
        projectFile = s.filter(p -> p.toString().endsWith(".ipr")).findFirst().get();
      }

      // Make sure copyright is escaped correctly.
      assertThat(Files.readString(projectFile))
          .contains("<option name=\"notice\" value=\"MIT License&#10;&#10;Copyright");
    }
  }
}
