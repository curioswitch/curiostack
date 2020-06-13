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
package org.curioswitch.gradle.conda;

import static com.google.common.base.Preconditions.checkState;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.curioswitch.gradle.conda.tasks.InstallCondaPackagesTask;
import org.curioswitch.gradle.conda.tasks.InstallPythonPackagesTask;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.helpers.task.TaskUtil;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CondaPlugin implements Plugin<Project> {

  @MonotonicNonNull private NamedDomainObjectContainer<CondaExtension> condas;

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null,
        "gradle-conda-plugin can only be applied to the root project.");

    project.getPlugins().apply(ToolDownloaderPlugin.class);

    condas =
        project.container(
            CondaExtension.class, name -> CondaExtension.create(name, project.getObjects()));
    project.getExtensions().add("conda", condas);

    condas.configureEach(conda -> addCondaTasks(project, conda));
  }

  public NamedDomainObjectContainer<CondaExtension> getCondas() {
    return condas;
  }

  private static void addCondaTasks(Project project, CondaExtension conda) {
    var operatingSystem = new PlatformHelper().getOs();
    project
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin -> {
              plugin.registerToolIfAbsent(
                  conda.getName(),
                  tool -> {
                    tool.getArtifact().set(conda.getName());
                    tool.getVersion().set(conda.getVersion());
                    tool.getBaseUrl().set("https://repo.continuum.io/miniconda/");
                    tool.getArtifactPattern().set("[revision]-[classifier].[ext]");

                    if (operatingSystem == OperatingSystem.WINDOWS) {
                      tool.getPathSubDirs().addAll("", "Scripts", "Library/bin");
                    } else {
                      tool.getPathSubDirs().add("bin");
                    }

                    var osClassifiers = tool.getOsClassifiers();
                    osClassifiers.getLinux().set("Linux-x86_64");
                    osClassifiers.getMac().set("MacOSX-x86_64");
                    osClassifiers.getWindows().set("Windows-x86_64");

                    var osExtensions = tool.getOsExtensions();
                    osExtensions.getLinux().set("sh");
                    osExtensions.getMac().set("sh");
                    osExtensions.getWindows().set("exe");
                  });
              var download = DownloadToolUtil.getDownloadTask(project, conda.getName());
              download.configure(
                  t ->
                      t.setArchiveExtractAction(
                          archive -> {
                            archive.setExecutable(true);
                            var toolDir = plugin.toolManager().getToolDir(conda.getName());
                            if (operatingSystem == OperatingSystem.WINDOWS) {
                              project.exec(
                                  exec -> {
                                    exec.executable("cmd");
                                    exec.args(
                                        "/k",
                                        "start /wait "
                                            + archive.getAbsolutePath()
                                            + " /S /InstallationType=JustMe /AddToPath=0 "
                                            + "/RegisterPython=0 /NoRegistry=1 /D="
                                            + toolDir.toAbsolutePath().toString());
                                  });
                            } else {
                              project.exec(
                                  exec -> {
                                    exec.executable(archive);
                                    exec.args(
                                        "-f", "-b", "-p", toolDir.toAbsolutePath().toString());
                                  });
                            }
                          }));
              var condaInstallPackages =
                  project
                      .getTasks()
                      .register(
                          "condaInstallPackages" + TaskUtil.toTaskSuffix(conda.getName()),
                          InstallCondaPackagesTask.class,
                          conda,
                          plugin.toolManager());
              condaInstallPackages.configure(t -> t.dependsOn(download));
              var condaInstallPythonPackages =
                  project
                      .getTasks()
                      .register(
                          "condaInstallPythonPackages" + TaskUtil.toTaskSuffix(conda.getName()),
                          InstallPythonPackagesTask.class,
                          conda,
                          plugin.toolManager());
              condaInstallPythonPackages.configure(t -> t.dependsOn(condaInstallPackages));
              DownloadToolUtil.getSetupTask(project, conda.getName())
                  .configure(t -> t.dependsOn(condaInstallPackages, condaInstallPythonPackages));
            });
  }
}
