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

package org.curioswitch.gradle.plugins.gcloud.tasks;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

public class UploadToolCacheTask extends DefaultTask {

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let's enjoy the speed.
  private static final ConcurrentHashMap<String, UploadToolCacheTask> TASKS =
      new ConcurrentHashMap<>();

  private final Property<String> dest;
  private final ListProperty<String> srcPaths;

  private final WorkerExecutor workerExecutor;

  @Inject
  public UploadToolCacheTask(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;

    dest = getProject().getObjects().property(String.class);
    srcPaths = getProject().getObjects().listProperty(String.class).empty();

    onlyIf(unused -> "true".equals(System.getenv("CI_MASTER")));
  }

  public UploadToolCacheTask setDest(String dest) {
    this.dest.set(dest);
    return this;
  }

  public UploadToolCacheTask setDest(Provider<String> dest) {
    this.dest.set(dest);
    return this;
  }

  public UploadToolCacheTask srcPath(String srcPath) {
    this.srcPaths.add(srcPath);
    return this;
  }

  @TaskAction
  public void exec() {
    String mapKey = UUID.randomUUID().toString();
    TASKS.put(mapKey, this);

    // We usually execute long-running tasks in the executor but directly run for now since gsutil
    // seems to be flaky under high concurrency.
    new DoUploadToolCache(mapKey).run();
  }

  public static class DoUploadToolCache implements Runnable {
    private final String mapKey;

    @Inject
    public DoUploadToolCache(String mapKey) {
      this.mapKey = mapKey;
    }

    @Override
    public void run() {
      UploadToolCacheTask task = TASKS.remove(mapKey);
      var toolManager = DownloadedToolManager.get(task.getProject());
      String gsutil = Os.isFamily(Os.FAMILY_WINDOWS) ? "gsutil" + ".cmd" : "gsutil";
      task.getProject()
          .exec(
              exec -> {
                exec.executable("bash");
                exec.workingDir(toolManager.getCuriostackDir());
                exec.args(
                    "-c",
                    "tar -cpf - "
                        + String.join(" ", task.srcPaths.get())
                        + " | lz4 -qc - |"
                        + gsutil
                        + " -o GSUtil:parallel_composite_upload_threshold=150M cp - "
                        + task.dest.get());
              });
    }
  }
}
