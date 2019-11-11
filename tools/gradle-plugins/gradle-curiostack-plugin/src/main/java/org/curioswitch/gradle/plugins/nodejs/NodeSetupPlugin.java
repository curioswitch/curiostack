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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.curioswitch.gradle.conda.CondaBuildEnvPlugin;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PathUtil;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask;
import org.curioswitch.gradle.plugins.nodejs.tasks.UpdateNodeResolutions;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Delete;

public class NodeSetupPlugin implements Plugin<Project> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null, "node-setup-plugin can only be applied to the root project.");

    NodeSetupExtension.create(project);

    project.getPlugins().apply(CondaBuildEnvPlugin.class);
    project.getPlugins().apply(BasePlugin.class);

    project
        .getTasks()
        .withType(Delete.class)
        .named("clean")
        .configure(t -> t.delete("node_modules"));

    var toolManager = DownloadedToolManager.get(project);

    project
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin ->
                plugin.registerToolIfAbsent(
                    "node",
                    tool -> {
                      var version = ToolDependencies.getNodeVersion(project);

                      tool.getVersion().set(version);
                      tool.getBaseUrl().set("https://nodejs.org/dist/");
                      tool.getArtifactPattern()
                          .set("v[revision]/[artifact]-v[revision]-[classifier].[ext]");
                      var classifiers = tool.getOsClassifiers();
                      classifiers.getLinux().set("linux-x64");
                      classifiers.getMac().set("darwin-x64");
                      classifiers.getWindows().set("win-x64");

                      var operatingSystem = new PlatformHelper().getOs();

                      String nodePathSubDir =
                          "node-v" + version + "-" + classifiers.getValue(operatingSystem);

                      Path prefixDir = toolManager.getToolDir("node").resolve(nodePathSubDir);

                      if (operatingSystem != OperatingSystem.WINDOWS) {
                        nodePathSubDir += "/bin";
                      }

                      tool.getPathSubDirs().add(nodePathSubDir);
                      tool.getAdditionalCachedDirs().add("yarn-cache");

                      var downloadYarn =
                          project
                              .getRootProject()
                              .getTasks()
                              .register(
                                  "toolsDownloadYarn",
                                  NodeTask.class,
                                  t -> {
                                    var yarnVersion = ToolDependencies.getYarnVersion(project);

                                    t.setCommand("npm");
                                    t.args(
                                        "install",
                                        "--prefix",
                                        PathUtil.toBashString(prefixDir),
                                        "--no-save",
                                        "yarn@" + yarnVersion);
                                    t.dependsOn(
                                        DownloadToolUtil.getDownloadTask(project, "node"),
                                        DownloadToolUtil.getSetupTask(project, "miniconda-build"));
                                    t.execOverride(
                                        exec -> exec.workingDir(toolManager.getBinDir("node")));

                                    t.onlyIf(
                                        unused -> {
                                          File packageJson =
                                              toolManager
                                                  .getBinDir("node")
                                                  .resolve(
                                                      operatingSystem != OperatingSystem.WINDOWS
                                                          ? "../lib"
                                                          : "")
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
                                                .equals(yarnVersion)) {
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
                      setupNode.configure(t -> t.dependsOn(downloadYarn));
                    }));

    var setupNode = DownloadToolUtil.getSetupTask(project, "node");

    project.allprojects(
        p ->
            p.getTasks()
                .withType(NodeTask.class)
                .configureEach(
                    t -> {
                      if (t.getPath().equals(":toolsDownloadYarn")) {
                        return;
                      }
                      t.dependsOn(setupNode);
                      t.execOverride(
                          exec ->
                              exec.environment(
                                  "YARN_CACHE_FOLDER",
                                  DownloadedToolManager.get(project)
                                      .getCuriostackDir()
                                      .resolve("yarn-cache")
                                      .toString()));
                    }));

    var updateNodeResolutions =
        project.getTasks().register(UpdateNodeResolutions.NAME, UpdateNodeResolutions.class, false);
    var checkNodeResolutions =
        project
            .getTasks()
            .register(UpdateNodeResolutions.CHECK_NAME, UpdateNodeResolutions.class, true);

    var yarnWarning =
        project
            .getTasks()
            .register(
                "yarnWarning",
                task ->
                    task.doFirst(
                        unused ->
                            project
                                .getLogger()
                                .warn(
                                    "yarn task failed. If you have updated a dependency and the "
                                        + "error says 'Your lockfile needs to be updated.', run \n\n"
                                        + "./gradlew yarnUpdate")));

    var yarn =
        project
            .getTasks()
            .register(
                "yarn",
                NodeTask.class,
                t -> {
                  t.dependsOn(setupNode);
                  t.args("--frozen-lockfile");

                  var packageJsonFile = project.file("package.json");
                  if (packageJsonFile.exists()) {
                    final JsonNode packageJson;
                    try {
                      packageJson = OBJECT_MAPPER.readTree(packageJsonFile);
                    } catch (IOException e) {
                      throw new UncheckedIOException("Could not read package.json", e);
                    }
                    if (packageJson.has("workspaces")) {
                      for (var workspaceNode : packageJson.get("workspaces")) {
                        String workspacePath = workspaceNode.asText();

                        // Assume any workspace in build/web requires a build before running yarn.
                        // This is usually used for generating protos.
                        if (!workspacePath.endsWith("/build/web")) {
                          continue;
                        }

                        String projectPath =
                            workspacePath.substring(
                                0, workspacePath.length() - "/build/web".length());

                        Project workspace =
                            project.findProject(':' + projectPath.replace('/', ':'));
                        if (workspace != null) {
                          t.dependsOn(workspace.getPath() + ":build");
                        }
                      }
                    }
                  }

                  yarnWarning.get().onlyIf(unused -> t.getState().getFailure() != null);
                  t.finalizedBy(yarnWarning, checkNodeResolutions);
                });
    checkNodeResolutions.configure(t -> t.dependsOn(yarn));

    project.getTasks().register("yarnUpdate", NodeTask.class, t -> t.dependsOn(setupNode));
  }
}
