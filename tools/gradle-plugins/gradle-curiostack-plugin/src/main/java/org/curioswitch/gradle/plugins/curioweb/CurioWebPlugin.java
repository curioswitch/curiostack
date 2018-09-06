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

package org.curioswitch.gradle.plugins.curioweb;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.moowork.gradle.node.yarn.YarnTask;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;

/**
 * A simple {@link Plugin} which configures a project with tasks for building a static npm web
 * application and bundling it into a jar for serving from a Java server.
 */
public class CurioWebPlugin implements Plugin<Project> {

  private static final Splitter YARN_TASK_SPLITTER = Splitter.on('_');

  @Override
  public void apply(Project project) {
    project.getExtensions().create("web", WebExtension.class);

    project.getPlugins().apply(JavaLibraryPlugin.class);

    JavaPluginConvention java = project.getConvention().getPlugin(JavaPluginConvention.class);
    java.getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        .getOutput()
        .dir(ImmutableMap.of("builtBy", "copyWeb"), "build/javaweb");

    var buildWebCheck =
        project
            .getTasks()
            .register(
                "buildWebCheck",
                t ->
                    t.doLast(
                        unused -> {
                          if (!project.file("build/web").exists()) {
                            throw new RuntimeException("Build failed.");
                          }
                        }));

    var buildWeb =
        project
            .getRootProject()
            .getTasks()
            .register(
                rootTaskName("buildWeb", project),
                CacheableYarnTask.class,
                t -> {
                  t.dependsOn(project.getRootProject().getTasks().findByName("yarn"));
                  t.setArgs(ImmutableList.of("run", "build"));
                  t.setWorkingDir(project.getProjectDir());
                  t.getInputs().dir(project.file("src").getAbsolutePath());
                  t.getInputs().dir(project.file("package.json").getAbsolutePath());
                  // We assume the yarn task correctly handles up-to-date checks for node_modules,
                  // so only
                  // need to look at yarn.lock here.
                  t.getInputs().file(project.getRootProject().file("yarn.lock").getAbsolutePath());
                  t.getOutputs()
                      .dirs(
                          ImmutableMap.of(
                              "yarnBuild", project.file("build/web").getAbsolutePath()));
                  t.finalizedBy(buildWebCheck);
                });

    var copyWeb =
        project
            .getTasks()
            .register(
                "copyWeb",
                Copy.class,
                t -> {
                  t.dependsOn(buildWeb);
                  t.from("build/web");
                });
    project.afterEvaluate(
        p -> {
          ImmutableWebExtension web = project.getExtensions().getByType(WebExtension.class);
          copyWeb.configure(t -> t.into("build/javaweb/" + web.javaPackage().replace('.', '/')));
        });

    // Copy in yarn rule from node plugin since we don't directly apply the plugin here.
    project
        .getTasks()
        .addRule(
            "Pattern: \"yarn_<command>\": Executes an Yarn command.",
            taskName -> {
              if (taskName.startsWith("yarn_")) {
                YarnTask yarnTask =
                    project
                        .getRootProject()
                        .getTasks()
                        .create(rootTaskName(taskName, project), YarnTask.class);
                yarnTask.setWorkingDir(project.getProjectDir());
                List<String> tokens = YARN_TASK_SPLITTER.splitToList(taskName);
                yarnTask.setYarnCommand(
                    tokens.subList(1, tokens.size()).stream().toArray(String[]::new));

                project.getTasks().create(taskName).dependsOn(yarnTask);
              }
            });
  }

  private static String rootTaskName(String prefix, Project project) {
    return prefix + '_' + project.getPath().replace(':', '_');
  }

  @CacheableTask
  public static class CacheableYarnTask extends YarnTask {}
}
