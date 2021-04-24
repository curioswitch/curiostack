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

package org.curioswitch.gradle.plugins.terraform;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Splitter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.curioswitch.gradle.golang.GolangSetupPlugin;
import org.curioswitch.gradle.golang.tasks.GoTask;
import org.curioswitch.gradle.helpers.platform.PathUtil;
import org.curioswitch.gradle.helpers.task.TaskUtil;
import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Delete;

public class TerraformSetupPlugin implements Plugin<Project> {

  private static final Splitter GITHUB_REPO_SPLITTER = Splitter.on('/');

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null,
        "TerraformSetupPlugin can only be applied to the root project.");
    var toolDownloaderPlugin = project.getPlugins().apply(ToolDownloaderPlugin.class);
    project.getPlugins().apply(GolangSetupPlugin.class);

    var config = TerraformSetupExtension.create(project);

    var terraformCopyPlugins =
        project
            .getTasks()
            .register(
                "terraformCopyPlugins",
                Copy.class,
                t -> {
                  // Must make sure plugins are copied after Terraform is downloaded, otherwise the
                  // download task will find an existing folder and skip the download.
                  t.dependsOn(DownloadToolUtil.getDownloadTask(project, "terraform"));

                  t.into(DownloadedToolManager.get(project).getBinDir("terraform"));
                  t.include("terraform-provider-*");
                });

    toolDownloaderPlugin.registerToolIfAbsent(
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

    toolDownloaderPlugin.registerToolIfAbsent(
        "terraform-provider-gsuite",
        tool -> {
          tool.getVersion().set(ToolDependencies.getTerraformGsuiteProviderVersion(project));
          tool.getBaseUrl()
              .set("https://github.com/DeviaVir/terraform-provider-gsuite" + "/releases/download/");
          tool.getArtifactPattern().set("v[revision]/[artifact]_[revision]_[classifier].[ext]");

          tool.getOsClassifiers().getLinux().set("linux_amd64");
          tool.getOsClassifiers().getMac().set("darwin_amd64");
          tool.getOsClassifiers().getWindows().set("windows_amd64");

          tool.getOsExtensions().getLinux().set("zip");
          tool.getOsExtensions().getMac().set("zip");
          tool.getOsExtensions().getWindows().set("zip");

          tool.getPathSubDirs().add("");
        });

    config.providers(
        providers -> {
          providers.register(
              "terraform-provider-k8s",
              provider -> {
                provider.getGitHubRepo().set("chokoswitch/terraform-provider-k8s");
                provider.getVersion().set("v1.0.1-choko");
              });
          providers.register(
              "terraform-provider-k8s-next",
              provider -> {
                provider.getGitHubRepo().set("mingfang/terraform-provider-k8s");
                provider.getGitHubVersion().set("master");
                provider.getVersion().set("v0.0.1-choko");
              });
        });

    config
        .getProviders()
        // TODO(choko): See if it's possible to do this lazily.
        .all(
            provider -> {
              toolDownloaderPlugin.registerToolIfAbsent(
                  provider.getName(),
                  tool -> {
                    tool.getVersion().set(provider.getVersion());
                    tool.getBaseUrl()
                        .set(
                            "https://github.com/"
                                + provider.getGitHubRepo().get()
                                + "/archive/"
                                + provider.getGitHubVersion().getOrElse(provider.getVersion().get())
                                + ".zip");
                    tool.getArtifactPattern().set("");
                  });

              var sourceSetupTask = DownloadToolUtil.getSetupTask(project, provider.getName());

              String repoName =
                  GITHUB_REPO_SPLITTER.splitToList(provider.getGitHubRepo().get()).get(1);
              // GitHub source archive convention seems to be reponame-tag without
              // leading v.
              String sourceFolder =
                  repoName
                      + '-'
                      + provider
                          .getGitHubVersion()
                          .getOrElse(provider.getVersion().get().substring(1));

              Path sourceDir =
                  DownloadedToolManager.get(project)
                      .getToolDir(provider.getName())
                      .resolve(sourceFolder);
              var buildTask =
                  project
                      .getTasks()
                      .register(
                          "terraformBuild" + TaskUtil.toTaskSuffix(provider.getName()),
                          GoTask.class,
                          t -> {
                            t.dependsOn(sourceSetupTask);

                            String filename =
                                PathUtil.getExeName(
                                    provider.getName() + '_' + provider.getVersion().get());

                            t.args("build", "-o", filename);

                            t.execCustomizer(exec -> exec.workingDir(sourceDir));

                            t.onlyIf(unused -> !Files.exists(sourceDir.resolve(filename)));
                          });

              terraformCopyPlugins.configure(
                  copy -> {
                    copy.dependsOn(buildTask);
                    copy.from(sourceDir);
                  });
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

    terraformCopyPlugins.configure(
        copy -> {
          copy.dependsOn(setupTerraformGsuiteProvider, terraformDeleteOldPlugins);
          copy.from(DownloadedToolManager.get(project).getBinDir("terraform-provider-gsuite"));
        });

    DownloadToolUtil.getSetupTask(project, "terraform")
        .configure(setupTerraform -> setupTerraform.dependsOn(terraformCopyPlugins));

    var setupGo = DownloadToolUtil.getSetupTask(project, "go");
    project.getTasks().withType(GoTask.class).configureEach(t -> t.dependsOn(setupGo));
  }
}
