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
package org.curioswitch.gradle.plugins.ci.tasks;

import javax.inject.Inject;
import org.curioswitch.gradle.helpers.exec.ExternalExecUtil;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

public class FetchCodeCovCacheTask extends DefaultTask {

  private final Property<String> src;

  private final WorkerExecutor workerExecutor;

  @Inject
  public FetchCodeCovCacheTask(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;

    src = getProject().getObjects().property(String.class);
  }

  @Input
  public Property<String> getSrc() {
    return src;
  }

  public FetchCodeCovCacheTask setSrc(String src) {
    this.src.set(src);
    return this;
  }

  public FetchCodeCovCacheTask setSrc(Provider<String> src) {
    this.src.set(src);
    return this;
  }

  @TaskAction
  public void exec() {
    ExternalExecUtil.exec(
        getProject(),
        workerExecutor,
        exec -> {
          var toolManager = DownloadedToolManager.get(getProject());

          String gsutil =
              new PlatformHelper().getOs() == OperatingSystem.WINDOWS ? "gsutil.cmd" : "gsutil";

          exec.executable("bash");

          exec.args("-c", gsutil + " cp " + src.get() + " - | tar -xpz --skip-old-files");

          exec.setIgnoreExitValue(true);

          toolManager.addAllToPath(exec);
        });
  }
}
