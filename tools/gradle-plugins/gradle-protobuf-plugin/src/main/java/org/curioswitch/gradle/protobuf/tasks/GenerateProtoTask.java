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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.curioswitch.gradle.protobuf.ProtobufExtension;
import org.curioswitch.gradle.protobuf.ProtobufExtension.Executable;
import org.curioswitch.gradle.protobuf.ProtobufExtension.LanguageSettings;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

public class GenerateProtoTask extends DefaultTask {

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let"s enjoy the speed.
  private static final ConcurrentHashMap<String, GenerateProtoTask> TASKS =
      new ConcurrentHashMap<>();

  private final ConfigurableFileCollection protoFiles;
  private final ListProperty<Directory> includeDirs;
  private final Property<File> protocPath;
  private final Property<String> protocArtifact;
  private final Property<File> outputBaseDir;
  private final ListProperty<LanguageSettings> languages;

  private final WorkerExecutor workerExecutor;

  @Nullable private Map<String, File> downloadedTools;

  @Inject
  public GenerateProtoTask(ProtobufExtension config, WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;

    ObjectFactory objects = getProject().getObjects();

    protoFiles = getProject().getLayout().configurableFiles();
    includeDirs = objects.listProperty(Directory.class);
    protocPath = objects.property(File.class);
    protocArtifact = objects.property(String.class);
    outputBaseDir = objects.property(File.class);
    languages = objects.listProperty(LanguageSettings.class);

    protocPath.set(config.getProtoc().getPath());
    protocArtifact.set(config.getProtoc().getArtifact());
    outputBaseDir.set(config.getOutputBaseDir());
  }

  @InputFiles
  public ConfigurableFileCollection getProtoFiles() {
    return protoFiles;
  }

  @InputFiles
  public ListProperty<Directory> getIncludeDirs() {
    return includeDirs;
  }

  public GenerateProtoTask include(DirectoryProperty directory) {
    includeDirs.add(directory);
    return this;
  }

  public GenerateProtoTask setDownloadedTools(Map<String, File> downloadedTools) {
    this.downloadedTools = downloadedTools;
    return this;
  }

  @TaskAction
  public void exec() {
    Project project = getProject();

    List<LanguageSettings> languages = this.languages.get();
    File outputBaseDir = this.outputBaseDir.get();
    Map<String, File> downloadedTools =
        Objects.requireNonNullElse(this.downloadedTools, ImmutableMap.of());

    File protocPath = this.protocPath.getOrElse(downloadedTools.get(protocArtifact.get()));

    ImmutableList.Builder<String> protocCommandBaseBuilder = ImmutableList.builder();
    protocCommandBaseBuilder.add(protocPath.getAbsolutePath());

    for (LanguageSettings language : languages) {
      String optionsPrefix = optionsPrefix(language.getOptions().getOrElse(ImmutableList.of()));

      String outputDir =
          language
              .getOutputDir()
              .getOrElse(new File(outputBaseDir, language.getName()))
              .getAbsolutePath();
      project.mkdir(outputDir);

      protocCommandBaseBuilder.add("--" + language.getName() + "_out=" + optionsPrefix + outputDir);

      Executable plugin = language.getPlugin();
      if (plugin.isPresent()) {
        String pluginPath =
            plugin
                .getPath()
                .getOrElse(downloadedTools.get(plugin.getArtifact().get()))
                .getAbsolutePath();
        protocCommandBaseBuilder.add(
            "--plugin=protoc-gen-" + language.getName() + "=" + pluginPath);
      }
    }

    List<String> protocCommandBase = protocCommandBaseBuilder.build();

    // Sort to ensure generated descriptors have a canonical representation
    // to avoid triggering unnecessary rebuilds downstream
    for (File file : ImmutableList.sortedCopyOf(protoFiles)) {
      String mapKey = UUID.randomUUID().toString();
      TASKS.put(mapKey, this);

      workerExecutor.submit(
          DoGenerateProto.class,
          config -> {
            config.setIsolationMode(IsolationMode.NONE);
            config.params(file, protocCommandBase, mapKey);
          });
    }
  }

  public static class DoGenerateProto implements Runnable {

    private final File file;
    private final List<String> protocCommandBase;
    private final String mapKey;

    @Inject
    public DoGenerateProto(File file, List<String> protocCommandBase, String mapKey) {
      this.file = file;
      this.protocCommandBase = protocCommandBase;
      this.mapKey = mapKey;
    }

    @Override
    public void run() {
      GenerateProtoTask task = TASKS.remove(mapKey);

      task.getProject()
          .exec(
              exec -> {
                List<String> command =
                    ImmutableList.<String>builder()
                        .addAll(protocCommandBase)
                        .add(file.getAbsolutePath())
                        .build();
                exec.commandLine(command);
              });
    }
  }

  private static String optionsPrefix(List<String> options) {
    if (options.isEmpty()) {
      return "";
    }
    return options.stream().collect(Collectors.joining(",", "", ":"));
  }
}
