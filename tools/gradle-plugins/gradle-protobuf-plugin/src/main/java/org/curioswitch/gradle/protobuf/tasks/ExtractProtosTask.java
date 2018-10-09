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

package org.curioswitch.gradle.protobuf.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

public class ExtractProtosTask extends DefaultTask {

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let"s enjoy the speed.
  private static final ConcurrentHashMap<String, ExtractProtosTask> TASKS =
      new ConcurrentHashMap<>();

  private final ConfigurableFileCollection files;
  private final DirectoryProperty destDir;

  private final WorkerExecutor workerExecutor;

  @Inject
  public ExtractProtosTask(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;

    files = getProject().getLayout().configurableFiles();
    destDir = newOutputDirectory();

    onlyIf(unused -> !files.isEmpty());
  }

  @InputFiles
  public ConfigurableFileCollection getFiles() {
    return files;
  }

  @OutputDirectory
  public DirectoryProperty getDestDir() {
    return destDir;
  }

  public ExtractProtosTask setDestDir(File destDir) {
    this.destDir.set(destDir);
    return this;
  }

  @TaskAction
  public void exec() {
    checkNotNull(destDir.getOrNull(), "destDir must be set.");

    getProject().delete(destDir);
    getProject().mkdir(destDir);

    for (File file : files) {
      String mapKey = UUID.randomUUID().toString();
      TASKS.put(mapKey, this);

      workerExecutor.submit(
          DoProtobufExtract.class,
          config -> {
            config.setIsolationMode(IsolationMode.NONE);
            config.params(file, mapKey);
          });
    }
  }

  public static class DoProtobufExtract implements Runnable {

    private final File file;
    private final String mapKey;

    @Inject
    public DoProtobufExtract(File file, String mapKey) {
      this.file = file;
      this.mapKey = mapKey;
    }

    @Override
    public void run() {
      ExtractProtosTask task = TASKS.remove(mapKey);

      Project project = task.getProject();
      File destDir = task.destDir.getAsFile().get();

      if (file.isDirectory()) {
        project.copy(
            copy -> {
              copy.setIncludeEmptyDirs(false);
              copy.from(
                  file,
                  spec -> {
                    spec.include("**/*.proto");
                  });
              copy.into(destDir);
            });
      } else if (file.getPath().endsWith(".proto")) {
        project.copy(
            copy -> {
              copy.setIncludeEmptyDirs(false);
              copy.from(file);
              copy.into(destDir);
            });
      } else if (file.getPath().endsWith(".jar") || file.getPath().endsWith(".zip")) {
        project.copy(
            copy -> {
              copy.setIncludeEmptyDirs(false);
              copy.from(
                  project.zipTree(file),
                  spec -> {
                    spec.include("**/*.proto");
                  });
              copy.into(destDir);
            });
      } else if (file.getPath().endsWith(".tar")
          || file.getPath().endsWith(".tar.gz")
          || file.getPath().endsWith(".tar.bz2")) {
        project.copy(
            copy -> {
              copy.setIncludeEmptyDirs(false);
              copy.from(
                  project.tarTree(file),
                  spec -> {
                    spec.include("**/*.proto");
                  });
              copy.into(destDir);
            });
      }
    }
  }
}
