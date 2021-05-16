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

package org.curioswitch.gradle.plugins.ci.tasks;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import javax.inject.Inject;
import org.curioswitch.gradle.helpers.exec.ExternalExecUtil;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

public class UploadCodeCovCacheTask extends DefaultTask {

  private final Property<String> dest;

  private final WorkerExecutor workerExecutor;

  @Inject
  public UploadCodeCovCacheTask(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;

    dest = getProject().getObjects().property(String.class);
  }

  @InputFile
  public File getCodeCovReportFile() {
    return getProject().file("build/codecov-report.txt");
  }

  @Input
  public Property<String> getDest() {
    return dest;
  }

  public UploadCodeCovCacheTask setDest(String dest) {
    this.dest.set(dest);
    return this;
  }

  public UploadCodeCovCacheTask setDest(Provider<String> dest) {
    this.dest.set(dest);
    return this;
  }

  @TaskAction
  public void exec() {
    if (!getCodeCovReportFile().exists()) {
      // UploadToCodeCovTask failed, skip uploading the cache.
      return;
    }

    ExternalExecUtil.exec(
        getProject(),
        workerExecutor,
        exec -> {
          final List<String> coverageFiles;
          try (var lines = Files.lines(getCodeCovReportFile().toPath())) {
            coverageFiles =
                lines
                    .filter(line -> line.startsWith("# path="))
                    .map(line -> line.substring("# path=".length()))
                    .filter(filename -> Files.exists(getProject().file(filename).toPath()))
                    .map(line -> "./" + line)
                    .collect(toImmutableList());
          } catch (IOException e) {
            throw new UncheckedIOException("Could not read coverage report dump", e);
          }

          var toolManager = DownloadedToolManager.get(getProject());

          String gsutil =
              new PlatformHelper().getOs() == OperatingSystem.WINDOWS ? "gsutil.cmd" : "gsutil";
          exec.executable("bash");

          exec.args(
              "-c",
              "tar -cpzf - "
                  + String.join(" ", coverageFiles)
                  + " | "
                  + gsutil
                  + " cp - "
                  + dest.get());

          toolManager.addAllToPath(exec);
        });
  }
}
