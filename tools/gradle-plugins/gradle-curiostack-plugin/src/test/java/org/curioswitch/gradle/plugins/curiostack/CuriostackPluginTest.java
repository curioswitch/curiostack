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
import org.curioswitch.gradle.testing.GradleTempDirectories;
import org.curioswitch.gradle.testing.ResourceProjects;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

class CuriostackPluginTest {

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class WrapperUpdate {

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
                  Resources.getResource("curiostack/get-jdk.sh"), StandardCharsets.UTF_8));
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
                  .withArguments("toolsSetupOpenjdk8")
                  .withPluginClasspath()
                  .withTestKitDir(gradleUserHome.toFile()))
          .builds()
          .tasksDidSucceed(":toolsSetupOpenjdk8");
      assertThat(gradleUserHome.resolve("curiostack/openjdk8")).exists();
    }
  }
}
