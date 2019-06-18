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
import org.curioswitch.gradle.golang.GolangSetupPlugin;
import org.curioswitch.gradle.golang.tasks.GoTask;
import org.curioswitch.gradle.helpers.platform.PathUtil;
import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Delete;

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
                    tool.getVersion().set(ToolDependencies.getTerraformVersion(project));
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
                    tool.getVersion().set(ToolDependencies.getHelmVersion(project));
                    tool.getBaseUrl().set("https://storage.googleapis.com/kubernetes-helm/");
                    tool.getArtifactPattern().set("[artifact]-v[revision]-[classifier].[ext]");
                  });

              plugin.registerToolIfAbsent(
                  "terraform-provider-gsuite",
                  tool -> {
                    tool.getVersion()
                        .set(ToolDependencies.getTerraformGsuiteProviderVersion(project));
                    tool.getBaseUrl()
                        .set(
                            "https://github.com/DeviaVir/terraform-provider-gsuite"
                                + "/releases/download/");
                    tool.getArtifactPattern()
                        .set("v[revision]/[artifact]_[revision]_[classifier].[ext]");

                    tool.getOsClassifiers().getLinux().set("linux_amd64");
                    tool.getOsClassifiers().getMac().set("darwin_amd64");
                    tool.getOsClassifiers().getWindows().set("windows_amd64");

                    tool.getOsExtensions().getLinux().set("zip");
                    tool.getOsExtensions().getMac().set("zip");
                    tool.getOsExtensions().getWindows().set("zip");

                    tool.getPathSubDirs().add("");
                  });

              plugin.registerToolIfAbsent(
                  "terraform-provider-kubernetes-choko",
                  tool -> {
                    tool.getVersion().set("1.7.1-choko");
                    tool.getBaseUrl()
                        .set(
                            "https://github.com/chokoswitch/terraform-provider-kubernetes/archive/v1.7.1-choko.zip");
                    tool.getArtifactPattern().set("");
                  });

              plugin.registerToolIfAbsent(
                  "terraform-provider-k8s-choko",
                  tool -> {
                    tool.getVersion().set("1.0.1-choko");
                    tool.getBaseUrl()
                        .set(
                            "https://github.com/chokoswitch/terraform-provider-k8s/archive/v1.0.1-choko.zip");
                    tool.getArtifactPattern().set("");
                  });

              plugin.registerToolIfAbsent(
                  "terraform-provider-k8s-next-choko",
                  tool -> {
                    tool.getVersion().set("0.0.1-choko");
                    tool.getBaseUrl()
                        .set(
                            "https://github.com/chokoswitch/terraform-provider-k8s-1/archive/v0.0.1-choko.zip");
                    tool.getArtifactPattern().set("");
                  });
            });

    var terraformBuildK8s =
        project
            .getTasks()
            .register(
                "terraformBuildK8sProvider",
                GoTask.class,
                t -> {
                  t.dependsOn(
                      DownloadToolUtil.getSetupTask(project, "terraform-provider-k8s-choko"));
                  t.args("build", "-o", PathUtil.getExeName("terraform-provider-k8s_v1.0.1-choko"));
                  t.execCustomizer(
                      exec ->
                          exec.workingDir(
                              DownloadedToolManager.get(project)
                                  .getToolDir("terraform-provider-k8s-choko")
                                  .resolve("terraform-provider-k8s-1.0.1-choko")));
                  t.onlyIf(
                      unused ->
                          !Files.exists(
                              DownloadedToolManager.get(project)
                                  .getToolDir("terraform-provider-k8s-choko")
                                  .resolve("terraform-provider-k8s-1.0.1-choko")
                                  .resolve(
                                      PathUtil.getExeName("terraform-provider-k8s_v1.0.1-choko"))));
                });

    var terraformBuildKubernetesFork =
        project
            .getTasks()
            .register(
                "terraformBuildKubernetesForkProvider",
                GoTask.class,
                t -> {
                  t.dependsOn(
                      DownloadToolUtil.getSetupTask(
                          project, "terraform-provider-kubernetes-choko"));
                  t.args(
                      "build",
                      "-o",
                      PathUtil.getExeName("terraform-provider-kubernetes_v1.7.1-choko"));
                  t.execCustomizer(
                      exec ->
                          exec.workingDir(
                              DownloadedToolManager.get(project)
                                  .getToolDir("terraform-provider-kubernetes-choko")
                                  .resolve("terraform-provider-kubernetes-1.7.1-choko")));
                  t.onlyIf(
                      unused ->
                          !Files.exists(
                              DownloadedToolManager.get(project)
                                  .getToolDir("terraform-provider-kubernetes-choko")
                                  .resolve("terraform-provider-kubernetes-1.7.1-choko")
                                  .resolve(
                                      PathUtil.getExeName(
                                          "terraform-provider-kubernetes_v1.7.1-choko"))));
                });

    var terraformBuildK8sNext =
        project
            .getTasks()
            .register(
                "terraformBuildK8sNextProvider",
                GoTask.class,
                t -> {
                  t.dependsOn(
                      DownloadToolUtil.getSetupTask(
                          project, "terraform-provider-k8s-next-choko"));
                  t.args(
                      "build",
                      "-o",
                      PathUtil.getExeName("terraform-provider-k8s-next_v0.0.1-choko"));
                  t.execCustomizer(
                      exec ->
                          exec.workingDir(
                              DownloadedToolManager.get(project)
                                  .getToolDir("terraform-provider-k8s-next-choko")
                                  .resolve("terraform-provider-k8s-1-0.0.1-choko")));
                  t.onlyIf(
                      unused ->
                          !Files.exists(
                              DownloadedToolManager.get(project)
                                  .getToolDir("terraform-provider-k8s-next-choko")
                                  .resolve("terraform-provider-k8s-1-0.0.1-choko")
                                  .resolve(
                                      PathUtil.getExeName(
                                          "terraform-provider-k8s-next_v0.0.1-choko"))));
                });

    var setupTerraformGsuiteProvider =
        DownloadToolUtil.getSetupTask(project, "terraform-provider-gsuite");

    var terraformDeleteOldPlugins =
        project
            .getTasks()
            .register(
                "terraformDeleteOldPlugins",
                Delete.class,
                t -> {
                  var terraformDir = DownloadedToolManager.get(project).getBinDir("terraform");
                  t.delete(
                      terraformDir.resolve(PathUtil.getExeName("terraform-provider-kubernetes")));
                  t.delete(terraformDir.resolve(PathUtil.getExeName("terraform-provider-k8s")));
                  t.delete(terraformDir.resolve(PathUtil.getExeName("terraform-provider-gsuite")));
                });

    var terraformCopyPlugins =
        project
            .getTasks()
            .register(
                "terraformCopyPlugins",
                Copy.class,
                t -> {
                  t.dependsOn(
                      terraformBuildK8s,
                      terraformBuildK8sNext,
                      setupTerraformGsuiteProvider,
                      terraformBuildKubernetesFork,
                      terraformDeleteOldPlugins);
                  t.into(DownloadedToolManager.get(project).getBinDir("terraform"));
                  t.from(DownloadedToolManager.get(project).getBinDir("terraform-provider-gsuite"));
                  t.from(
                      DownloadedToolManager.get(project)
                          .getToolDir("terraform-provider-kubernetes-choko")
                          .resolve("terraform-provider-kubernetes-1.7.1-choko"));
                  t.from(
                      DownloadedToolManager.get(project)
                          .getToolDir("terraform-provider-k8s-choko")
                          .resolve("terraform-provider-k8s-1.0.1-choko"));
                  t.from(
                      DownloadedToolManager.get(project)
                          .getToolDir("terraform-provider-k8s-next-choko")
                          .resolve("terraform-provider-k8s-1-0.0.1-choko"));
                  t.include("terraform-provider-*");
                });

    DownloadToolUtil.getSetupTask(project, "terraform")
        .configure(setupTerraform -> setupTerraform.dependsOn(terraformCopyPlugins));

    var setupGo = DownloadToolUtil.getSetupTask(project, "go");
    project.getTasks().withType(GoTask.class).configureEach(t -> t.dependsOn(setupGo));
  }
}
