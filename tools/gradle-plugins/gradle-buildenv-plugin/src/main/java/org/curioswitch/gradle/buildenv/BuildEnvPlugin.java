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

package org.curioswitch.gradle.buildenv;

import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.tasks.DownloadToolTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BuildEnvPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin -> {
              plugin.registerToolIfAbsent(
                  "Miniconda2",
                  tool -> {
                    tool.getVersion().set("4.5.11");
                    tool.getBaseUrl().set("https://repo.continuum.io/miniconda/");
                    tool.getArtifactPattern().set("[artifact]-[revision]-[classifier].[ext]");

                    var osClassifiers = tool.getOsClassifiers();
                    osClassifiers.getLinux().set("Linux-x86_64");
                    osClassifiers.getMac().set("MacOSX-x86_64");
                    osClassifiers.getWindows().set("Windows-x86_64");

                    var osExtensions = tool.getOsExtensions();
                    osExtensions.getLinux().set("sh");
                    osExtensions.getMac().set("sh");
                    osExtensions.getWindows().set("exe");
                  });
              project
                  .getTasks()
                  .withType(DownloadToolTask.class)
                  .named("toolsDownloadMiniconda2")
                  .configure(
                      t ->
                          t.setArchiveExtractAction(
                              archive -> {
                                archive.setExecutable(true);
                                var toolDir = plugin.toolManager().getToolDir("Miniconda2");
                                PlatformHelper helper = new PlatformHelper();
                                if (helper.getOs() == OperatingSystem.WINDOWS) {
                                  project.exec(
                                      exec -> {
                                        exec.executable(archive);
                                        exec.args(
                                            "/S",
                                            "/InstallationType=JustMe",
                                            "/AddToPath=0",
                                            "/RegisterPython=0",
                                            "/NoRegistry=1",
                                            "/D=" + toolDir.toAbsolutePath().toString());
                                      });
                                }
                              }));
            });
  }
}
