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

import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Files;
import java.nio.file.Path;
import org.curioswitch.gradle.golang.GolangSetupPlugin;
import org.curioswitch.gradle.golang.tasks.GoTask;
import org.curioswitch.gradle.helpers.platform.PathUtil;
import org.curioswitch.gradle.plugins.curiostack.StandardDependencies;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.Copy;

public class TerraformSetupPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null,
        "TerraformSetupPlugin can only be applied to the root project.");
    project.getPlugins().apply(ToolDownloaderPlugin.class);
    project.getPlugins().apply(GolangSetupPlugin.class);

    project
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin -> {
              plugin.registerToolIfAbsent(
                  "terraform",
                  tool -> {
                    tool.getVersion().set(StandardDependencies.TERRAFORM_VERSION);
                    tool.getBaseUrl().set("https://releases.hashicorp.com/");
                    tool.getArtifactPattern()
                        .set("[artifact]/[revision]/[artifact]_[revision]_[classifier].[ext]");

                    tool.getOsClassifiers().getLinux().set("linux_amd64");
                    tool.getOsClassifiers().getMac().set("darwin_amd64");
                    tool.getOsClassifiers().getWindows().set("windows_amd64");

                    tool.getOsExtensions().getLinux().set("zip");
                    tool.getOsExtensions().getMac().set("zip");
                    tool.getOsExtensions().getWindows().set("zip");

                    tool.getPathSubDirs().add("");
                  });

              plugin.registerToolIfAbsent(
                  "helm",
                  tool -> {
                    tool.getVersion().set(StandardDependencies.HELM_VERSION);
                    tool.getBaseUrl().set("https://storage.googleapis.com/kubernetes-helm/");
                    tool.getArtifactPattern().set("[artifact]-v[revision]-[classifier].[ext]");
                  });
            });

    var goBinDir =
        ((Path)
                project
                    .getRootProject()
                    .getExtensions()
                    .getByType(ExtraPropertiesExtension.class)
                    .get("gopath"))
            .resolve("bin");
    var terraformDownloadK8s =
        project
            .getTasks()
            .register(
                "terraformDownloadK8sProvider",
                GoTask.class,
                t -> {
                  t.args("get", "github.com/ericchiang/terraform-provider-k8s");
                  t.onlyIf(
                      unused ->
                          !Files.exists(
                              goBinDir.resolve(PathUtil.getExeName("terraform-provider-k8s"))));
                });

    var terraformDownloadKubernetesFork =
        project
            .getTasks()
            .register(
                "terraformDownloadKubernetesForkProvider",
                GoTask.class,
                t -> {
                  t.args("get", "github.com/sl1pm4t/terraform-provider-kubernetes");
                  t.onlyIf(
                      unused ->
                          !Files.exists(
                              goBinDir.resolve(
                                  PathUtil.getExeName("terraform-provider-kubernetes"))));
                });

    var terraformDownloadGsuite =
        project
            .getTasks()
            .register(
                "terraformDownloadGsuiteProvider",
                GoTask.class,
                t -> {
                  t.args("get", "github.com/DeviaVir/terraform-provider-gsuite");
                  t.onlyIf(
                      unused ->
                          !Files.exists(
                              goBinDir.resolve(PathUtil.getExeName("terraform-provider-gsuite"))));
                });

    var terraformCopyPlugins =
        project
            .getTasks()
            .register(
                "terraformCopyPlugins",
                Copy.class,
                t -> {
                  t.dependsOn(
                      terraformDownloadK8s,
                      terraformDownloadKubernetesFork,
                      terraformDownloadGsuite);
                  t.into(DownloadedToolManager.get(project).getBinDir("terraform"));
                  t.from(goBinDir);
                  t.include("terraform-provider-*");
                });

    DownloadToolUtil.getSetupTask(project, "terraform")
        .configure(setupTerraform -> setupTerraform.dependsOn(terraformCopyPlugins));
  }
}
