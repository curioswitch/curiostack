/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.plugins.pulumi;

import static com.google.common.base.Preconditions.checkState;

import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PulumiSetupPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null, "PulumiSetupPlugin can only be applied to the root project.");

    var toolDownloaderPlugin = project.getPlugins().apply(ToolDownloaderPlugin.class);

    toolDownloaderPlugin.registerToolIfAbsent(
        "pulumi",
        tool -> {
          tool.getVersion().set(ToolDependencies.getPulumiVersion(project));
          tool.getBaseUrl().set("https://get.pulumi.com/releases/sdk/");
          tool.getArtifactPattern().set("[artifact]-v[revision]-[classifier].[ext]");

          var osClassifiers = tool.getOsClassifiers();
          osClassifiers.getLinux().set("linux-x64");
          osClassifiers.getMac().set("darwin-x64");
          osClassifiers.getWindows().set("windows-x64");

          switch (new PlatformHelper().getOs()) {
            case LINUX:
            case MAC_OSX:
              tool.getPathSubDirs().add("pulumi");
              break;
            case WINDOWS:
              tool.getPathSubDirs().add("Pulumi/bin");
              break;
            case UNKNOWN:
              throw new IllegalStateException("Unsupported OS");
          }
        });
  }
}
