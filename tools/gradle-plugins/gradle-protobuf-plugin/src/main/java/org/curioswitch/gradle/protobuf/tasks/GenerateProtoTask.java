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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.gradle.osdetector.OsDetector;
import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.curioswitch.gradle.protobuf.ProtobufExtension;
import org.curioswitch.gradle.protobuf.ProtobufExtension.DescriptorSetOptions;
import org.curioswitch.gradle.protobuf.ProtobufExtension.Executable;
import org.curioswitch.gradle.protobuf.ProtobufExtension.LanguageSettings;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

@CacheableTask
public class GenerateProtoTask extends DefaultTask {

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let"s enjoy the speed.
  private static final ConcurrentHashMap<String, GenerateProtoTask> TASKS =
      new ConcurrentHashMap<>();

  private static final Splitter COORDINATE_SPLITTER = Splitter.on('@');
  private static final Splitter ARTIFACT_SPLITTER = Splitter.on(':');

  private final String sourceSetName;
  private final ConfigurableFileCollection protoFiles;
  private final ListProperty<Directory> includeDirs;
  private final Property<File> protocPath;
  private final Property<String> protocArtifact;
  private final Property<File> outputBaseDir;
  private final ListProperty<LanguageSettings> languages;
  private final DescriptorSetOptions descriptorSetOptions;

  private final List<Action<? super ExecSpec>> execOverrides;

  private final WorkerExecutor workerExecutor;

  @Inject
  public GenerateProtoTask(
      String sourceSetName, ProtobufExtension config, WorkerExecutor workerExecutor) {
    this.sourceSetName = sourceSetName;
    this.workerExecutor = workerExecutor;

    execOverrides = new ArrayList<>();

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

    descriptorSetOptions = DescriptorSetOptions.create(objects);
    descriptorSetOptions.getEnabled().set(config.getDescriptorSetOptions().getEnabled());
    descriptorSetOptions.getPath().set(config.getDescriptorSetOptions().getPath());
    descriptorSetOptions
        .getIncludeSourceInfo()
        .set(config.getDescriptorSetOptions().getIncludeSourceInfo());
    descriptorSetOptions
        .getIncludeImports()
        .set(config.getDescriptorSetOptions().getIncludeImports());

    onlyIf(unused -> !protoFiles.isEmpty());
  }

  @InputFiles
  public ConfigurableFileCollection getProtoFiles() {
    return protoFiles;
  }

  @InputFiles
  public Provider<List<FileTree>> getIncludeProtos() {
    return includeDirs.map(
        directories ->
            directories
                .stream()
                .map(
                    directory ->
                        directory
                            .getAsFileTree()
                            .matching(patterns -> patterns.include("**/*.proto")))
                .collect(toImmutableList()));
  }

  @OutputFile
  public Property<File> getDescriptorOutputPath() {
    return descriptorSetOptions.getPath();
  }

  @OutputDirectories
  public Map<String, File> getOutputDirs() {
    return languages
        .getOrElse(ImmutableList.of())
        .stream()
        .collect(toImmutableMap(LanguageSettings::getName, this::getLanguageOutputDir));
  }

  public GenerateProtoTask include(DirectoryProperty directory) {
    includeDirs.add(directory);
    return this;
  }

  public GenerateProtoTask language(LanguageSettings language) {
    languages.add(language);
    return this;
  }

  public GenerateProtoTask execOverride(Action<? super ExecSpec> execOverride) {
    this.execOverrides.add(execOverride);
    return this;
  }

