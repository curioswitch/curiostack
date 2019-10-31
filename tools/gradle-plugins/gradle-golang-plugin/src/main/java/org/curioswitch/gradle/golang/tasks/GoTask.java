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
package org.curioswitch.gradle.golang.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.curioswitch.gradle.golang.GoExecUtil;
import org.curioswitch.gradle.helpers.exec.ExternalExecUtil;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;
import org.gradle.workers.WorkerExecutor;

public class GoTask extends DefaultTask {

  private final Property<String> command;
  private final ListProperty<String> args;
  private final List<Action<ExecSpec>> execCustomizers;

  private final WorkerExecutor workerExecutor;

  @Inject
  public GoTask(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;

    var objects = getProject().getObjects();
    command = objects.property(String.class).value("go");
    args = objects.listProperty(String.class).empty();
    execCustomizers = new ArrayList<>();
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

  public GoTask execCustomizer(Action<ExecSpec> execCustomizer) {
    this.execCustomizers.add(checkNotNull(execCustomizer, "execCustomizer"));
    return this;
  }

  @Input
  public ListProperty<String> getArgs() {
    return args;
  }

  @TaskAction
  void exec() {
    ExternalExecUtil.exec(
        getProject(),
        workerExecutor,
        exec -> {
          GoExecUtil.goExec(exec, getProject(), command.get(), args.get());
          execCustomizers.forEach(a -> a.execute(exec));
        });
  }
}
