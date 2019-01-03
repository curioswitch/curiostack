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

import org.curioswitch.gradle.plugins.codelabs.tasks.DownloadDepsTask;
import org.curioswitch.gradle.plugins.codelabs.tasks.ExportDocsTask;
import org.curioswitch.gradle.plugins.nodejs.NodePlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Copy;

public class CodelabsPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getRootProject().getPlugins().apply(CodelabsSetupPlugin.class);
    project.getPlugins().apply(BasePlugin.class);
    project.getPlugins().apply(NodePlugin.class);

    var setupClaat = DownloadToolUtil.getSetupTask(project, "claat");
    var exportDocs =
        project
            .getTasks()
            .register(
                "exportDocs",
                ExportDocsTask.class,
                t -> {
                  t.dependsOn(setupClaat);

                  var mdFileTree = project.fileTree("src");
                  mdFileTree.exclude("build").include("**/*.md");

                  t.getMdFiles().set(mdFileTree);
                  t.getOutputDir().set(project.file("build/exported"));
                });

    var downloadDeps =
        project
            .getTasks()
            .register(
                "downloadDeps",
                DownloadDepsTask.class,
                t -> {
                  t.dependsOn(setupClaat);

                  t.getDepsVersion().set(1);
                  t.getOutputDir().set(project.file("build/deps"));
                });

    var assembleSite =
        project
            .getTasks()
            .register(
                "assembleSite",
                Copy.class,
                t -> {
                  t.dependsOn(exportDocs, downloadDeps);

                  t.from("build/deps", "build/exported");
                  t.into("build/site");
                });

    project.getTasks().named("assemble").configure(t -> t.dependsOn(assembleSite));
  }
}
