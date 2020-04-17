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
package org.curioswitch.gradle.protobuf.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public class ExtractProtosTask extends DefaultTask {

  private final ConfigurableFileCollection files;
  private final DirectoryProperty destDir;

  @Inject
  public ExtractProtosTask() {

    files = getProject().getObjects().fileCollection();
    destDir = getProject().getObjects().directoryProperty();

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
      Project project = getProject();
      File destDir = this.destDir.getAsFile().get();

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
              copy.from(project.zipTree(file), spec -> spec.include("**/*.proto"));
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
