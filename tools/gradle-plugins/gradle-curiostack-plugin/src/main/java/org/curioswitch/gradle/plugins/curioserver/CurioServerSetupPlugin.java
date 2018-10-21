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

package org.curioswitch.gradle.plugins.curioserver;

import static com.google.common.base.Preconditions.checkState;

import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CurioServerSetupPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null, "CurioServerSetupPlugin can only be applied to base project.");

    project.getPlugins().apply(ToolDownloaderPlugin.class);

    project
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin ->
                plugin.registerToolIfAbsent(
                    "graalvm",
                    tool -> {
                      tool.getVersion().set("1.0.0-rc8");
                      tool.getBaseUrl().set("https://github.com/oracle/graal/releases/download/");
                      tool.getArtifactPattern()
                          .set("vm-[revision]/[artifact]-ce-[revision]-[classifier].[ext]");

                      // Just use the linux binary to try the plugin on it, it'll fail at
                      // runtime since Windows isn't supported yet.
                      tool.getOsClassifiers().getWindows().set("linux-amd64");
                      tool.getOsExtensions().getWindows().set("tar.gz");

                      var pathBase = tool.getVersion().map(v -> "graalvm-ce-" + v);
                      switch (new PlatformHelper().getOs()) {
                        case LINUX:
                        case WINDOWS:
                          tool.getPathSubDirs().add(pathBase.map(p -> p + "/bin"));
                          break;
                        case MAC_OSX:
                          tool.getPathSubDirs().add(pathBase.map(p -> p + "/Contents/Home/bin"));
                          break;
                      }
                    }));
  }
}
