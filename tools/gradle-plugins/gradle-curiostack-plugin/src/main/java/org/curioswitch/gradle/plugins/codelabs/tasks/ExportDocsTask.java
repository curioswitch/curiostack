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

package org.curioswitch.gradle.plugins.codelabs.tasks;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.File;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public class ExportDocsTask extends DefaultTask {

  private final Property<ConfigurableFileTree> mdFiles;
  private final DirectoryProperty outputDir;

  public ExportDocsTask() {
    var objects = getProject().getObjects();

    mdFiles = objects.property(ConfigurableFileTree.class);
    outputDir = objects.directoryProperty();
  }

  @InputFiles
  public Property<ConfigurableFileTree> getMdFiles() {
    return mdFiles;
  }

  @OutputDirectory
  public DirectoryProperty getOutputDir() {
    return outputDir;
  }

  @TaskAction
  public void exec() {
    List<String> mdFiles =
        this.mdFiles
            .get()
            .getFiles()
            .stream()
            .map(File::getAbsolutePath)
            .collect(toImmutableList());

    getProject().exec(exec -> {
      exec.executable(ClaatTaskUtil.getClaatPath(getProject()));
      exec.args("export");
      exec.args(mdFiles);

      exec.workingDir(outputDir);
    });
  }
}
