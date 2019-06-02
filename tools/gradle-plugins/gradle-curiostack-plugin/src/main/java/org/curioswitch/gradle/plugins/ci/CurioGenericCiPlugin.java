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
import org.curioswitch.gradle.golang.GolangExtension;
import org.curioswitch.gradle.golang.GolangPlugin;
import org.curioswitch.gradle.golang.tasks.GoTestTask;
import org.curioswitch.gradle.golang.tasks.JibTask;
import org.curioswitch.gradle.plugins.ci.tasks.FetchCodeCovCacheTask;
import org.curioswitch.gradle.plugins.ci.tasks.UploadCodeCovCacheTask;
import org.curioswitch.gradle.plugins.ci.tasks.UploadToCodeCovTask;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
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
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;
import org.gradle.testing.jacoco.tasks.JacocoReport;

public class CurioGenericCiPlugin implements Plugin<Project> {

  private static final ImmutableSet<String> IGNORED_ROOT_FILES =
      ImmutableSet.of("settings.gradle", "yarn.lock", ".gitignore");

  private static final Splitter RELEASE_TAG_SPLITTER = Splitter.on('_');

  static final ImmutableList<String> COMMON_RELEASE_BRANCH_ENV_VARS =
      ImmutableList.of(
          "TAG_NAME", "CIRCLE_TAG", "TRAVIS_TAG", "BRANCH_NAME", "CIRCLE_BRANCH", "TRAVIS_BRANCH");

  static final String CI_STATE_PROPERTY = "org.curioswitch.curiostack.generic-ci-plugin.ciState";

  /** Registers the given {@code task} to run during a master CI build. */
  public static void addToMasterBuild(Project project, TaskProvider<?> task) {
    doAddToMasterBuild(project, task);
  }

  /** Registers the given {@code task} to run during a master CI build. */
  public static void addToMasterBuild(Project project, Task task) {
    doAddToMasterBuild(project, task);
  }

  private static void doAddToMasterBuild(Project project, Object task) {
    project
        .getRootProject()
        .getPlugins()
        .withType(
            CurioGenericCiPlugin.class,
            unused -> {
              if (getCiState(project).isMasterBuild()) {
                project
                    .getPlugins()
                    .withType(
                        BasePlugin.class,
                        unused2 ->
                            project.getTasks().named("build").configure(t -> t.dependsOn(task)));
              }
            });
  }

  /** Registers the given {@code task} to run during a release CI build. */
  public static void addToReleaseBuild(Project project, TaskProvider<?> task) {
    doAddToReleaseBuild(project, task);
  }

  /** Registers the given {@code task} to run during a release CI build. */
  public static void addToReleaseBuild(Project project, Task task) {
    doAddToReleaseBuild(project, task);
  }

  private static void doAddToReleaseBuild(Project project, Object task) {
    project
        .getRootProject()
        .getPlugins()
        .withType(
            CurioGenericCiPlugin.class,
            unused -> {
              if (getCiState(project).isReleaseBuild()) {
                project
                    .getPlugins()
                    .withType(
                        BasePlugin.class,
                        unused2 ->
                            project.getTasks().named("build").configure(t -> t.dependsOn(task)));
              }
            });
  }

  /** Registers the given {@code task} to run during a branch CI build. */
  public static void addToBranchBuild(Project project, TaskProvider<?> task) {
    doAddToBranchBuild(project, task);
  }

  /** Registers the given {@code task} to run during a branch CI build. */
  public static void addToBranchBuild(Project project, Task task) {
    doAddToBranchBuild(project, task);
  }

  private static void doAddToBranchBuild(Project project, Object task) {
    project
        .getRootProject()
        .getPlugins()
        .withType(
            CurioGenericCiPlugin.class,
            unused -> {
              var state = getCiState(project);
              if (!state.isReleaseBuild() && !state.isMasterBuild()) {
                project
                    .getPlugins()
                    .withType(
                        BasePlugin.class,
                        unused2 ->
                            project.getTasks().named("build").configure(t -> t.dependsOn(task)));
              }
            });
  }

  /** Returns the {@link CiState} for this build. */
  public static CiState getCiState(Project project) {
    Object state =
        project
            .getRootProject()
            .getExtensions()
            .getByType(ExtraPropertiesExtension.class)
            .get(CI_STATE_PROPERTY);
    checkNotNull(
        state,
        "Could not find CiState. Has gradle-curiostack-plugin or curio-generic-ci-plugin been "
            + "applied to the root project?");
    return (CiState) state;
  }

