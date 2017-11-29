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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.moowork.gradle.node.NodePlugin;
import com.moowork.gradle.node.yarn.YarnInstallTask;
import com.moowork.gradle.node.yarn.YarnTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
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

  @Override
  public void apply(Project project) {
    project.getExtensions().create("web", WebExtension.class);

    project.getPlugins().apply(NodePlugin.class);
    project.getPlugins().apply(JavaLibraryPlugin.class);

    JavaPluginConvention java = project.getConvention().getPlugin(JavaPluginConvention.class);
    java.getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        .getOutput()
        .dir(ImmutableMap.of("builtBy", "copyWeb"), "build/javaweb");

    YarnInstallTask yarnUpdateTask = project.getTasks().create("yarnUpdate", YarnInstallTask.class);

    YarnInstallTask yarnTask = project.getTasks().withType(YarnInstallTask.class).getByName("yarn");
    yarnTask.setArgs(ImmutableList.of("--frozen-lockfile"));
    Task yarnWarning =
        project
            .getTasks()
            .create(
                "yarnWarning",
                task -> {
                  task.onlyIf(unused -> yarnTask.getState().getFailure() != null);
                  task.doFirst(
                      unused ->
                          project
                              .getLogger()
                              .warn(
                                  "yarn task failed. If you have updated a dependency and the "
                                      + "error says 'Your lockfile needs to be updated.', run \n\n"
                                      + "./gradlew "
                                      + yarnUpdateTask.getPath()));
                });
    yarnTask.finalizedBy(yarnWarning);

    CacheableYarnTask buildWeb = project.getTasks().create("buildWeb", CacheableYarnTask.class);
    buildWeb.dependsOn(yarnTask);
    buildWeb.setArgs(ImmutableList.of("run", "build"));
    buildWeb.getInputs().dir("app");
    buildWeb.getInputs().dir("internals");
    // We assume the yarn task correctly handles up-to-date checks for node_modules, so only
    // need to look at yarn.lock here.
    buildWeb.getInputs().file("yarn.lock");
    buildWeb.getOutputs().dir("build");

    Copy copyWeb = project.getTasks().create("copyWeb", Copy.class);
    copyWeb.dependsOn(buildWeb);
    copyWeb.from("build/web");
    project.afterEvaluate(
        p -> {
          ImmutableWebExtension web = project.getExtensions().getByType(WebExtension.class);
          copyWeb.into("build/javaweb/" + web.javaPackage().replace('.', '/'));
        });
  }

  @CacheableTask
  public static class CacheableYarnTask extends YarnTask {}
}
