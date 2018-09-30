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

package org.curioswitch.gradle.plugins.terraform.tasks;

import de.undercouch.gradle.tasks.download.DownloadAction;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

public class DownloadTerraformPluginTask extends DefaultTask {

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let's enjoy the speed.
  private static final ConcurrentHashMap<String, DownloadTerraformPluginTask> TASKS =
      new ConcurrentHashMap<>();

  private final String pluginPackage;
  private final String version;

  private final WorkerExecutor workerExecutor;

  @Inject
  public DownloadTerraformPluginTask(
      String pluginPackage, String version, WorkerExecutor workerExecutor) {
    this.pluginPackage = pluginPackage;
    this.version = version;
    this.workerExecutor = workerExecutor;
  }

  @TaskAction
  public void exec() {
    String mapKey = UUID.randomUUID().toString();
    TASKS.put(mapKey, this);

    workerExecutor.submit(
        DoInstallTerraformPluginTask.class,
        config -> {
          config.setIsolationMode(IsolationMode.NONE);
          config.params(mapKey);
        });
  }

  public static class DoInstallTerraformPluginTask implements Runnable {

    private final String mapKey;

    @Inject
    public DoInstallTerraformPluginTask(String mapKey) {
      this.mapKey = mapKey;
    }

    @Override
    public void run() {
      var task = TASKS.remove(mapKey);

      File archiveDir = task.getTemporaryDir();
      String url = "https://" + task.pluginPackage + "/archive/" + task.version + ".zip";
      File archive = new File(archiveDir, task.version + ".zip");

      var download = new DownloadAction(task.getProject());
      try {
        download.src(url);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      download.dest(archive);
      try {
        download.execute();
      } catch (IOException e) {
        throw new UncheckedIOException("Could not download archive.", e);
      }

      task.getProject().copy(copy -> {
        copy.from(task.getProject().zipTree(archive));
        copy.into(archiveDir);
      });
    }
  }
}