  @TaskAction
  public void exec() {
    Project project = getProject();

    List<LanguageSettings> languages = this.languages.get();

    ImmutableList.Builder<String> artifacts = ImmutableList.builder();
    if (protocArtifact.isPresent()) {
      artifacts.add(protocArtifact.get());
    }

    for (LanguageSettings language : languages) {
      if (language.getPlugin().getArtifact().isPresent()) {
        artifacts.add(language.getPlugin().getArtifact().get());
      }
    }

    Map<String, File> downloadedTools = downloadTools(artifacts.build());

    File protocPath = this.protocPath.getOrElse(downloadedTools.get(protocArtifact.get()));

    ImmutableList.Builder<String> protocCommandBaseBuilder = ImmutableList.builder();
    protocCommandBaseBuilder.add(protocPath.getAbsolutePath());

    for (LanguageSettings language : languages) {
      String optionsPrefix = optionsPrefix(language.getOptions().getOrElse(ImmutableList.of()));

      String outputDir = getLanguageOutputDir(language).getAbsolutePath();
      project.mkdir(outputDir);

      protocCommandBaseBuilder.add("--" + language.getName() + "_out=" + optionsPrefix + outputDir);

      Executable plugin = language.getPlugin();
      if (plugin.isPresent()) {
        String pluginPath =
            Objects.requireNonNullElseGet(
                    plugin.getPath().getOrNull(),
                    () -> downloadedTools.get(plugin.getArtifact().get()))
                .getAbsolutePath();
        protocCommandBaseBuilder.add(
            "--plugin=protoc-gen-" + language.getName() + "=" + pluginPath);
      }
    }

    Streams.concat(
            getProtoFiles().getFiles().stream(),
            includeDirs.getOrElse(ImmutableList.of()).stream().map(Directory::getAsFile))
        .map(f -> "-I" + f.getAbsolutePath())
        .forEach(protocCommandBaseBuilder::add);

    if (descriptorSetOptions.getEnabled().get()) {
      File descriptorSetPath =
          descriptorSetOptions
              .getPath()
              .getOrElse(project.file("build/descriptors/" + sourceSetName + ".dsc"));
      project.mkdir(descriptorSetPath.getParent());

      protocCommandBaseBuilder.add("--descriptor_set_out=" + descriptorSetPath.getAbsolutePath());
      if (descriptorSetOptions.getIncludeSourceInfo().get()) {
        protocCommandBaseBuilder.add("--include_source_info");
      }
      if (descriptorSetOptions.getIncludeImports().get()) {
        protocCommandBaseBuilder.add("--include_imports");
      }
    }

    List<String> protocCommandBase = protocCommandBaseBuilder.build();

    // Sort to ensure generated descriptors have a canonical representation
    // to avoid triggering unnecessary rebuilds downstream
    for (File file : ImmutableList.sortedCopyOf(protoFiles.getAsFileTree())) {
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

                task.execOverrides.forEach(a -> a.execute(exec));
              });
    }
  }

  private static String optionsPrefix(List<String> options) {
    if (options.isEmpty()) {
      return "";
    }
    return options.stream().collect(Collectors.joining(",", "", ":"));
  }

  private Map<String, File> downloadTools(List<String> artifacts) {
    RepositoryHandler repositories = getProject().getRepositories();
    List<ArtifactRepository> currentRepositories = ImmutableList.copyOf(repositories);
    // Make sure Maven Central is present as a repository since it's the usual place to
    // get protoc, even for non-Java projects. We restore to the previous state after the task.
    repositories.mavenCentral();

    Dependency[] dependencies =
        artifacts
            .stream()
            .map(
                artifact -> {
                  checkArgument(!artifact.isEmpty(), "artifact must not be empty");

                  List<String> coordinateParts = COORDINATE_SPLITTER.splitToList(artifact);

                  List<String> artifactParts =
                      ARTIFACT_SPLITTER.splitToList(coordinateParts.get(0));

                  ImmutableMap.Builder<String, String> depParts =
                      ImmutableMap.builderWithExpectedSize(5);

                  // Do a loose matching to allow for the possibility of dependency management
                  // manipulation.
                  if (artifactParts.size() > 0) {
                    depParts.put("group", artifactParts.get(0));
                  }
                  if (artifactParts.size() > 1) {
                    depParts.put("name", artifactParts.get(1));
                  }
                  if (artifactParts.size() > 2) {
                    depParts.put("version", artifactParts.get(2));
                  }

                  if (artifactParts.size() > 3) {
                    depParts.put("classifier", artifactParts.get(3));
                  } else {
                    depParts.put(
                        "classifier",
                        getProject().getExtensions().getByType(OsDetector.class).getClassifier());
                  }

                  if (coordinateParts.size() > 1) {
                    depParts.put("ext", coordinateParts.get(1));
                  } else {
                    depParts.put("ext", "exe");
                  }

                  return getProject().getDependencies().create(depParts.build());
                })
            .toArray(Dependency[]::new);
    Configuration configuration =
        getProject().getConfigurations().detachedConfiguration(dependencies);

    // Resolve once to download all tools in parallel.
    configuration.resolve();

    // This will not redownload.
    ResolvedConfiguration resolved = configuration.getResolvedConfiguration();
    Map<String, File> downloaded =
        Streams.zip(
                artifacts.stream(),
                Arrays.stream(dependencies),
                (artifact, dep) -> {
                  Set<File> files =
                      resolved.getFiles(
                          d -> {
                            // Dependency.contentEquals doesn't match for some reason...
                            return Objects.equals(dep.getGroup(), d.getGroup())
                                && dep.getName().equals(d.getName())
                                && Objects.equals(dep.getVersion(), d.getVersion());
                          });
                  checkState(files.size() == 1);
                  return new SimpleImmutableEntry<>(artifact, Iterables.getOnlyElement(files));
                })
            .collect(toImmutableMap(Entry::getKey, Entry::getValue));

    repositories.clear();
    repositories.addAll(currentRepositories);

    return downloaded;
  }

  private File getLanguageOutputDir(LanguageSettings language) {
    return language
        .getOutputDir()
        .getOrElse(outputBaseDir.get().toPath().resolve(sourceSetName).toFile());
  }
}
