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
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

public class FetchToolCacheTask extends DefaultTask {

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let's enjoy the speed.
  private static final ConcurrentHashMap<String, FetchToolCacheTask> TASKS =
      new ConcurrentHashMap<>();

  private final Property<String> src;

  private final WorkerExecutor workerExecutor;

  @Inject
  public FetchToolCacheTask(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;
    src = getProject().getObjects().property(String.class);
    onlyIf(unused -> "true".equals(System.getenv("CI")));
  }

  public FetchToolCacheTask setSrc(String src) {
    this.src.set(src);
    return this;
  }

  public FetchToolCacheTask setSrc(Provider<String> src) {
    this.src.set(src);
    return this;
  }

  @TaskAction
  public void exec() {
    getProject().mkdir(DownloadedToolManager.get(getProject()).getCuriostackDir());

    String mapKey = UUID.randomUUID().toString();
    TASKS.put(mapKey, this);

    // We usually execute long-running tasks in the executor but directly run for now since gsutil
    // seems to be flaky under high concurrency.
    new GsutilCopy(mapKey).run();
  }

  public static class GsutilCopy implements Runnable {

    private final String mapKey;

    @Inject
    public GsutilCopy(String mapKey) {
      this.mapKey = mapKey;
    }

    @Override
    public void run() {
      FetchToolCacheTask task = TASKS.remove(mapKey);

      var toolManager = DownloadedToolManager.get(task.getProject());

      String gsutil = Os.isFamily(Os.FAMILY_WINDOWS) ? "gsutil" + ".cmd" : "gsutil";

      task.getProject()
          .exec(
              exec -> {
                exec.executable("bash");
                exec.workingDir(toolManager.getCuriostackDir());

                exec.args("-c", gsutil + " cp " + task.src.get() + " - | lz4 -dc - | tar -xp");

                exec.setIgnoreExitValue(true);
              });
    }
  }
}
