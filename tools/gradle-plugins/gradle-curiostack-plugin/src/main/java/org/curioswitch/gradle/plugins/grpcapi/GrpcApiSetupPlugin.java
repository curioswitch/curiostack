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

package org.curioswitch.gradle.plugins.grpcapi;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import org.curioswitch.gradle.plugins.nodejs.NodeSetupPlugin;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GrpcApiSetupPlugin implements Plugin<Project> {

  private static final String PROTOC_GEN_GRPC_WEB_VERSION = "1.0.4";

  @Override
  public void apply(Project project) {
    checkState(project.getParent() == null, "GrpcApiSetupPlugin must be applied to root project.");
    project.getPlugins().apply(ToolDownloaderPlugin.class);
    project.getPlugins().apply(NodeSetupPlugin.class);

    project
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin -> {
              plugin.registerToolIfAbsent(
                  "protoc-gen-grpc-web",
                  tool -> {
                    tool.getVersion().set(PROTOC_GEN_GRPC_WEB_VERSION);
                    tool.getBaseUrl().set("https://github.com/grpc/grpc-web/releases/download/");
                    tool.getArtifactPattern()
                        .set("[revision]/[artifact]-[revision]-[classifier][ext]");
                    var classifiers = tool.getOsClassifiers();
                    classifiers.getLinux().set("linux-x86_64");
                    classifiers.getMac().set("darwin-x86_64");
                    classifiers.getWindows().set("windows-x86_64");

                    // Not an archive.
                    var extensions = tool.getOsExtensions();
                    extensions.getLinux().set("");
                    extensions.getMac().set("");
                    extensions.getWindows().set(".exe");

                    tool.getPathSubDirs().add("");
                  });

              var download = DownloadToolUtil.getDownloadTask(project, "protoc-gen-grpc-web");
              // Not an archive.
              download.configure(
                  t ->
                      t.setArchiveExtractAction(
                          file -> {
                            var toolDir = plugin.toolManager().getToolDir("protoc-gen-grpc-web");
                            var filename = "protoc-gen-grpc-web";
                            if (file.getName().endsWith(".exe")) {
                              filename += ".exe";
                            }
                            file.setExecutable(true);
                            try {
                              Files.move(file.toPath(), toolDir.resolve(filename));
                            } catch (IOException e) {
                              throw new UncheckedIOException(
                                  "Could not move protoc-gen-grpc-web.", e);
                            }
                          }));
            });
  }
}
