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
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import org.curioswitch.gradle.testing.GradleTempDirectories;
import org.curioswitch.gradle.testing.ResourceProjects;
import org.gradle.api.Project;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.mockito.Mock;

// This test is slow since it downloads a file, just run locally for now.
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class CuriostackRootPluginToolsTest {

  // A default mock will always return null for findProperty which makes our tests independent of
  // default versions.
  @Mock private Project project;

  @BeforeEach
  void setUp() {
    when(project.getRootProject()).thenReturn(project);
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class DefaultVersion {

    private File projectDir;

    @BeforeAll
    void copyProject() {
      projectDir =
          ResourceProjects.fromResources("test-projects/gradle-curiostack-plugin/tools/claat")
              .toFile();
    }

    @Test
    void normal() {
      Path gradleUserHome = GradleTempDirectories.create("tools-home");
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("toolsSetupClaat")
                  .withPluginClasspath()
                  .withTestKitDir(gradleUserHome.toFile()))
          .builds()
          .tasksDidSucceed(":toolsDownloadClaat", ":toolsSetupClaat");

      assertToolDirExists(gradleUserHome, "claat", ToolDependencies.getClaatVersion(project));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class OverriddenVersion {

    private File projectDir;

    @BeforeAll
    void copyProject() {
      projectDir =
          ResourceProjects.fromResources(
                  "test-projects/gradle-curiostack-plugin/tools/claat-override-version")
              .toFile();
    }

    @Test
    void normal() {
      Path gradleUserHome = GradleTempDirectories.create("tools-home");
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("toolsSetupClaat")
                  .withPluginClasspath()
                  .withTestKitDir(gradleUserHome.toFile()))
          .builds()
          .tasksDidSucceed(":toolsDownloadClaat", ":toolsSetupClaat");

      assertThat(ToolDependencies.getClaatVersion(project)).isNotEqualTo("1.0.4");
      assertToolDirExists(gradleUserHome, "claat", "1.0.4");
    }
  }

  private static void assertToolDirExists(Path gradleUserHome, String tool, String version) {
    assertThat(gradleUserHome.resolve("curiostack").resolve(tool).resolve(version)).exists();
  }
}
