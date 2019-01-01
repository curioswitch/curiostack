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

package org.curioswitch.gradle.plugins.codelabs;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.File;
import java.util.List;
import org.curioswitch.gradle.helpers.platform.PathUtil;
import org.curioswitch.gradle.plugins.nodejs.NodePlugin;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Exec;

public class CodelabsPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getRootProject().getPlugins().apply(CodelabsSetupPlugin.class);
    project.getPlugins().apply(BasePlugin.class);
    project.getPlugins().apply(NodePlugin.class);

    var toolManager = DownloadedToolManager.get(project);
    var claatPath = toolManager.getToolDir("claat").resolve(PathUtil.getExeName("claat"));
    var exportDocs =
        project
            .getTasks()
            .register(
                "exportDocs",
                Exec.class,
                t -> {
                  t.getOutputs().dir("build/claat");

                  t.executable(claatPath);

                  var mdFileTree = project.fileTree(".");
                  mdFileTree.exclude("build").include("**/*.md");

                  t.getInputs().files(mdFileTree);

                  List<String> mdFiles =
                      mdFileTree
                          .getFiles()
                          .stream()
                          .map(File::getAbsolutePath)
                          .collect(toImmutableList());

                  t.args("export");
                  t.args(mdFiles);

                  t.workingDir("build/claat");
                });
    var buildSite =
        project
            .getTasks()
            .register(
                "buildSite",
                Exec.class,
                t -> {
                  t.dependsOn(exportDocs);
                  t.getInputs().dir("build/claat");
                  t.getOutputs().dir("build/site");

                  t.executable(claatPath);

                  t.args("build");

                  t.workingDir("build/site");

                  t.doFirst(
                      unused ->
                          project.copy(
                              copy -> {
                                copy.from("build/claat");
                                copy.into("build/site");
                              }));
                });
    project.getTasks().named("assemble").configure(t -> t.dependsOn(buildSite));
  }
}
