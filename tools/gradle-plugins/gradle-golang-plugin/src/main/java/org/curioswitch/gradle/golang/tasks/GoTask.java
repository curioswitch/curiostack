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

package org.curioswitch.gradle.golang.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;

public class GoTask extends DefaultTask {

  private final Property<String> command;
  private final ListProperty<String> args;

  @Nullable private Action<ExecSpec> execCustomizer;

  public GoTask() {
    var objects = getProject().getObjects();
    command = objects.property(String.class);
    args = objects.listProperty(String.class);

    command.set("go");
  }

  public GoTask command(Property<String> command) {
    this.command.set(command);
    return this;
  }

  public GoTask command(String command) {
    this.command.set(command);
    return this;
  }

  public GoTask args(ListProperty<String> args) {
    this.args.addAll(checkNotNull(args, "args"));
    return this;
  }

  public GoTask args(String... args) {
    this.args.addAll(checkNotNull(args, "args"));
    return this;
  }

  public GoTask args(Iterable<String> args) {
    this.args.addAll(checkNotNull(args, "args"));
    return this;
  }

  public GoTask setExecCustomizer(Action<ExecSpec> execCustomizer) {
    this.execCustomizer = checkNotNull(execCustomizer, "execCustomizer");
    return this;
  }

  @Input
  public ListProperty<String> getArgs() {
    return args;
  }

  @TaskAction
  void exec() {
    getProject()
        .exec(
            exec -> {
              var toolManager = DownloadedToolManager.get(getProject());

              exec.executable(toolManager.getBinDir("go").resolve(command.get()));
              exec.args(args.get());
              exec.environment("GOROOT", toolManager.getToolDir("go").resolve("go"));
              exec.environment(
                  "GOPATH",
                  getProject()
                      .getGradle()
                      .getGradleUserHomeDir()
                      .toPath()
                      .resolve("curiostack")
                      .resolve("gopath"));

              toolManager.addAllToPath(exec);

              if (execCustomizer != null) {
                execCustomizer.execute(exec);
              }
            });
  }
}
