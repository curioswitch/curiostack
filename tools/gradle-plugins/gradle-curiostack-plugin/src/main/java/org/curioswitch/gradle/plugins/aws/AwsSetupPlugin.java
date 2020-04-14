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
package org.curioswitch.gradle.plugins.aws;

import static com.google.common.base.Preconditions.checkState;

import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AwsSetupPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    checkState(project.getParent() == null, "AwsSetupPlugin must be applied to root project.");
    var toolDownloaderPlugin = project.getPlugins().apply(ToolDownloaderPlugin.class);

    var operatingSystem = new PlatformHelper().getOs();

    toolDownloaderPlugin.registerToolIfAbsent(
        "awscli",
        tool -> {
          tool.getVersion().set(ToolDependencies.getAwsCliVersion(project));
          tool.getBaseUrl().set("https://awscli.amazonaws.com/");
          tool.getArtifactPattern().set("[artifact].[ext]");
          if (operatingSystem == OperatingSystem.LINUX) {
            tool.getArtifact().set("awscli-exe-linux-x86_64");
          } else {
            tool.getArtifact().set("AWSCLIV2");
          }
          var osExtensions = tool.getOsExtensions();
          osExtensions.getLinux().set("zip");
          osExtensions.getMac().set("pkg");
          osExtensions.getWindows().set("msi");

          if (operatingSystem == OperatingSystem.WINDOWS) {
            tool.getPathSubDirs().add("Amazon/AWSCLIV2");
          }
        });
    if (operatingSystem != OperatingSystem.LINUX) {
      var downloadTool = DownloadToolUtil.getDownloadTask(project, "awscli");
      downloadTool.configure(
          t -> {
            t.setArchiveExtractAction(
                archive -> {
                  var toolDir = toolDownloaderPlugin.toolManager().getToolDir("awscli");
                  if (operatingSystem == OperatingSystem.WINDOWS) {
                    project.exec(
                        exec -> {
                          exec.executable("cmd");
                          exec.args(
                              "/c",
                              "start /wait msiexec /a "
                                  + archive.getAbsolutePath()
                                  + " TARGETDIR="
                                  + toolDir.toAbsolutePath().toString()
                                  + " /qn");
                        });
                  }
                });
          });
    }
  }
}
