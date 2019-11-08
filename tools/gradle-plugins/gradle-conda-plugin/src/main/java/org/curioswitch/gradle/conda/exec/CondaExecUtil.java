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
package org.curioswitch.gradle.conda.exec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PathUtil;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.process.ExecSpec;

public final class CondaExecUtil {

  private static final List<String> SYSROOT_GCC_ENV_VARIABLES =
      ImmutableList.of("CFLAGS", "CXXFLAGS", "CGO_CFLAGS", "CGO_CXXFLAGS");

  public static void addExecToProject(Project project) {
    project
        .getExtensions()
        .getExtraProperties()
        .set(
            "condaExec",
            (Action<Action<ExecSpec>>)
                execSpecAction ->
                    project.exec(
                        exec -> {
                          execSpecAction.execute(exec);
                          condaExec(exec, project);
                        }));
  }

  public static void condaExec(ExecSpec exec, Project project) {
    condaExec(exec, DownloadedToolManager.get(project), "miniconda-build");
  }

  /** Modifies the {@link ExecSpec} to run its command in a conda environment. */
  public static void condaExec(ExecSpec exec, DownloadedToolManager toolManager, String tool) {
    var platformHelper = new PlatformHelper();

    Path condaDir = toolManager.getToolDir(tool);
    Path condaSh = condaDir.resolve(Paths.get("etc", "profile.d", "conda.sh"));

    List<String> currentCommandLine = exec.getCommandLine();
    if (platformHelper.getOs() == OperatingSystem.WINDOWS) {
      currentCommandLine =
          ImmutableList.<String>builderWithExpectedSize(currentCommandLine.size())
              .add(PathUtil.toBashString(currentCommandLine.get(0)))
              .addAll(Iterables.skip(currentCommandLine, 1))
              .build();
    }
    exec.setExecutable(PathUtil.getBashExecutable());
    exec.setArgs(
        ImmutableList.of(
            "-c",
            ". "
                + PathUtil.toBashString(condaSh)
                + " && conda activate > /dev/null && cd "
                + PathUtil.toBashString(exec.getWorkingDir().toPath())
                + " && "
                + String.join(" ", currentCommandLine)));

    if (platformHelper.getOs() == OperatingSystem.MAC_OSX) {
      // Set known environment variables for controlling sysroot on Mac. It never hurts to define
      // too many environment variables.

      var macOsSdkPath = toolManager.getToolDir("macos-sdk").toAbsolutePath().toString();

      // CMake - CMAKE_OSX_SYSROOT
      exec.environment("SDKROOT", macOsSdkPath);

      for (var flag : SYSROOT_GCC_ENV_VARIABLES) {
        var environment = exec.getEnvironment();
        exec.environment(
            flag,
            "-isysroot"
                + macOsSdkPath
                + " -mmacosx-version-min=10.10 "
                + environment.getOrDefault(flag, "")
                + ' '
                + System.getenv().getOrDefault(flag, ""));
      }
    }
  }

  private CondaExecUtil() {}
}
