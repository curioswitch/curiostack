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

package org.curioswitch.gradle.plugins.gcloud;

import java.util.Arrays;
import java.util.List;
import org.curioswitch.gradle.plugins.gcloud.tasks.GcloudTask;
import org.curioswitch.gradle.plugins.gcloud.tasks.SetupTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Rule;

/**
 * A plugin that adds tasks for automatically downloading the gcloud sdk and running commands using
 * it from gradle. Python 2 will have to be available for gcloud sdk commands to work. Eventually,
 * most commands should be migrated to using the gcloud Rest APIs to remove this dependency.
 */
public class GcloudPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getExtensions().create(ImmutableGcloudExtension.NAME, GcloudExtension.class, project);

    project
        .getExtensions()
        .getExtraProperties()
        .set(GcloudTask.class.getSimpleName(), GcloudTask.class);

    project
        .getTasks()
        .addRule(
            new Rule() {
              @Override
              public String getDescription() {
                return "Pattern: \"gcloud_<command>\": Executes a Gcloud command.";
              }

              @Override
              public void apply(String taskName) {
                if (taskName.startsWith("gcloud_")) {
                  GcloudTask task = project.getTasks().create(taskName, GcloudTask.class);
                  List<String> tokens = Arrays.asList(taskName.split("_"));
                  task.setArgs(tokens.subList(1, tokens.size()));
                }
              }
            });

    project.afterEvaluate(
        p -> {
          SetupTask setupTask = project.getTasks().create(SetupTask.NAME, SetupTask.class);
          ImmutableGcloudExtension config =
              project.getExtensions().getByType(GcloudExtension.class);
          setupTask.setEnabled(config.download());

          GcloudTask createClusterProject =
              project.getTasks().create("gcloudCreateClusterProject", GcloudTask.class);
          createClusterProject.setArgs(
              Arrays.asList("alpha", "projects", "create", config.clusterProject()));

          // This task currently always fails, probably due to a bug in the SDK. It attempts
          // to create the same repo twice, and the second one fails with an error...
          GcloudTask createSourceRepo =
              project.getTasks().create("gcloudCreateSourceRepository", GcloudTask.class);
          createSourceRepo.setArgs(
              Arrays.asList("alpha", "source", "repos", "create", config.sourceRepository()));
        });
  }
}
