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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.curioswitch.gradle.golang.GoExecUtil;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

public class GoTask extends DefaultTask {

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let's enjoy the speed.
  private static final ConcurrentHashMap<String, GoTask> TASKS = new ConcurrentHashMap<>();

  private final Property<String> command;
  private final ListProperty<String> args;
  private final List<Action<ExecSpec>> execCustomizers;

  private final WorkerExecutor workerExecutor;

  @Nullable private File lockFile;

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

  public GoTask setLockFile(File lockFile) {
    this.lockFile = lockFile;
    return this;
  }

  @Input
  public ListProperty<String> getArgs() {
    return args;
  }

  @TaskAction
  void exec() throws Exception {
    String mapKey = UUID.randomUUID().toString();
    TASKS.put(mapKey, this);

    workerExecutor.submit(
        DoGoTask.class,
        config -> {
          config.setIsolationMode(IsolationMode.NONE);
          config.params(mapKey);
        });
  }

  public static class DoGoTask implements Runnable {

    private final String mapKey;

    @Inject
    public DoGoTask(String mapKey) {
      this.mapKey = mapKey;
    }

    @Override
    public void run() {
      var task = TASKS.remove(mapKey);

      FileLock lock = null;
      if (task.lockFile != null) {
        try {
          FileChannel lockChannel = new RandomAccessFile(task.lockFile, "rw").getChannel();
          while (true) {
            try {
              lock = lockChannel.lock();
              break;
            } catch (OverlappingFileLockException e) {
              Thread.sleep(100);
            }
          }
        } catch (InterruptedException | IOException e) {
          throw new IllegalStateException("Could not acquire lock.", e);
        }
      }

      try {
        task.getProject()
            .exec(
                exec -> {
                  GoExecUtil.goExec(exec, task.getProject(), task.command.get(), task.args.get());

                  for (var execCustomizer : task.execCustomizers) {
                    execCustomizer.execute(exec);
                  }
                });
      } finally {
        if (lock != null) {
          try {
            lock.release();
          } catch (IOException e) {
            task.getProject().getLogger().warn("Could not release lock.", e);
          }
        }
      }
    }
  }
}
