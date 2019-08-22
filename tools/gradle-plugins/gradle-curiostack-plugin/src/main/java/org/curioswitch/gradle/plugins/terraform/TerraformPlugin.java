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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.curioswitch.gradle.plugins.terraform.tasks.ConvertConfigsToJsonTask;
import org.curioswitch.gradle.plugins.terraform.tasks.HelmTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TargetableTerraformPlanTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TargetableTerraformTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TerraformImportTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TerraformOutputTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TerraformTask;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;

public class TerraformPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getRootProject().getPlugins().apply(TerraformSetupPlugin.class);

    project.getPlugins().apply(BasePlugin.class);

    var config = TerraformExtension.create(project);

    var convertConfigs =
        project.getTasks().create(ConvertConfigsToJsonTask.NAME, ConvertConfigsToJsonTask.class);
    var copyDependencies =
        project
            .getTasks()
            .register("terraformCopyDependencies", Copy.class, t -> t.into("build/terraform"));
    project.afterEvaluate(
        unused -> {
          for (var dependencyPath : config.getDependencies().get()) {
            var depedendency = project.findProject(dependencyPath);
            if (depedendency == null) {
              throw new IllegalStateException(
                  "Could not find dependency project with path  " + dependencyPath);
            }
            copyDependencies.configure(
                t -> {
                  t.dependsOn(depedendency.getTasks().withType(ConvertConfigsToJsonTask.class));
                  t.from(
                      depedendency.file("build/terraform"),
                      copy -> copy.into(depedendency.getName()));
                });
          }
        });
    convertConfigs.dependsOn(BasePlugin.CLEAN_TASK_NAME, copyDependencies);

    List<TaskProvider<?>> sysadminOutputTasks =
        project.getPath().contains(":sysadmin")
            ? ImmutableList.of(
                createTerraformOutputTask(
                    project,
                    "outputTillerCaCert",
                    "tiller-ca-cert",
                    project.file("build/helm/ca.pem")),
                createTerraformOutputTask(
                    project,
                    "outputTillerCertKey",
                    "tiller-client-key",
                    project.file("build/helm/key.pem")),
                createTerraformOutputTask(
                    project,
                    "outputTillerCert",
                    "tiller-client-cert",
                    project.file("build/helm/cert.pem")),
                project
                    .getTasks()
                    .register(
                        "terraformInitHelm",
                        HelmTask.class,
                        helm -> helm.args("init", "--client-only")))
            : ImmutableList.of();

    var gcloudInstallComponents =
        project.getRootProject().getTasks().named("gcloudInstallComponents");

    var terraformInit =
        project
            .getTasks()
            .register(
                "terraformInit",
                TerraformTask.class,
                t -> {
                  t.setArgs(ImmutableList.of("init", "-input=false"));
                  t.setExecCustomizer(
                      exec ->
                          exec.environment(
                              "TF_PLUGIN_CACHE_DIR",
                              DownloadedToolManager.get(project)
                                  .getCuriostackDir()
                                  .resolve("terraform-plugins")));
                  sysadminOutputTasks.forEach(t::finalizedBy);

                  t.dependsOn(gcloudInstallComponents);
                });

    project
        .getTasks()
        .register(
            "terraformPlan",
            TargetableTerraformPlanTask.class,
            t -> {
              t.setArgs(ImmutableList.of("plan", "-input=false"));
              t.dependsOn(terraformInit);
            });

    project
        .getTasks()
        .register(
            "terraformApply",
            TargetableTerraformTask.class,
            t -> {
              t.setArgs(ImmutableList.of("apply"));
              t.dependsOn(terraformInit);
            });

    project
        .getTasks()
        .register(
            "terraformCopyState",
            TerraformTask.class,
            t -> {
              t.setArgs(ImmutableList.of("init", "-input=false", "-force-copy"));
              t.dependsOn(terraformInit);
            });

    project
        .getTasks()
        .register(
            TerraformImportTask.NAME,
            TerraformImportTask.class,
            t -> {
              t.dependsOn(terraformInit);
              t.setArgs(ImmutableList.of("import", "-input=false"));
            });

    project
        .getTasks()
        .register(
            TerraformOutputTask.NAME,
            TerraformOutputTask.class,
            t -> {
              t.dependsOn(terraformInit);
              t.setArgs(ImmutableList.of("output"));
            });

    var setupTerraform = DownloadToolUtil.getSetupTask(project, "terraform");
    project
        .getTasks()
        .withType(TerraformTask.class)
        .configureEach(t -> t.dependsOn(setupTerraform, convertConfigs));

    project
        .getTasks()
        .withType(HelmTask.class)
        .configureEach(t -> t.dependsOn(DownloadToolUtil.getSetupTask(project, "helm")));
  }

  private static TaskProvider<TerraformOutputTask> createTerraformOutputTask(
      Project project, String taskName, String outputName, File outputFile) {
    return project
        .getTasks()
        .register(
            taskName,
            TerraformOutputTask.class,
            t -> {
              t.doFirst(unused -> project.mkdir(outputFile.getParent()));
              t.setArgs(ImmutableList.of("output", outputName));
              t.dependsOn(project.getTasks().getByName("terraformInit"));
              t.setExecCustomizer(
                  exec -> {
                    exec.setIgnoreExitValue(true);
                    try {
                      exec.setStandardOutput(new FileOutputStream(outputFile));
                    } catch (FileNotFoundException e) {
                      throw new UncheckedIOException("Could not open file.", e);
                    }
                  });
            });
  }
}
