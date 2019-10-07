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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.curioswitch.gradle.testing.assertj.CurioGradleAssertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.curioswitch.gradle.testing.ResourceProjects;
import org.eclipse.jgit.api.Git;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DisabledIfEnvironmentVariable(named = "CLOUDBUILD_BUILD_ID", matches = ".+")
class CurioGenericCiPluginTest {

  private static final List<String> ALL_TASKS =
      ImmutableList.of(
          ":library1:jar",
          ":library1:build",
          ":library2:jar",
          ":library2:build",
          ":server1:build",
          ":server1:jib",
          ":server1:deployAlpha",
          ":server2:build",
          ":server2:jib",
          ":server2:deployAlpha",
          ":staticsite1:deployAlpha",
          ":staticsite1:deployProd",
          ":staticsite2:deployAlpha",
          ":staticsite2:deployProd");

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchNoDiffs {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void noCi() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild", "--stacktrace")
                  .withEnvironment(ImmutableMap.of())
                  .withPluginClasspath())
          .fails()
          .outputContains("Task 'continuousBuild' not found in root project");
    }

    @Test
    void ciNotMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true"))
                  .withPluginClasspath())
          .builds()
          .tasksDidNotRun(ALL_TASKS);
    }

    @Test
    void ciIsMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
                  .withPluginClasspath())
          .builds()
          .tasksDidNotRun(ALL_TASKS);
    }

    @SuppressWarnings("ClassCanBeStatic")
    @Nested
    class ReleaseBuild {
      @Test
      void implicitTag() {
        assertThat(
                GradleRunner.create()
                    .withProjectDir(projectDir)
                    .withArguments("releaseBuild")
                    .withEnvironment(
                        ImmutableMap.of("CI", "true", "TAG_NAME", "RELEASE_SERVER1_20190522"))
                    .withPluginClasspath())
            .builds()
            .satisfies(onlyDidRun(":server1:build", ":server1:jib"));
      }

      @Test
      void explicitTag() {
        assertThat(
                GradleRunner.create()
                    .withProjectDir(projectDir)
                    .withArguments("releaseBuild")
                    .withEnvironment(
                        ImmutableMap.of("CI", "true", "TAG_NAME", "RELEASE_CUSTOM_TAG_20190522"))
                    .withPluginClasspath())
            .builds()
            .satisfies(onlyDidRun(":library1:jar", ":server2:build", ":server2:jib"));
      }

      @Test
      void unknownTag() {
        assertThat(
                GradleRunner.create()
                    .withProjectDir(projectDir)
                    .withArguments("releaseBuild")
                    .withEnvironment(
                        ImmutableMap.of("CI", "true", "TAG_NAME", "RELEASE_UNKNOWN1_20190522"))
                    .withPluginClasspath())
            .fails()
            .outputContains("Task 'releaseBuild' not found in root project");
      }

      @Test
      void staticSite() {
        assertThat(
                GradleRunner.create()
                    .withProjectDir(projectDir)
                    .withArguments("releaseBuild")
                    .withEnvironment(
                        ImmutableMap.of("CI", "true", "TAG_NAME", "RELEASE_STATICSITE1_20190522"))
                    .withPluginClasspath())
            .builds()
            .satisfies(onlyDidRun(":staticsite1:deployProd"));
      }
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchLibrary1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      addDiffs(projectDir, "library1/src/main/java/Library1.java");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciIsMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(
              onlyDidRun(
                  ":library1:jar",
                  ":library1:build",
                  ":server2:build",
                  ":server2:jib",
                  ":server2:deployAlpha"));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchLibrary2Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      addDiffs(projectDir, "library2/src/main/java/Library2.java");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciIsMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(onlyDidRun(":library2:jar", ":library2:build"));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchServer1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      addDiffs(projectDir, "server1/src/main/java/Server1.java");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciIsMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(onlyDidRun(":server1:build", ":server1:jib", ":server1:deployAlpha"));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchServer2Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      addDiffs(projectDir, "server2/src/main/java/Server2.java");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciIsMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(
              onlyDidRun(
                  ":library1:jar", ":server2:build", ":server2:jib", ":server2:deployAlpha"));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchStaticSite1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      addDiffs(projectDir, "staticsite1/build.gradle.kts");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciIsMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(onlyDidRun(":staticsite1:deployAlpha"));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchStaticSite2Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      addDiffs(projectDir, "staticsite2/build.gradle.kts");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciIsMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
                  .withPluginClasspath())
          .builds()
          .tasksDidNotRun(ALL_TASKS);
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchTopLevelDiff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      addDiffs(projectDir, "build.gradle.kts");

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciIsMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(
              onlyDidRun(
                  ":library1:jar",
                  ":library1:build",
                  ":library2:jar",
                  ":library2:build",
                  ":server1:build",
                  ":server1:jib",
                  ":server1:deployAlpha",
                  ":server2:build",
                  ":server2:jib",
                  ":server2:deployAlpha",
                  ":staticsite1:deployAlpha"));
    }
  }

  // We go ahead and verify a master branch with the same state as the pr branch has different build
  // behavior.
  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchTopLevelDiffFollowedByTwoEmptyCommits {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      addDiffs(projectDir, "build.gradle.kts");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciIsMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true", "CI_MASTER", "true"))
                  .withPluginClasspath())
          .builds()
          .tasksDidNotRun(ALL_TASKS);
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchLibrary1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "library1/src/main/java/Library1.java");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(onlyDidRun(":library1:jar", ":library1:build", ":server2:build"));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchLibrary2Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "library2/src/main/java/Library2.java");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(onlyDidRun(":library2:jar", ":library2:build"));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchServer1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "server1/src/main/java/Server1.java");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(onlyDidRun(":server1:build"));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchServer2Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "server2/src/main/java/Server2.java");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true"))
                  .withPluginClasspath()
                  .forwardOutput())
          .builds()
          .satisfies(onlyDidRun(":library1:jar", ":server2:build"));
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchStaticSite1Diff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "staticsite1/build.gradle.kts");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true"))
                  .withPluginClasspath()
                  .forwardOutput())
          .builds()
          .tasksDidNotRun(ALL_TASKS);
    }
  }

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class PRBranchTopLevelDiff {

    private File projectDir;

    @BeforeAll
    void copyProject() throws Exception {
      Path projectDir =
          copyGitRepoFromResources("test-projects/gradle-curio-generic-ci-plugin/master-no-diffs");

      changeBranch(projectDir, "prbuild");
      addDiffs(projectDir, "build.gradle.kts");
      addTwoEmptyCommits(projectDir);

      this.projectDir = projectDir.toFile();
    }

    @Test
    void ciNotMaster() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir)
                  .withArguments("continuousBuild")
                  .withEnvironment(ImmutableMap.of("CI", "true"))
                  .withPluginClasspath())
          .builds()
          .satisfies(
              onlyDidRun(
                  ":library1:jar",
                  ":library1:build",
                  ":library2:jar",
                  ":library2:build",
                  ":server1:build",
                  ":server2:build"));
    }
  }

  private static Path copyGitRepoFromResources(String resourcePath) {
    Path projectDir = ResourceProjects.fromResources(resourcePath);

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

    return projectDir;
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

  private static Consumer<BuildResult> onlyDidRun(String... tasks) {
    return result -> assertThat(result).tasksDidRun(tasks).tasksDidNotRun(allTasksExcept(tasks));
  }

  private static List<String> allTasksExcept(String... exceptions) {
    Set<String> exceptionsSet = ImmutableSet.copyOf(exceptions);
    return ALL_TASKS.stream()
        .filter(task -> !exceptionsSet.contains(task))
        .collect(toImmutableList());
  }
}
