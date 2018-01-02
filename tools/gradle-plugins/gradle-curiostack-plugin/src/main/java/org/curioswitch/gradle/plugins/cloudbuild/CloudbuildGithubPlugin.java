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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.moowork.gradle.node.NodePlugin;
import com.moowork.gradle.node.yarn.YarnTask;
import java.io.File;
import java.util.Map;
import org.curioswitch.gradle.common.LambdaClosure;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableGcloudExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.process.ExecSpec;

public class CloudbuildGithubPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(NodePlugin.class);

    YarnTask initTask = project.getTasks().create("init", YarnTask.class);
    initTask.setArgs(ImmutableList.of("init", "--yes", "--private"));
    initTask.finalizedBy(
        project
            .getTasks()
            .create(
                "initAddDependency",
                YarnTask.class,
                task -> {
                  task.setArgs(ImmutableList.of("add", "@curiostack/cloudbuild-github"));
                  task.onlyIf(t -> !project.file("package.json").exists());
                }),
        "yarn");
    initTask.onlyIf(t -> !project.file("package.json").exists());

    ImmutableGcloudExtension gcloudConfig =
        project.getRootProject().getExtensions().getByType(GcloudExtension.class);
    Map<String, String> defaultEnvironment =
        ImmutableMap.of("GCLOUD_PROJECT", gcloudConfig.clusterProject());

    YarnTask setupTask = project.getTasks().create("setup", YarnTask.class);
    setupTask.setArgs(ImmutableList.of("run", "cloudbuild-cli", "setup", "--defaults"));
    setupTask.dependsOn(initTask);
    setupTask.onlyIf(t -> !project.file("config.yml").exists());
    setupTask.setExecOverrides(
        LambdaClosure.of(
            ((ExecSpec exec) -> exec.setStandardInput(System.in).setStandardOutput(System.out))));
    setupTask.setEnvironment(defaultEnvironment);

    Map<String, String> environment =
        gcloudConfig.download()
            ? ImmutableMap.<String, String>builder()
                .put(
                    "PATH",
                    gcloudConfig.platformConfig().gcloudBinDir()
                        + File.pathSeparator
                        + System.getenv("PATH"))
                .putAll(defaultEnvironment)
                .build()
            : defaultEnvironment;

    YarnTask deployTask = project.getTasks().create("deploy", YarnTask.class);
    deployTask.setArgs(ImmutableList.of("run", "cloudbuild-cli", "deploy", "--delete"));
    deployTask.dependsOn(setupTask);
    deployTask.setEnvironment(environment);
    deployTask.dependsOn(":gcloudSetup");
  }
}
