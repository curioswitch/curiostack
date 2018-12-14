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

package org.curioswitch.gradle.plugins.ci;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.curioswitch.gradle.golang.GolangExtension;
import org.curioswitch.gradle.golang.GolangPlugin;
import org.curioswitch.gradle.golang.tasks.GoTestTask;
import org.curioswitch.gradle.golang.tasks.JibTask;
import org.curioswitch.gradle.plugins.ci.tasks.UploadToCodeCovTask;
import org.curioswitch.gradle.plugins.curioserver.CurioServerPlugin;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
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
import org.gradle.api.UnknownTaskException;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Delete;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;
import org.gradle.testing.jacoco.tasks.JacocoReport;

public class CurioGenericCiPlugin implements Plugin<Project> {

  private static final ImmutableSet<String> IGNORED_ROOT_FILES =
      ImmutableSet.of("settings.gradle", "yarn.lock", ".gitignore");

  private static final ImmutableList<String> COMMON_RELEASE_BRANCH_ENV_VARS =
      ImmutableList.of(
          "TAG_NAME", "CIRCLE_TAG", "TRAVIS_TAG", "BRANCH_NAME", "CIRCLE_BRANCH", "TRAVIS_BRANCH");

  private static final Splitter RELEASE_TAG_SPLITTER = Splitter.on('_');

  @Override
  public void apply(Project project) {
    if (project.getParent() != null) {
      throw new IllegalStateException(
          "curio-generic-ci-plugin can only be " + "applied to the root project.");
    }

    var config = CiExtension.createAndAdd(project);

    if (System.getenv("CI") == null && !project.hasProperty("ci")) {
      return;
    }

    var cleanWrapper =
        project
            .getTasks()
            .register(
                "cleanWrapper",
                Delete.class,
                t -> {
                  File gradleHome = project.getGradle().getGradleHomeDir();

                  t.delete(new File(gradleHome, "docs"));
                  t.delete(new File(gradleHome, "media"));
                  t.delete(new File(gradleHome, "samples"));
                  t.delete(new File(gradleHome, "src"));

                  // Zip file should always be foldername-all.zip
                  var archive = new File(gradleHome.getParent(), gradleHome.getName() + "-all.zip");
                  t.delete(archive);
                });

    Task continuousBuild = project.task("continuousBuild");

    boolean isMaster = System.getenv("CI_MASTER") != null;
    if (isMaster) {
      continuousBuild.dependsOn(cleanWrapper);
    }

    project.allprojects(
        proj ->
            proj.getPlugins()
                .withType(
                    GolangPlugin.class,
                    unused ->
                        proj.getExtensions()
                            .getByType(GolangExtension.class)
                            .jib(jib -> jib.getAdditionalTags().addAll(getRevisionTags(proj)))));

    String releaseBranch = getCiBranchOrTag();
    if (releaseBranch != null && releaseBranch.startsWith("RELEASE_")) {
      project
          .getExtensions()
          .getByType(ExtraPropertiesExtension.class)
          .set("curiostack.releaseBranch", releaseBranch);
      project.afterEvaluate(CurioGenericCiPlugin::configureReleaseBuild);
      return;
    }

    var uploadCoverage = project.getTasks().create("uploadToCodeCov", UploadToCodeCovTask.class);
    continuousBuild.dependsOn(uploadCoverage);

    project.allprojects(
        proj -> {
          proj.getPlugins()
              .withType(JavaPlugin.class, unused -> proj.getPlugins().apply(JacocoPlugin.class));

          proj.getPlugins()
              .withType(
                  GolangPlugin.class,
                  unused ->
                      proj.getTasks()
                          .withType(GoTestTask.class)
                          .configureEach(
                              t -> {
                                t.coverage(true);
                                uploadCoverage.dependsOn(t);
                              }));
        });

    project.subprojects(
        proj ->
            proj.getPlugins()
                .withType(
                    JacocoPlugin.class,
                    unused -> {
                      var testReport =
                          proj.getTasks().named("jacocoTestReport", JacocoReport.class);
                      uploadCoverage.dependsOn(testReport);
                      testReport.configure(
                          t ->
                              t.reports(
                                  reports -> {
                                    reports.getXml().setEnabled(true);
                                    reports.getHtml().setEnabled(true);
                                    reports.getCsv().setEnabled(false);
                                  }));
                      try {
                        proj.getTasks().named("build").configure(t -> t.dependsOn(testReport));
                      } catch (UnknownTaskException e) {
                        // Ignore.
                      }
                    }));

    final Set<Project> affectedProjects;
    try {
      affectedProjects = computeAffectedProjects(project);
    } catch (Throwable t) {
      // Don't prevent further gradle configuration due to issues computing the git state.
      project.getLogger().warn("Couldn't compute affected targets.", t);
      return;
    }

    if (affectedProjects.contains(project.getRootProject())) {
      // Rebuild everything when the root project is changed.
      project.allprojects(
          proj ->
              proj.afterEvaluate(
                  p -> {
                    Task task = p.getTasks().findByName("build");
                    if (task != null) {
                      continuousBuild.dependsOn(task);
                    }
                    if (isMaster) {
                      continuousBuild.dependsOn(p.getTasks().withType(JibTask.class));
                    }
                  }));
      return;
    }

    for (Project proj : affectedProjects) {
      proj.afterEvaluate(
          p -> {
            Task task = p.getTasks().findByName("build");
            if (task != null) {
              continuousBuild.dependsOn(task);
            }
            if (isMaster) {
              continuousBuild.dependsOn(p.getTasks().withType(JibTask.class));
            }
            Task dependentsTask = p.getTasks().findByName("buildDependents");
            if (dependentsTask != null) {
              continuousBuild.dependsOn(dependentsTask);
            }
          });
    }
  }

