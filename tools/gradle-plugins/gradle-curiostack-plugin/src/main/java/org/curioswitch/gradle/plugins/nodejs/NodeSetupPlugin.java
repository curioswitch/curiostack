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

package org.curioswitch.gradle.plugins.nodejs;

import static com.google.common.base.Preconditions.checkState;
import static org.curioswitch.gradle.plugins.curiostack.StandardDependencies.NODE_VERSION;
import static org.curioswitch.gradle.plugins.curiostack.StandardDependencies.YARN_VERSION;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class NodeSetupPlugin implements Plugin<Project> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null, "node-setup-plugin can only be applied to the root project.");

    project
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin ->
                plugin.registerToolIfAbsent(
                    "node",
                    tool -> {
                      tool.getVersion().set(NODE_VERSION);
                      tool.getBaseUrl().set("https://nodejs.org/dist/");
                      tool.getArtifactPattern()
                          .set("v[revision]/[artifact]-v[revision]-[classifier].[ext]");
                      var classifiers = tool.getOsClassifiers();
                      classifiers.getLinux().set("linux-x64");
                      classifiers.getMac().set("darwin-x64");
                      classifiers.getWindows().set("win-x64");

                      tool.getPathSubDirs()
                          .add(
                              "node-v"
                                  + NODE_VERSION
                                  + "-"
                                  + classifiers.getValue(new PlatformHelper().getOs()));
                      tool.getAdditionalCachedDirs().add("yarn-cache");
                      var downloadYarn =
                          project
                              .getRootProject()
                              .getTasks()
                              .register(
                                  "toolsDownloadYarn",
                                  NodeTask.class,
                                  t -> {
                                    t.setCommand("npm");
                                    t.args(
                                        "install", "--global", "--no-save", "yarn@" + YARN_VERSION);
                                    t.dependsOn(DownloadToolUtil.getDownloadTask(project, "node"));

                                    t.onlyIf(
                                        unused -> {
                                          File packageJson =
                                              DownloadedToolManager.get(project)
                                                  .getBinDir("node")
                                                  .resolve(
                                                      Paths.get(
                                                          "node_modules", "yarn", "package.json"))
                                                  .toFile();
                                          if (!packageJson.exists()) {
                                            return true;
                                          }
                                          try {
                                            if (!OBJECT_MAPPER
                                                .readTree(packageJson)
                                                .get("version")
                                                .asText()
                                                .equals(YARN_VERSION)) {
                                              return true;
                                            }
                                          } catch (IOException e) {
                                            throw new UncheckedIOException(
                                                "Could not read package.json", e);
                                          }
                                          return false;
                                        });
                                  });

                      var setupNode = DownloadToolUtil.getSetupTask(project, "node");
                      setupNode.configure(
                          t -> {
                            t.dependsOn(DownloadToolUtil.getSetupTask(project, "miniconda2-build"));
                            t.dependsOn(downloadYarn);
                          });
                    }));


    var setupNode = DownloadToolUtil.getSetupTask(project, "node");
    var yarn =
        project
            .getTasks()
            .register(
                "yarn",
                NodeTask.class,
                t -> {
                  t.dependsOn(setupNode);
                  t.args("--frozen-lockfile");

                  var yarnWarning =
                      project
                          .getTasks()
                          .register(
                              "yarnWarning",
                              task -> {
                                task.onlyIf(unused -> t.getState().getFailure() != null);
                                task.doFirst(
                                    unused ->
                                        project
                                            .getLogger()
                                            .warn(
                                                "yarn task failed. If you have updated a dependency and the "
                                                    + "error says 'Your lockfile needs to be updated.', run \n\n"
                                                    + "./gradlew yarnUpdate"));
                              });
                  t.finalizedBy(yarnWarning);
                });

    project.getTasks().register("yarnUpdate", NodeTask.class, t -> t.dependsOn(setupNode));
  }
}
