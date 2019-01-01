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

package org.curioswitch.gradle.plugins.codelabs;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CodelabsSetupPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null, "codelabs-setup-plugin can only be applied to the root project.");

    project
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin -> {
                plugin.registerToolIfAbsent(
                    "claat",
                    tool -> {
                      tool.getVersion().set("1.1.0");
                      tool.getBaseUrl().set("https://github.com/googlecodelabs/tools/releases/download/");
                      tool.getArtifactPattern().set("v[revision]/[artifact]-[classifier][ext]");

                      // Not an archive.
                      tool.getOsExtensions().getLinux().set("");
                      tool.getOsExtensions().getMac().set("");
                      tool.getOsExtensions().getWindows().set(".exe");

                      tool.getPathSubDirs().add("");
                    });
              var download = DownloadToolUtil.getDownloadTask(project, "claat");
              // Not an archive.
              download.configure(t -> t.setArchiveExtractAction(file -> {
                var toolDir = plugin.toolManager().getToolDir("claat");
                var filename = "claat";
                if (file.getName().endsWith(".exe")) {
                  filename += ".exe";
                }
                try {
                  Files.move(file.toPath(), toolDir.resolve(filename));
                } catch (IOException e) {
                  throw new UncheckedIOException("Could not move claat.", e);
                }
              }));
            });
  }
}
