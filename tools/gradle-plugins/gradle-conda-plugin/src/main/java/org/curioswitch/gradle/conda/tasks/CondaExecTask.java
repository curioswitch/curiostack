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

package org.curioswitch.gradle.conda.tasks;

import javax.annotation.Nullable;
import org.curioswitch.gradle.conda.exec.CondaExecUtil;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;

public class CondaExecTask extends DefaultTask {

  private final Property<String> command;
  private final Property<String> condaName;

  @Nullable private Action<ExecSpec> execCustomizer;

  public CondaExecTask() {
    var objects = getProject().getObjects();
    command = objects.property(String.class);
    condaName = objects.property(String.class);
  }

  public CondaExecTask setCommand(String command) {
    this.command.set(command);
    return this;
  }

  public CondaExecTask setCondaName(String condaName) {
    this.condaName.set(condaName);
    return this;
  }

  public CondaExecTask setExecCustomizer(Action<ExecSpec> execCustomizer) {
    this.execCustomizer = execCustomizer;
    return this;
  }

  @TaskAction
  public void exec() {
    getProject()
        .exec(
            exec -> {
              exec.setCommandLine(command.get());

              CondaExecUtil.condaExec(
                  exec, DownloadedToolManager.get(getProject()), condaName.get());

              if (execCustomizer != null) {
                execCustomizer.execute(exec);
              }
            });
  }
}
