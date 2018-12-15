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

package org.curioswitch.gradle.plugins.ci.tasks;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

public class UploadCodeCovCacheTask extends DefaultTask {

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let's enjoy the speed.
  private static final ConcurrentHashMap<String, UploadCodeCovCacheTask> TASKS =
      new ConcurrentHashMap<>();

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
    String mapKey = UUID.randomUUID().toString();
    TASKS.put(mapKey, this);

    workerExecutor.submit(
        UploadReports.class,
        config -> {
          config.setIsolationMode(IsolationMode.NONE);
          config.params(mapKey);
        });
  }

  public static class UploadReports implements Runnable {

    private final String mapKey;

    @Inject
    public UploadReports(String mapKey) {
      this.mapKey = mapKey;
    }

    @Override
    public void run() {
      var task = TASKS.remove(mapKey);

      final List<String> coverageFiles;
      try (var lines = Files.lines(task.getCodeCovReportFile().toPath())) {
        coverageFiles =
            lines
                .filter(line -> line.startsWith("# path="))
                .map(line -> line.substring("# path=".length()))
                .filter(filename -> Files.exists(task.getProject().file(filename).toPath()))
                .map(line -> "./" + line)
                .collect(toImmutableList());
      } catch (IOException e) {
        throw new UncheckedIOException("Could not read coverage report dump", e);
      }

      var toolManager = DownloadedToolManager.get(task.getProject());

      String gsutil =
          new PlatformHelper().getOs() == OperatingSystem.WINDOWS ? "gsutil.cmd" : "gsutil";

      task.getProject()
          .exec(
              exec -> {
                exec.executable("bash");

                exec.args(
                    "-c",
                    "tar -cpzf - "
                        + String.join(" ", coverageFiles)
                        + " | "
                        + gsutil
                        + " cp - "
                        + task.dest.get());

                toolManager.addAllToPath(exec);
              });
    }
  }
}
