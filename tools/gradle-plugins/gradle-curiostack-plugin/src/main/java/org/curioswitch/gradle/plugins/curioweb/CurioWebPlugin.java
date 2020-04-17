/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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

import com.google.common.collect.ImmutableMap;
import org.curioswitch.gradle.plugins.curioweb.tasks.CopyWebTask;
import org.curioswitch.gradle.plugins.nodejs.NodePlugin;
import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

/**
 * A simple {@link Plugin} which configures a project with tasks for building a static npm web
 * application and bundling it into a jar for serving from a Java server.
 */
public class CurioWebPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    var config = WebExtension.createAndAdd(project);

    project.getPlugins().apply(JavaLibraryPlugin.class);
    project.getPlugins().apply(NodePlugin.class);

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
            .getTasks()
            .register(
                "buildWeb",
                NodeTask.class,
                t -> {
                  t.dependsOn(project.getRootProject().getTasks().findByName("yarn"));
                  t.args("run", "build");

                  t.getInputs().dir(project.file("src"));
                  t.getInputs().file(project.file("package.json"));

                  var javaConvention =
                      project.getConvention().getPlugin(JavaPluginConvention.class);

                  t.getInputs()
                      .files(
                          javaConvention
                              .getSourceSets()
                              .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                              .getCompileClasspath());

                  // We assume the yarn task correctly handles up-to-date checks for node_modules,
                  // so only need to look at yarn.lock here.
                  t.getInputs().file(project.getRootProject().file("yarn.lock"));
                  t.getOutputs().dirs(ImmutableMap.of("yarnBuild", project.file("build/web")));
                  if ("true"
                      .equals(project.getRootProject().findProperty("curiostack.skipBuildWeb"))) {
                    t.setEnabled(false);
                  } else {
                    t.finalizedBy(buildWebCheck);
                  }
                });

    project
        .getTasks()
        .register(
            "copyWeb",
            CopyWebTask.class,
            t -> {
              t.dependsOn(buildWeb);

              t.getJavaPackage().set(config.getJavaPackage());
              t.getStorybookJavaPackage().set(config.getStorybookJavaPackage());
            });
  }
}
