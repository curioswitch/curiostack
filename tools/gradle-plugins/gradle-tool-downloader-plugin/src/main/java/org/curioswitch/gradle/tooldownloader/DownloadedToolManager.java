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
package org.curioswitch.gradle.tooldownloader;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.process.ProcessForkOptions;

/**
 * Manager of all the downloaded tools in a build. Can be used to access the tool directories and
 * executables.
 */
public class DownloadedToolManager {

  public static DownloadedToolManager get(Project project) {
    var toolManager =
        (DownloadedToolManager)
            project.getRootProject().getExtensions().getExtraProperties().get("toolManager");
    checkNotNull(toolManager, "toolManager not found. Did you apply the tool-downloader plugin?");
    return toolManager;
  }

  private final Path curiostackDir;

  private final NamedDomainObjectContainer<ToolDownloaderExtension> tools;

  @Inject
  public DownloadedToolManager(
      Project project, NamedDomainObjectContainer<ToolDownloaderExtension> tools) {
    this.tools = tools;

    curiostackDir = project.getGradle().getGradleUserHomeDir().toPath().resolve("curiostack");
  }

  public Path getCuriostackDir() {
    return curiostackDir;
  }

  public Path getToolDir(String toolName) {
    checkNotNull(toolName, "toolName");
    var tool = tools.findByName(toolName);
    checkState(tool != null, "Tool %s not registered.", toolName);
    return curiostackDir.resolve(toolName).resolve(tool.getVersion().get());
  }

  public Path getBinDir(String toolName) {
    return getBinDirs(toolName).get(0);
  }

  public List<Path> getBinDirs(String toolName) {
    checkNotNull(toolName, "toolName");
    var tool = tools.findByName(toolName);
    checkState(tool != null, "Tool %s not registered.", toolName);
    return tool.getPathSubDirs().get().stream()
        .map(
            subDir ->
                curiostackDir.resolve(toolName).resolve(tool.getVersion().get()).resolve(subDir))
        .collect(toImmutableList());
  }

  public List<Path> getAllBinDirs() {
    return tools.getNames().stream()
        .flatMap(tool -> getBinDirs(tool).stream())
        .collect(toImmutableList());
  }

  public void addAllToPath(ProcessForkOptions exec, Path... additionalPathItems) {
    String toolsPath =
        Stream.concat(
                tools.stream()
                    .filter(tool -> !tool.getName().equals("graalvm"))
                    .flatMap(
                        tool -> {
                          Path toolDir = getToolDir(tool.getName()).toAbsolutePath();
                          return tool.getPathSubDirs().get().stream().map(toolDir::resolve);
                        }),
                Arrays.stream(additionalPathItems))
            .map(Path::toString)
            .collect(Collectors.joining(File.pathSeparator));
    String linuxStylePath = System.getenv("PATH");
    if (linuxStylePath != null) {
      var modifiedPath = exec.getEnvironment().get("PATH");
      if (modifiedPath != null) {
        toolsPath = toolsPath + File.pathSeparator + modifiedPath;
      }
      exec.environment("PATH", toolsPath + File.pathSeparator + linuxStylePath);
    }
    String windowsStylePath = System.getenv("Path");
    if (windowsStylePath != null) {
      var modifiedPath = exec.getEnvironment().get("Path");
      if (modifiedPath != null) {
        toolsPath = toolsPath + File.pathSeparator + modifiedPath;
      }
      exec.environment("Path", toolsPath + File.pathSeparator + windowsStylePath);
    }
  }
}
