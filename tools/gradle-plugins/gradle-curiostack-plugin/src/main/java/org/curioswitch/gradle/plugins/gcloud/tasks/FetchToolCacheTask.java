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
package org.curioswitch.gradle.plugins.gcloud.tasks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.curioswitch.gradle.helpers.exec.ExternalExecUtil;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

public class FetchToolCacheTask extends DefaultTask {

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
  public void exec() throws Exception {
    getProject().mkdir(DownloadedToolManager.get(getProject()).getCuriostackDir());

    File tempDir = getTemporaryDir();
    tempDir.createNewFile();

    Path archive = Paths.get(tempDir.getAbsolutePath(), "tool-cache.lz4");

    var toolManager = DownloadedToolManager.get(getProject());

    ExternalExecUtil.exec(
        getProject(),
        workerExecutor,
        exec -> {
          String gsutil = Os.isFamily(Os.FAMILY_WINDOWS) ? "gsutil" + ".cmd" : "gsutil";

          exec.executable(gsutil);

          exec.args("cp", src.get(), archive);

          exec.setIgnoreExitValue(true);
        });

    workerExecutor.await();

    if (!Files.exists(archive)) {
      getLogger().quiet("Cache failed to download, skipping.");
      return;
    }

    ExternalExecUtil.exec(
        getProject(),
        workerExecutor,
        exec -> {
          exec.executable("bash");
          exec.workingDir(toolManager.getCuriostackDir());

          exec.args("-c", "lz4 -dc " + archive + " | tar -xp");

          exec.setIgnoreExitValue(true);
        });
  }
}
