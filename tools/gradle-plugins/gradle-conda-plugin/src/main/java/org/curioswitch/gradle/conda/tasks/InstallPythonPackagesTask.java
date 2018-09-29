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

package org.curioswitch.gradle.conda.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import org.curioswitch.gradle.conda.CondaExtension;
import org.curioswitch.gradle.conda.exec.CondaExecUtil;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public class InstallPythonPackagesTask extends DefaultTask {

  private final String name;

  private final ListProperty<String> packages;

  private final DownloadedToolManager toolManager;

  @Inject
  public InstallPythonPackagesTask(CondaExtension conda, DownloadedToolManager toolManager) {
    name = conda.getName();
    packages = getProject().getObjects().listProperty(String.class);
    this.toolManager = toolManager;

    packages.set(conda.getPythonPackages());

    onlyIf(
        unused -> {
          var packages = this.packages.get();
          if (packages.isEmpty()) {
            return false;
          }

          Path minicondaDir = toolManager.getToolDir("miniconda2-build");
          final Path sitePackagesDir;
          if (new PlatformHelper().getOs() == OperatingSystem.WINDOWS) {
            sitePackagesDir = minicondaDir.resolve("lib/site-packages");
          } else {
            sitePackagesDir = minicondaDir.resolve("lib/python2.7/site-packages");
          }

          return packages.stream().anyMatch(pkg -> !Files.exists(sitePackagesDir.resolve(pkg)));
        });
  }

  @Input
  ListProperty<String> getPackages() {
    return packages;
  }

  @TaskAction
  void exec() {
    Path toolDir = toolManager.getToolDir(name);
    getProject()
        .exec(
            exec -> {
              OperatingSystem operatingSystem = new PlatformHelper().getOs();
              if (operatingSystem == OperatingSystem.WINDOWS) {
                exec.executable(toolDir.resolve("Scripts").resolve("pip.exe"));
              } else {
                exec.executable(toolDir.resolve("bin").resolve("pip"));
              }

              exec.args("install");
              exec.args(packages.get());
              toolManager.addAllToPath(exec);
              CondaExecUtil.condaExec(exec, toolManager, name);
            });
  }
}
