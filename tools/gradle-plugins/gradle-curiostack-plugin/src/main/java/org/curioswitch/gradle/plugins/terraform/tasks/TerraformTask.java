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

import java.io.File;
import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;

public class TerraformTask extends DefaultTask {

  private Iterable<String> args;
  private Action<ExecSpec> execCustomizer;

  public TerraformTask setArgs(Iterable<String> args) {
    this.args = args;
    return this;
  }

  public TerraformTask setExecCustomizer(Action<ExecSpec> execCustomizer) {
    this.execCustomizer = execCustomizer;
    return this;
  }

  public Action<ExecSpec> getExecCustomizer() {
    return execCustomizer;
  }

  @TaskAction
  void exec() {
    var project = getProject();
    project.exec(
        exec -> {
          exec.executable(
              CommandUtil.getCuriostackDir(project)
                  .resolve("terraform")
                  .resolve(ToolDependencies.getTerraformVersion(project))
                  .resolve("terraform"));
          exec.args(args);
          exec.workingDir(new File(project.getBuildDir(), "terraform"));
          exec.setStandardInput(System.in);
          if (execCustomizer != null) {
            execCustomizer.execute(exec);
          }
        });
  }
}
