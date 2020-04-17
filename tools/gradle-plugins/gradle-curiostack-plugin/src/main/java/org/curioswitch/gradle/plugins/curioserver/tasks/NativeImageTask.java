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
package org.curioswitch.gradle.plugins.curioserver.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public class NativeImageTask extends DefaultTask {

  private final RegularFileProperty jarFile;
  private final ConfigurableFileCollection classpath;
  private final DirectoryProperty outputDir;
  private final Property<String> outputName;
  private final ListProperty<String> options;

  public NativeImageTask() {
    var objects = getProject().getObjects();

    jarFile = objects.fileProperty();
    classpath = getProject().getLayout().configurableFiles();

    outputDir = objects.directoryProperty();
    outputDir.set(getProject().file("build/graal"));
    outputName = objects.property(String.class).value("app");
    options = objects.listProperty(String.class).empty();
  }

  @InputFile
  public RegularFileProperty getJarFile() {
    return jarFile;
  }

  @InputFiles
  public ConfigurableFileCollection getClasspath() {
    return classpath;
  }

  @Input
  public Property<String> getOutputName() {
    return outputName;
  }

  @Input
  public ListProperty<String> getOptions() {
    return options;
  }

  @OutputDirectory
  public DirectoryProperty getOutputDir() {
    return outputDir;
  }

  @TaskAction
  public void exec() {
    var appPluginConvention =
        getProject().getConvention().getPlugin(ApplicationPluginConvention.class);

    String classpathArg =
        Streams.concat(classpath.getFiles().stream(), Stream.of(jarFile.getAsFile().get()))
            .map(File::getAbsolutePath)
            .collect(Collectors.joining(":"));

    getProject()
        .exec(
            exec -> {
              exec.executable(
                  DownloadedToolManager.get(getProject())
                      .getBinDir("graalvm")
                      .resolve("native-image"));
              var args = new ImmutableList.Builder<String>();
              args.add(
                      "-cp",
                      classpathArg,
                      "-H:Path=" + outputDir.getAsFile().get().getAbsolutePath(),
                      "-H:name=" + outputName.get())
                  .addAll(options.get())
                  .add(appPluginConvention.getMainClassName());
              exec.args(args);
            });
  }
}
