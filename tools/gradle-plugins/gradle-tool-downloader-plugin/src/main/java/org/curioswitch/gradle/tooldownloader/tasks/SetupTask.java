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
package org.curioswitch.gradle.tooldownloader.tasks;

import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.curioswitch.gradle.helpers.platform.PathUtil;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

public class SetupTask extends DefaultTask {

  private final String toolName;
  private final WorkerExecutor workerExecutor;

  @Inject
  public SetupTask(String toolName, WorkerExecutor workerExecutor) {
    this.toolName = toolName;
    this.workerExecutor = workerExecutor;
  }

  @TaskAction
  public void exec() throws Exception {
    if ("true".equals(System.getenv("CI")) || toolName.equals("graalvm")) {
      return;
    }

    DownloadedToolManager toolManager = DownloadToolUtil.getManager(getProject());

    Path shimsPath = toolManager.getCuriostackDir().resolve("shims");
    if (Files.exists(shimsPath)) {
      try (var s = Files.walk(shimsPath)) {
        s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
    getProject().mkdir(shimsPath);

    final String shimTemplate;
    try {
      shimTemplate =
          Resources.toString(
              Resources.getResource("tooldownloader/shim-template.sh"), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not open shim template.", e);
    }

    for (Path binDir : toolManager.getBinDirs(toolName)) {
      try (Stream<Path> s = Files.list(binDir)) {
        s.filter(Files::isExecutable)
            // Git is mainly for use on CI, don't shim it on dev machines since it's never better.
            .filter(p -> !p.getFileName().toString().startsWith("git"))
            // Probably not a good idea to shim DLLs
            .filter(p -> !p.getFileName().endsWith(".dll"))
            .filter(p -> !p.getFileName().endsWith(".pyd"))
            .forEach(
                path ->
                    workerExecutor.submit(
                        WriteShim.class,
                        config -> {
                          config.setIsolationMode(IsolationMode.NONE);
                          config.params(shimsPath.toString(), path.toString(), shimTemplate);
                        }));
      } catch (IOException e) {
        throw new UncheckedIOException("Could not open directory.", e);
      }
    }
  }

  public static class WriteShim implements Runnable {

    private final String shimsPath;
    private final String exePath;
    private final String shimTemplate;

    @Inject
    public WriteShim(String shimsPath, String exePath, String shimTemplate) {
      this.shimsPath = shimsPath;
      this.exePath = exePath;
      this.shimTemplate = shimTemplate;
    }

    @Override
    public void run() {
      Path shimsPath = Paths.get(this.shimsPath);
      Path exePath = Paths.get(this.exePath);

      Path shimPath = shimsPath.resolve(exePath.getFileName());

      String shim =
          shimTemplate
              .replace(
                  "||REPO_PATH||",
                  PathUtil.toBashString(
                      "$(git rev-parse --show-toplevel 2>/dev/null || echo /dev/null)"))
              .replace("||TOOL_EXE_PATH||", PathUtil.toBashString(exePath.toString()))
              .replace("||EXE_NAME||", exePath.getFileName().toString())
              .replace("||SHIMS_PATH||", PathUtil.toBashString(shimsPath.toString()));

      try {
        com.google.common.io.Files.asCharSink(shimPath.toFile(), StandardCharsets.UTF_8)
            .write(shim);
        if (!shimPath.toFile().setExecutable(true)) {
          throw new IOException("Could not make shim file executable.");
        }
      } catch (IOException e) {
        throw new UncheckedIOException("Could not write to shim file.", e);
      }
    }
  }
}
