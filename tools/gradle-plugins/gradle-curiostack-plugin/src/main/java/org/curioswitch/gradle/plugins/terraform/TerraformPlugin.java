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

package org.curioswitch.gradle.plugins.terraform;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import org.curioswitch.gradle.plugins.curiostack.StandardDependencies;
import org.curioswitch.gradle.plugins.terraform.tasks.ConvertConfigsToJsonTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TerraformImportTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TerraformOutputTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TerraformTask;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderExtension;
import org.curioswitch.gradle.tooldownloader.tasks.DownloadToolTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;

public class TerraformPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(BasePlugin.class);

    @SuppressWarnings("unchecked")
    NamedDomainObjectContainer<ToolDownloaderExtension> tools =
        (NamedDomainObjectContainer<ToolDownloaderExtension>)
            project.getRootProject().getExtensions().getByName("tools");
    var tool = tools.maybeCreate("terraform");
    if (tool.getVersion().getOrNull() == null) {
      tool.getVersion().set(StandardDependencies.TERRAFORM_VERSION);
      tool.getBaseUrl().set("https://releases.hashicorp.com/");
      tool.getArtifactPattern().set("[artifact]/[revision]/[artifact]_[revision]_[classifier].[ext]");

      tool.getOsClassifiers().getLinux().set("linux_amd64");
      tool.getOsClassifiers().getMac().set("darwin_amd64");
      tool.getOsClassifiers().getWindows().set("windows_amd64");

      tool.getOsExtensions().getLinux().set("zip");
      tool.getOsExtensions().getMac().set("zip");
      tool.getOsExtensions().getWindows().set("zip");
    }

    Path plansPath = project.getProjectDir().toPath().resolve("plans");

    var convertConfigs =
        project.getTasks().create(ConvertConfigsToJsonTask.NAME, ConvertConfigsToJsonTask.class);
    convertConfigs.dependsOn(BasePlugin.CLEAN_TASK_NAME);

    var terraformInit =
        project
            .getTasks()
            .create(
                "terraformInit",
                TerraformTask.class,
                t -> t.setArgs(ImmutableList.of("init", "-input=false")));

    var terraformPlan =
        project
            .getTasks()
            .create(
                "terraformPlan",
                TerraformTask.class,
                t -> {
                  t.doFirst(unused -> project.mkdir(plansPath));
                  t.setArgs(ImmutableList.of("plan", "-input=false"));
                  t.dependsOn(terraformInit);
                });

    var terraformApply =
        project
            .getTasks()
            .create(
                "terraformApply",
                TerraformTask.class,
                t -> {
                  t.setArgs(ImmutableList.of("apply"));
                  t.dependsOn(terraformInit);
                });

    project
        .getTasks()
        .create(
            "terraformCopyState",
            TerraformTask.class,
            t -> {
              t.setArgs(ImmutableList.of("init", "-input=false", "-force-copy"));
              t.dependsOn(terraformInit);
            });

    project
        .getTasks()
        .create(
            TerraformImportTask.NAME,
            TerraformImportTask.class,
            t -> {
              t.dependsOn(terraformInit);
              t.setArgs(ImmutableList.of("import", "-input=false"));
            });

    project
        .getTasks()
        .create(
            TerraformOutputTask.NAME,
            TerraformOutputTask.class,
            t -> {
              t.dependsOn(terraformInit);
              t.setArgs(ImmutableList.of("output"));
            });

    var downloadTerraformTask = project
        .getRootProject()
        .getTasks()
        .withType(DownloadToolTask.class)
        .named("toolsDownloadTerraform");
    project
        .getTasks()
        .withType(
            TerraformTask.class, t -> t.dependsOn(downloadTerraformTask, convertConfigs));
  }
}
