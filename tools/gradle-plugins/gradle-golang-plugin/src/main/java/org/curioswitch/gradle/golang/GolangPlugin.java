/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.golang;

import static org.curioswitch.gradle.helpers.task.TaskUtil.toTaskSuffix;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import org.curioswitch.gradle.conda.exec.CondaExecUtil;
import org.curioswitch.gradle.golang.tasks.GoTask;
import org.curioswitch.gradle.golang.tasks.GolangExtension;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Rule;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public class GolangPlugin implements Plugin<Project> {

  private static final Splitter TASK_NAME_SPLITTER = Splitter.on('_');

  @Override
  public void apply(Project project) {
    project.getRootProject().getPlugins().apply(GolangSetupPlugin.class);

    project.getPlugins().apply(BasePlugin.class);

    project
        .getExtensions()
        .getByType(ExtraPropertiesExtension.class)
        .set(
            "gopath",
            project
                .getGradle()
                .getGradleUserHomeDir()
                .toPath()
                .resolve("curiostack")
                .resolve("gopath"));

    var golang = GolangExtension.createAndAdd(project);

    project
        .getTasks()
        .addRule(
            new Rule() {
              @Override
              public String getDescription() {
                return "Pattern: \"go_<command>\": Executes a go command.";
              }

              @Override
              public void apply(String taskName) {
                if (taskName.startsWith("go_")) {
                  project
                      .getTasks()
                      .register(
                          taskName,
                          GoTask.class,
                          t -> {
                            List<String> tokens = TASK_NAME_SPLITTER.splitToList(taskName);
                            t.args(tokens.subList(1, tokens.size()));
                          });
                }
              }
            });

    var setupGo = DownloadToolUtil.getSetupTask(project, "go");
    setupGo.configure(t -> t.dependsOn(DownloadToolUtil.getSetupTask(project, "miniconda2-build")));

    project.getTasks().withType(GoTask.class).configureEach(t -> t.dependsOn(setupGo));

    var downloadDeps =
        project
            .getTasks()
            .register(
                "goDownloadDeps",
                GoTask.class,
                t -> {
                  t.args("mod", "download");
                  project.getRootProject().mkdir("build");
                  var lock = project.getRootProject().file("build/godeps.lock");
                  try {
                    lock.createNewFile();
                  } catch (IOException e) {
                    throw new UncheckedIOException("Could not create lock file.", e);
                  }
                  t.setLockFile(lock);
                });

    project
        .getTasks()
        .register(
            "goUpdateDeps",
            GoTask.class,
            t -> {
              t.args("get", "-u");
              // Go get often has a failed status code yet still basically worked so we let it go.
              t.execCustomizer(exec -> exec.setIgnoreExitValue(true));
            });

    var checkFormat =
        project
            .getTasks()
            .register(
                "goCheck",
                GoTask.class,
                t -> {
                  t.command("gofmt");
                  t.args("-s", "-d", ".");
                  ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                  t.doLast(
                      unused -> {
                        if (stdOut.size() > 0) {
                          throw new GradleException(
                              "There were formatting violations. Run :goFormat to fix.\n"
                                  + stdOut.toString());
                        }
                      });
                  t.execCustomizer(exec -> exec.setStandardOutput(stdOut));
                });
    var check = project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME);
    check.configure(t -> t.dependsOn(checkFormat));

    project
        .getTasks()
        .register(
            "goFormat",
            GoTask.class,
            t -> {
              t.command("gofmt");
              t.args("-s", "-w", ".");
            });

    var goTest =
        project
            .getTasks()
            .register(
                "goTest",
                GoTask.class,
                t -> {
                  var testPackageDirs =
                      Streams.stream(
                              project.fileTree(
                                  project.getProjectDir(), ft -> ft.include("**/*_test.go")))
                          .map(f -> f.getParentFile().toPath())
                          .map(p -> project.getProjectDir().toPath().relativize(p).toString())
                          .map(p -> p.isEmpty() ? "." : "./" + p)
                          .distinct()
                          .collect(Collectors.joining(" "));
                  t.args("test", testPackageDirs);
                  t.execCustomizer(
                      exec ->
                          CondaExecUtil.condaExec(
                              exec, DownloadedToolManager.get(project), golang.getConda().get()));

                  t.dependsOn(
                      DownloadToolUtil.getSetupTask(project, golang.getConda().get()),
                      downloadDeps);
                });

    var goBuildAll = project.getTasks().register("goBuildAll");
    project
        .getTasks()
        .named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        .configure(t -> t.dependsOn(goBuildAll));

    project.afterEvaluate(
        unused -> {
          TaskProvider<Task> test;
          try {
            test = project.getTasks().named("test");
          } catch (UnknownTaskException e) {
            test = project.getTasks().register("test");
            TaskProvider<Task> testCopy = test;
            check.configure(t -> t.dependsOn(testCopy));
          }
          test.configure(t -> t.dependsOn(goTest));

          List<String> goOses = golang.getGoOses().get();
          if (goOses.isEmpty()) {
            goOses = ImmutableList.of("");
          }
          List<String> goArchs = golang.getGoArchs().get();
          if (goArchs.isEmpty()) {
            goArchs = ImmutableList.of("");
          }

          var goBuildDir = project.file("build/exe").toPath();
          var exeName = golang.getExecutableName().getOrElse(project.getProjectDir().getName());

          for (String goOs : goOses) {
            for (String goArch : goArchs) {
              var goBuild =
                  project
                      .getTasks()
                      .register(
                          "goBuild" + toTaskSuffix(goOs) + toTaskSuffix(goArch),
                          GoTask.class,
                          t -> {
                            String outputDir = goOs.isEmpty() ? "current" : goOs;
                            if (!goArch.isEmpty()) {
                              outputDir += '-' + goArch;
                            }
                            String outPath =
                                goBuildDir.resolve(outputDir).resolve(exeName).toString();
                            if (new PlatformHelper().getOs() == OperatingSystem.WINDOWS) {
                              outPath += ".exe";
                            }
                            t.args("build", "-o", outPath);
                            t.execCustomizer(
                                exec -> {
                                  if (!goOs.isEmpty()) {
                                    exec.environment("GOOS", goOs);
                                  }
                                  if (!goArch.isEmpty()) {
                                    exec.environment("GOARCH", goArch);
                                  }
                                  CondaExecUtil.condaExec(
                                      exec,
                                      DownloadedToolManager.get(project),
                                      golang.getConda().get());
                                });

                            t.dependsOn(
                                DownloadToolUtil.getSetupTask(project, golang.getConda().get()),
                                downloadDeps);
                          });
              goBuildAll.configure(t -> t.dependsOn(goBuild));
            }
          }
        });
  }
}
