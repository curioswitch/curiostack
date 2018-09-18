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

package org.curioswitch.gradle.plugins.cloudbuild;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableGcloudExtension;
import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CloudbuildGithubPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    var initAddDependency =
        project
            .getTasks()
            .register(
                "initAddDependency",
                NodeTask.class,
                t -> {
                  t.args("add", "@curiostack/cloudbuild-github");
                  t.onlyIf(unused -> !project.file("package.json").exists());
                });

    var initTask =
        project
            .getTasks()
            .register(
                "init",
                NodeTask.class,
                t -> {
                  t.args("init", "--yes", "--private");
                  t.finalizedBy(initAddDependency);
                  t.onlyIf(unused -> !project.file("package.json").exists());
                });

    ImmutableGcloudExtension gcloudConfig =
        project.getRootProject().getExtensions().getByType(GcloudExtension.class);
    Map<String, String> defaultEnvironment =
        ImmutableMap.of("GCLOUD_PROJECT", gcloudConfig.clusterProject());

    var setupTask =
        project
            .getTasks()
            .register(
                "setup",
                NodeTask.class,
                t -> {
                  t.args("run", "cloudbuild-cli", "setup", "--defaults");
                  t.dependsOn(initTask);
                  t.onlyIf(unused -> !project.file("config.yml").exists());
                  t.execOverride(
                      exec -> {
                        exec.setStandardInput(System.in).setStandardOutput(System.out);
                        exec.environment(defaultEnvironment);
                      });
                });

    project
        .getTasks()
        .create(
            "deploy",
            NodeTask.class,
            t -> {
              t.args("run", "cloudbuild-cli", "deploy", "--delete");
              t.dependsOn(setupTask, DownloadToolUtil.getSetupTask(project, "gcloud"));
              t.execOverride(exec -> DownloadedToolManager.get(project).addAllToPath(exec));
            });
  }
}