  @Override
  public void apply(Project project) {
    if (project.getParent() != null) {
      throw new IllegalStateException(
          "curio-generic-ci-plugin can only be applied to the root project.");
    }

    var config = CiExtension.createAndAdd(project);

    var state = CiState.createAndAdd(project);

    if (!state.isCi()) {
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

    if (state.isMasterBuild()) {
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
                            .jib(jib -> jib.getAdditionalTags().addAll(state.getRevisionTags()))));

    if (state.isReleaseBuild()) {
      project.afterEvaluate(unused -> configureReleaseBuild(project, config, state));
      return;
    }

    var fetchCodeCovCache =
        project.getTasks().create("fetchCodeCovCache", FetchCodeCovCacheTask.class);

    var uploadCodeCovCache =
        project.getTasks().create("uploadCodeCovCache", UploadCodeCovCacheTask.class);

    var uploadCoverage =
        project
            .getTasks()
            .create(
                "uploadToCodeCov",
                UploadToCodeCovTask.class,
                t -> {
                  t.dependsOn(DownloadToolUtil.getSetupTask(project, "miniconda2-build"));
                  if (state.isMasterBuild()) {
                    t.finalizedBy(uploadCodeCovCache);
                  }
                });

    // Don't need to slow down local builds with coverage.
    if (!state.isLocalBuild()) {
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
                                  uploadCoverage.mustRunAfter(t);
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
                        uploadCoverage.mustRunAfter(testReport);
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
    }

    final Set<Project> affectedProjects;
    try {
      affectedProjects = computeAffectedProjects(project);
    } catch (Throwable t) {
      // Don't prevent further gradle configuration due to issues computing the git state.
      project.getLogger().warn("Couldn't compute affected targets.", t);
      return;
    }

    final Set<Project> projectsToBuild;
    if (affectedProjects.contains(project.getRootProject())) {
      // Rebuild everything when the root project is changed.
      projectsToBuild = ImmutableSet.copyOf(project.getAllprojects());
    } else {
      projectsToBuild = affectedProjects;
      // We only fetch the code cov cache on non-full builds since we don't need to propagate for
      // full ones.
      uploadCoverage.dependsOn(fetchCodeCovCache);
    }

    for (var proj : projectsToBuild) {
      proj.getPlugins()
          .withType(
              GolangPlugin.class,
              unused -> {
                // TODO(choko): Figure out whether it's better to register plugins outside this
                // artifact here or in each plugin somehow.
                proj.getTasks().withType(JibTask.class, t -> addToMasterBuild(proj, t));
              });

      proj.getPlugins()
          .withType(
              LifecycleBasePlugin.class,
              unused ->
                  continuousBuild.dependsOn(
                      proj.getTasks().named(LifecycleBasePlugin.BUILD_TASK_NAME)));

      proj.getPlugins()
          .withType(
              JavaBasePlugin.class,
              unused ->
                  continuousBuild.dependsOn(
                      proj.getTasks().named(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME)));
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
        affectedRelativeFilePaths.stream()
            .map(p -> Paths.get(project.getRootDir().getAbsolutePath(), p))
            .collect(Collectors.toSet());

    Map<Path, Project> projectsByPath =
        Collections.unmodifiableMap(
            project.getAllprojects().stream()
                .collect(
                    Collectors.toMap(
                        p -> Paths.get(p.getProjectDir().getAbsolutePath()), Function.identity())));
    return affectedPaths.stream()
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
      System.out.println(diff);
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
    return affectedRelativePaths.stream()
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

  private static void configureReleaseBuild(Project project, CiExtension config, CiState state) {
    List<Project> affectedProjects = new ArrayList<>();

    config
        .getReleaseTagPrefixes()
        .all(
            tag -> {
              if (state.getBranch().startsWith(tag.getName())) {
                for (String projectPath : tag.getProjects().get()) {
                  Project affectedProject = project.findProject(projectPath);
                  checkNotNull(affectedProject, "could not find project " + projectPath);
                  affectedProjects.add(affectedProject);
                }
              }
            });

    if (affectedProjects.isEmpty()) {
      // Not a statically defined tag, try to guess.
      var parts =
          RELEASE_TAG_SPLITTER.splitToList(state.getBranch().substring("RELEASE_".length()))
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
              LifecycleBasePlugin.class,
              unused ->
                  releaseBuild.dependsOn(
                      affectedProject.getTasks().named(LifecycleBasePlugin.BUILD_TASK_NAME)));

      affectedProject
          .getPlugins()
          .withType(
              GolangPlugin.class,
              unused -> releaseBuild.dependsOn(affectedProject.getTasks().withType(JibTask.class)));
    }
  }
}
