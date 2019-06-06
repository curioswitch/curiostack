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

package org.curioswitch.gradle.plugins.nodejs.tasks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import org.curioswitch.gradle.conda.exec.CondaExecUtil;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

@CacheableTask
public class NodeTask extends DefaultTask {

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let's enjoy the speed.
  private static final ConcurrentHashMap<String, NodeTask> TASKS = new ConcurrentHashMap<>();

  private final Property<String> command;
  private final ListProperty<String> args;
  private final List<Action<ExecSpec>> execOverrides;

  private final WorkerExecutor workerExecutor;

  @Inject
  public NodeTask(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;
    execOverrides = new ArrayList<>();

    var objects = getProject().getObjects();
    command = objects.property(String.class).value("yarn");
    args = objects.listProperty(String.class).empty();
  }

  public NodeTask setCommand(String command) {
    this.command.set(command);
    return this;
  }

  public NodeTask args(String... args) {
    this.args.addAll(args);
    return this;
  }

  public NodeTask args(Iterable<String> args) {
    this.args.addAll(args);
    return this;
  }

  public NodeTask args(Provider<Iterable<String>> args) {
    this.args.addAll(args);
    return this;
  }

  public NodeTask execOverride(Action<ExecSpec> execOverride) {
    execOverrides.add(execOverride);
    return this;
  }

  @TaskAction
  public void exec() {
    String mapKey = UUID.randomUUID().toString();
    TASKS.put(mapKey, this);

    workerExecutor.submit(
        DoNodeTask.class,
        config -> {
          config.setIsolationMode(IsolationMode.NONE);
          config.params(mapKey);
        });
  }

  public static class DoNodeTask implements Runnable {

    private final String mapKey;

    @Inject
    public DoNodeTask(String mapKey) {
      this.mapKey = mapKey;
    }

    @Override
    public void run() {
      var task = TASKS.remove(mapKey);

      task.getProject()
          .exec(
              exec -> {
                var toolManager = DownloadedToolManager.get(task.getProject());

                String command = task.command.get();
                if (new PlatformHelper().getOs() == OperatingSystem.WINDOWS) {
                  if (command.equals("node")) {
                    command += ".exe";
                  } else {
                    command += ".cmd";
                  }
                }
                Path binDir = toolManager.getBinDir("node");
                exec.executable(binDir.resolve(command));
                exec.args(task.args.get());

                toolManager.addAllToPath(
                    exec, task.getProject().getRootProject().file("node_modules/.bin").toPath());

                task.execOverrides.forEach(o -> o.execute(exec));
                CondaExecUtil.condaExec(exec, task.getProject());
              });
    }
  }
}
