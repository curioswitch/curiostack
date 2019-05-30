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

package org.curioswitch.gradle.plugins.ci;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.eclipse.jgit.api.Git;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CurioGenericCiPluginTest {

  private static final Logger logger = LogManager.getLogger();

  @BeforeAll
  void warnOnWindows() {
    if (new PlatformHelper().getOs() == OperatingSystem.WINDOWS) {
      logger.warn(
          "jgit does not clean up properly on Windows, so tests will fail with "
              + "executionError. It's ok to ignore this.");
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchNoDiffs {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void noCi() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild", "--stacktrace")
              .withEnvironment(ImmutableMap.of())
              .withPluginClasspath()
              .buildAndFail();

      assertThat(result.getOutput()).contains("Task 'continuousBuild' not found in root project");
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
    }

    @Test
    void ciIsMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }

    @SuppressWarnings("ClassCanBeStatic")
    @Nested
    class ReleaseBuild {
      @Test
      void implicitTag() {
        var result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("releaseBuild")
                .withEnvironment(
                    ImmutableMap.of("CI", "true", "TAG_NAME", "RELEASE_SERVER1_20190522"))
                .withPluginClasspath()
                .build();

        assertThat(result.task(":library1:jar")).isNull();
        assertThat(result.task(":library1:build")).isNull();
        assertThat(result.task(":library2:jar")).isNull();
        assertThat(result.task(":library2:build")).isNull();
        assertThat(result.task(":server1:build")).isNotNull();
        assertThat(result.task(":server1:jib")).isNotNull();
        assertThat(result.task(":server1:deployAlpha")).isNull();
        assertThat(result.task(":server2:build")).isNull();
        assertThat(result.task(":server2:jib")).isNull();
        assertThat(result.task(":server1:deployAlpha")).isNull();
      }

      @Test
      void explicitTag() {
        var result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("releaseBuild")
                .withEnvironment(
                    ImmutableMap.of("CI", "true", "TAG_NAME", "RELEASE_CUSTOM_TAG_20190522"))
                .withPluginClasspath()
                .build();

        assertThat(result.task(":library1:jar")).isNotNull();
        assertThat(result.task(":library1:build")).isNull();
        assertThat(result.task(":library2:jar")).isNull();
        assertThat(result.task(":library2:build")).isNull();
        assertThat(result.task(":server1:build")).isNull();
        assertThat(result.task(":server1:jib")).isNull();
        assertThat(result.task(":server1:deployAlpha")).isNull();
        assertThat(result.task(":server2:build")).isNotNull();
        assertThat(result.task(":server2:jib")).isNotNull();
        assertThat(result.task(":server2:deployAlpha")).isNull();
      }

      @Test
      void unknownTag() {
        var result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("releaseBuild")
                .withEnvironment(
                    ImmutableMap.of("CI", "true", "TAG_NAME", "RELEASE_UNKNOWN1_20190522"))
                .withPluginClasspath()
                .buildAndFail();

        assertThat(result.getOutput()).contains("Task 'releaseBuild' not found in root project");
      }
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchLibrary1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      addDiffs(projectDir, "library1/src/main/java/Library1.java");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void noCi() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild", "--stacktrace")
              .withEnvironment(ImmutableMap.of())
              .withPluginClasspath()
              .buildAndFail();

      assertThat(result.getOutput()).contains("Task 'continuousBuild' not found in root project");
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNotNull();
      assertThat(result.task(":library1:build")).isNotNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNotNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }

    @Test
    void ciIsMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNotNull();
      assertThat(result.task(":library1:build")).isNotNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNotNull();
      assertThat(result.task(":server2:jib")).isNotNull();
      assertThat(result.task(":server2:deployAlpha")).isNotNull();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchLibrary2Diff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      addDiffs(projectDir, "library2/src/main/java/Library2.java");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNotNull();
      assertThat(result.task(":library2:build")).isNotNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }

    @Test
    void ciIsMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNotNull();
      assertThat(result.task(":library2:build")).isNotNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchServer1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      addDiffs(projectDir, "server1/src/main/java/Server1.java");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNotNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }

    @Test
    void ciIsMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNotNull();
      assertThat(result.task(":server1:jib")).isNotNull();
      assertThat(result.task(":server1:deployAlpha")).isNotNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchServer2Diff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      addDiffs(projectDir, "server2/src/main/java/Server2.java");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNotNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNotNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }

    @Test
    void ciIsMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNotNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNotNull();
      assertThat(result.task(":server2:jib")).isNotNull();
      assertThat(result.task(":server2:deployAlpha")).isNotNull();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchTopLevelDiff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      addDiffs(projectDir, "build.gradle.kts");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNotNull();
      assertThat(result.task(":library1:build")).isNotNull();
      assertThat(result.task(":library2:jar")).isNotNull();
      assertThat(result.task(":library2:build")).isNotNull();
      assertThat(result.task(":server1:build")).isNotNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNotNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }

    @Test
    void ciIsMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNotNull();
      assertThat(result.task(":library1:build")).isNotNull();
      assertThat(result.task(":library2:jar")).isNotNull();
      assertThat(result.task(":library2:build")).isNotNull();
      assertThat(result.task(":server1:build")).isNotNull();
      assertThat(result.task(":server1:jib")).isNotNull();
      assertThat(result.task(":server1:deployAlpha")).isNotNull();
      assertThat(result.task(":server2:build")).isNotNull();
      assertThat(result.task(":server2:jib")).isNotNull();
      assertThat(result.task(":server2:deployAlpha")).isNotNull();
    }
  }

  // We go ahead and verify a master branch with the same state as the pr branch has different build
  // behavior.
  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchTopLevelDiffFollowedByTwoEmptyCommits {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      addDiffs(projectDir, "build.gradle.kts");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
    }

    @Test
    void ciIsMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchLibrary1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "library1/src/main/java/Library1.java");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void noCi() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild", "--stacktrace")
              .withEnvironment(ImmutableMap.of())
              .withPluginClasspath()
              .buildAndFail();

      assertThat(result.getOutput()).contains("Task 'continuousBuild' not found in root project");
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNotNull();
      assertThat(result.task(":library1:build")).isNotNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNotNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchLibrary2Diff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "library2/src/main/java/Library2.java");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNotNull();
      assertThat(result.task(":library2:build")).isNotNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchServer1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "server1/src/main/java/Server1.java");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNotNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchServer2Diff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "server2/src/main/java/Server2.java");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNotNull();
      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":library2:jar")).isNull();
      assertThat(result.task(":library2:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNotNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchTopLevelDiff {

    private File projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      copyProjectFromResources(
          "test-projects/gradle-curio-generic-ci-plugin/master-no-diffs", projectDir);

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "build.gradle.kts");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:jar")).isNotNull();
      assertThat(result.task(":library1:build")).isNotNull();
      assertThat(result.task(":library2:jar")).isNotNull();
      assertThat(result.task(":library2:build")).isNotNull();
      assertThat(result.task(":server1:build")).isNotNull();
      assertThat(result.task(":server1:jib")).isNull();
      assertThat(result.task(":server1:deployAlpha")).isNull();
      assertThat(result.task(":server2:build")).isNotNull();
      assertThat(result.task(":server2:jib")).isNull();
      assertThat(result.task(":server2:deployAlpha")).isNull();
    }
  }

  private static void copyProjectFromResources(String resourcePath, Path projectDir)
      throws Exception {
    var resourceDir = Paths.get(Resources.getResource(resourcePath).toURI());
    try (var stream = Files.walk(resourceDir)) {
      stream
          .filter(path -> !path.equals(resourceDir))
          .forEach(
              path -> {
                var destPath = projectDir.resolve(resourceDir.relativize(path));
                try {
                  if (Files.isDirectory(path)) {
                    Files.createDirectory(destPath);
                  } else {
                    Files.copy(path, destPath);
                  }
                } catch (IOException e) {
                  throw new UncheckedIOException("Could not copy test project file.", e);
                }
              });
    }

    var projectFiles = projectDir.toFile().listFiles();
    checkNotNull(projectFiles);
    for (var file : projectFiles) {
      if (file.getName().equals("dot-git")) {
        if (!file.renameTo(new File(file.getParent(), ".git"))) {
          throw new IllegalStateException("Could not rename dot-git to .git.");
        }
        break;
      }
    }
  }

  private static void addDiffs(Path projectDir, String... paths) {
    for (String path : paths) {
      var filePath = projectDir.resolve(path);
      try {
        Files.writeString(filePath, Files.readString(filePath).replace("|DIFF-ME|", "-DIFFED-"));
      } catch (IOException e) {
        throw new UncheckedIOException("Could not add diff to file.", e);
      }
    }

    try (Git git = Git.open(projectDir.toFile())) {
      git.commit()
          .setAll(true)
          .setMessage("Automated commit in test")
          .setAuthor("Unit Test", "test@curioswitch.org")
          .call();
    } catch (Exception e) {
      throw new IllegalStateException("Error manipulating git repo.", e);
    }
  }

  private static void changeBranch(Path projectDir, String branch) {
    try (Git git = Git.open(projectDir.toFile())) {
      git.checkout().setName(branch).call();
    } catch (Exception e) {
      throw new IllegalStateException("Error manipulating git repo.", e);
    }
  }

  // We add two empty commits to the branch builds to make sure the diff isn't being computed from
  // the latest commits but the actual branch diff.
  private static void addTwoEmptyCommits(Path projectDir) {
    try (Git git = Git.open(projectDir.toFile())) {
      git.commit()
          .setAllowEmpty(true)
          .setMessage("Empty commit 1")
          .setAuthor("Unit Test", "test@curioswitch.org")
          .call();
      git.commit()
          .setAllowEmpty(true)
          .setMessage("Empty commit 2")
          .setAuthor("Unit Test", "test@curioswitch.org")
          .call();
    } catch (Exception e) {
      throw new IllegalStateException("Error manipulating git repo.", e);
    }
  }
}
