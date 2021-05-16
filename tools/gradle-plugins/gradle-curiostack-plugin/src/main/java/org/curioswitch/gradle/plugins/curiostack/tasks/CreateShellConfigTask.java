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

package org.curioswitch.gradle.plugins.curiostack.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.curioswitch.gradle.helpers.platform.PathUtil;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class CreateShellConfigTask extends DefaultTask {

  private static final String MARKER =
      "#### curio-generated - Edits may be overwritten at any time. ####";

  private static final ImmutableList<String> SHELL_RCS = ImmutableList.of(".zshrc", ".bashrc");

  @TaskAction
  public void exec() {
    String homeDir = System.getProperty("user.shellHome", System.getProperty("user.home", ""));
    if (homeDir.isEmpty()) {
      return;
    }
    var toolManager = DownloadedToolManager.get(getProject());
    List<String> configLines =
        ImmutableList.of(
            MARKER,
            "export PATH="
                + PathUtil.toBashString(toolManager.getCuriostackDir().resolve("shims"))
                + ":$PATH",
            "export CLOUDSDK_PYTHON="
                + PathUtil.toBashString(toolManager.getBinDir("miniconda-build").resolve("python")),
            "export CLOUDSDK_PYTHON_SITEPACKAGES=1",
            ". "
                + PathUtil.toBashString(
                    toolManager.getToolDir("miniconda-build").resolve("etc/profile.d/conda.sh")),
            MARKER);

    for (String rcFile : SHELL_RCS) {
      Path rcPath = Paths.get(homeDir, rcFile);
      if (!Files.exists(rcPath)) {
        continue;
      }
      final List<String> lines;
      try {
        lines = Files.readAllLines(rcPath, StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new UncheckedIOException("Could not read shell file.", e);
      }
      int firstMarkerIndex = lines.indexOf(MARKER);

      final Iterable<String> rcLines;
      if (firstMarkerIndex == -1) {
        rcLines = Iterables.concat(lines, ImmutableList.of("\n"), configLines);
      } else {
        int lastMarkerIndex = lines.lastIndexOf(MARKER);
        rcLines =
            Iterables.concat(
                lines.subList(0, firstMarkerIndex),
                configLines,
                lines.subList(lastMarkerIndex + 1, lines.size()));
      }

      try {
        Files.write(rcPath, rcLines, StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new UncheckedIOException("Could not write to shell file.", e);
      }
    }

    try {
      Files.writeString(
          getProject().getRootProject().file(".gradle/is-curiostack.txt").toPath(),
          "Satisfying curiosity");
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write curiostack marker.", e);
    }
  }
}
