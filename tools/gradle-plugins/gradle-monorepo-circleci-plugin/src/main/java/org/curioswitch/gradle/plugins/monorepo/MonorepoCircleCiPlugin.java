/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.plugins.monorepo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;

public class MonorepoCircleCiPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    if (project.getParent() != null) {
      throw new IllegalStateException(
          "gradle-monorepo-circleci-plugin can only be " + "applied to the root project.");
    }
    if (System.getenv("CIRCLECI") == null) {
      return;
    }

    project.allprojects(
        p ->
            p.getPlugins()
                .withType(JavaPlugin.class)
                .whenPluginAdded(
                    plugin -> {
                      Task testDependents = p.task("testDependents");
                      testDependents.dependsOn(p.getTasks().findByName("check"));
                      testDependents.dependsOn(
                          p.getConfigurations()
                              .getByName("testRuntime")
                              .getTaskDependencyFromProjectDependency(false, "testDependents"));
                    }));

    Set<Project> affectedProjects = computeAffectedProjects(project);
    Task continuousTest = project.task("continuousTest");
    for (Project proj : affectedProjects) {
      proj.afterEvaluate(
          p -> {
            Task testTask = p.getTasks().findByName("check");
            if (testTask != null) {
              continuousTest.dependsOn(testTask);
            }
            Task testDependentsTask = p.getTasks().findByName("testDependents");
            if (testDependentsTask != null) {
              continuousTest.dependsOn(testDependentsTask);
            }
          });
    }
  }

  private Set<Project> computeAffectedProjects(Project project) {
    final Set<String> affectedRelativeFilePaths;
    try (Git git = Git.open(project.getRootDir())) {
      String branch = currentBranch();
      if (branch.equals("master")) {
        affectedRelativeFilePaths = computeAffectedFilesForMaster(git);
      } else {
        affectedRelativeFilePaths = computeAffectedFilesForBranch(git, branch);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    Set<Path> affectedPaths =
        affectedRelativeFilePaths
            .stream()
            .map(p -> Paths.get(project.getRootDir().getAbsolutePath(), p))
            .collect(Collectors.toSet());

    Map<Path, Project> projectsByPath =
        Collections.unmodifiableMap(
            project
                .getAllprojects()
                .stream()
                .collect(
                    Collectors.toMap(
                        p -> Paths.get(p.getProjectDir().getAbsolutePath()), Function.identity())));
    return affectedPaths
        .stream()
        .map(f -> getProjectForFile(f, projectsByPath))
        .collect(Collectors.toSet());
  }

  private Project getProjectForFile(Path filePath, Map<Path, Project> projectsByPath) {
    Path currentDirectory = filePath.getParent();
    while (!currentDirectory.toAbsolutePath().toString().isEmpty()) {
      Project project = projectsByPath.get(currentDirectory);
      if (project != null) {
        return project;
      }
      currentDirectory = currentDirectory.getParent();
    }
    throw new IllegalStateException(
        "Could not find project for a file in the project, this cannot happen: " + filePath);
  }

  private Set<String> computeAffectedFilesForBranch(Git git, String branch) throws IOException {
    CanonicalTreeParser oldTreeParser =
        parserForBranch(git, git.getRepository().exactRef("refs/remotes/origin/master"));
    CanonicalTreeParser newTreeParser =
        parserForBranch(git, git.getRepository().exactRef("refs/heads/" + branch));
    return computeAffectedFiles(git, oldTreeParser, newTreeParser);
  }

  private Set<String> computeAffectedFilesForMaster(Git git) throws IOException {
    String compareUrl = System.getenv("CIRCLE_COMPARE_URL");
    List<String> parts = Arrays.asList(compareUrl.split("/"));
    String commitsPart = parts.get(parts.size() - 1);
    List<String> commits = Arrays.asList(commitsPart.split("\\.\\.\\."));
    ObjectId oldTreeId = git.getRepository().resolve(commits.get(0) + "^{tree}");
    ObjectId newTreeId = git.getRepository().resolve(commits.get(1) + "^{tree}");

    final CanonicalTreeParser oldTreeParser;
    final CanonicalTreeParser newTreeParser;
    try (ObjectReader reader = git.getRepository().newObjectReader()) {
      oldTreeParser = parser(reader, oldTreeId);
      newTreeParser = parser(reader, newTreeId);
    }

    return computeAffectedFiles(git, oldTreeParser, newTreeParser);
  }

  private Set<String> computeAffectedFiles(
      Git git, CanonicalTreeParser oldTreeParser, CanonicalTreeParser newTreeParser) {
    final List<DiffEntry> diffs;
    try {
      diffs =
          git.diff()
              .setNewTree(newTreeParser)
              .setOldTree(oldTreeParser)
              .setShowNameAndStatusOnly(true)
              .call();
    } catch (GitAPIException e) {
      throw new IllegalStateException(e);
    }
    Set<String> affectedRelativePaths = new HashSet<>();
    for (DiffEntry diff : diffs) {
      switch (diff.getChangeType()) {
        case ADD:
        case MODIFY:
        case COPY:
          affectedRelativePaths.add(diff.getNewPath());
          break;
        case DELETE:
          affectedRelativePaths.add(diff.getOldPath());
          break;
        case RENAME:
          affectedRelativePaths.add(diff.getNewPath());
          affectedRelativePaths.add(diff.getOldPath());
          break;
      }
    }
    return affectedRelativePaths;
  }

  private CanonicalTreeParser parserForBranch(Git git, Ref branch) throws IOException {
    try (RevWalk walk = new RevWalk(git.getRepository())) {
      RevCommit commit = walk.parseCommit(branch.getObjectId());
      RevTree tree = walk.parseTree(commit.getTree().getId());

      final CanonicalTreeParser parser;
      try (ObjectReader reader = git.getRepository().newObjectReader()) {
        parser = parser(reader, tree.getId());
      }

      walk.dispose();

      return parser;
    }
  }

  private CanonicalTreeParser parser(ObjectReader reader, ObjectId id) throws IOException {
    CanonicalTreeParser parser = new CanonicalTreeParser();
    parser.reset(reader, id);
    return parser;
  }

  private String currentBranch() {
    String tag = System.getenv("CIRCLE_TAG");
    return tag != null ? tag : System.getenv("CIRCLE_BRANCH");
  }
}