  private static Set<Project> computeAffectedProjects(Project project) {
    final Set<String> affectedRelativeFilePaths;
    try (Git git = Git.open(project.getRootDir())) {
      // TODO(choko): Validate the remote of the branch, which matters if there are forks.
      String branch = git.getRepository().getBranch();

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

  private static Project getProjectForFile(Path filePath, Map<Path, Project> projectsByPath) {
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

  private static Set<String> computeAffectedFilesForBranch(Git git, String branch)
      throws IOException {
    String masterRemote =
        git.getRepository().getRemoteNames().contains("upstream") ? "upstream" : "origin";
    CanonicalTreeParser oldTreeParser =
        parserForBranch(
            git, git.getRepository().exactRef("refs/remotes/" + masterRemote + "/master"));
    CanonicalTreeParser newTreeParser =
        parserForBranch(git, git.getRepository().exactRef(Constants.HEAD));
    return computeAffectedFiles(git, oldTreeParser, newTreeParser);
  }

  // Assume all tested changes are in a single commit, which works when commits are always squashed.
  private static Set<String> computeAffectedFilesForMaster(Git git) throws IOException {
    ObjectId oldTreeId = git.getRepository().resolve("HEAD^{tree}");
    ObjectId newTreeId = git.getRepository().resolve("HEAD^^{tree}");

    final CanonicalTreeParser oldTreeParser;
    final CanonicalTreeParser newTreeParser;
    try (ObjectReader reader = git.getRepository().newObjectReader()) {
      oldTreeParser = parser(reader, oldTreeId);
      newTreeParser = parser(reader, newTreeId);
    }

    return computeAffectedFiles(git, oldTreeParser, newTreeParser);
  }

  private static Set<String> computeAffectedFiles(
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
    return affectedRelativePaths
        .stream()
        .filter(path -> !IGNORED_ROOT_FILES.contains(path))
        .collect(toImmutableSet());
  }

  private static CanonicalTreeParser parserForBranch(Git git, Ref branch) throws IOException {
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

  private static CanonicalTreeParser parser(ObjectReader reader, ObjectId id) throws IOException {
    CanonicalTreeParser parser = new CanonicalTreeParser();
    parser.reset(reader, id);
    return parser;
  }

  private static void configureReleaseBuild(Project project) {
    var ci = project.getExtensions().getByType(CiExtension.class);
    String branch = getCiBranchOrTag();
    checkNotNull(branch);

    List<Project> affectedProjects = new ArrayList<>();

    ci.releaseTagPrefixes()
        .forEach(
            (prefix, projectPaths) -> {
              if (branch.startsWith(prefix)) {
                for (String projectPath : projectPaths) {
                  Project affectedProject = project.findProject(projectPath);
                  checkNotNull(affectedProject, "could not find project " + projectPath);
                  affectedProjects.add(affectedProject);
                }
              }
            });
    if (affectedProjects.isEmpty()) {
      // Not a statically defined tag, try to guess.
      var parts =
          RELEASE_TAG_SPLITTER
              .splitToList(branch.substring("RELEASE_".length()))
              .stream()
              .map(Ascii::toLowerCase)
              .collect(toImmutableList());
      for (int i = parts.size(); i >= 1; i--) {
        String projectPath = ":" + String.join(":", parts.subList(0, i));
        Project affectedProject = project.findProject(projectPath);
        if (affectedProject != null) {
          affectedProjects.add(affectedProject);
          break;
        }
      }
    }
    if (affectedProjects.isEmpty()) {
      return;
    }

    Task releaseBuild = project.getTasks().create("releaseBuild");
    for (var affectedProject : affectedProjects) {
      affectedProject
          .getPlugins()
          .withType(
              CurioServerPlugin.class,
              unused -> {
                releaseBuild.dependsOn(affectedProject.getTasks().getByName("build"));
                releaseBuild.dependsOn(affectedProject.getTasks().getByName("jib"));
              });
      affectedProject
          .getPlugins()
          .withType(
              GolangPlugin.class,
              unused -> {
                releaseBuild.dependsOn(affectedProject.getTasks().getByName("build"));
                releaseBuild.dependsOn(affectedProject.getTasks().withType(JibTask.class));
              });
    }
  }

  public static List<String> getRevisionTags(Project project) {
    var tags = new ImmutableList.Builder<String>();
    String revisionId = (String) project.getRootProject().findProperty("curiostack.revisionId");
    if (revisionId != null) {
      tags.add(revisionId);
    }
    String branch = getCiBranchOrTag();
    if (branch != null && branch.startsWith("RELEASE_")) {
      tags.add(branch);
    }
    return tags.build();
  }

  @Nullable
  private static String getCiBranchOrTag() {
    // Not quite "generic" but should satisfy 99% of cases.
    for (String key : COMMON_RELEASE_BRANCH_ENV_VARS) {
      String branch = System.getenv(key);
      if (!Strings.isNullOrEmpty(branch)) {
        return branch;
      }
    }
    String jenkinsBranch = System.getenv("GIT_BRANCH");
    if (!Strings.isNullOrEmpty(jenkinsBranch)) {
      // Usually has remote too
      int slashIndex = jenkinsBranch.indexOf('/');
      return slashIndex >= 0 ? jenkinsBranch.substring(slashIndex + 1) : jenkinsBranch;
    }
    return null;
  }
}
