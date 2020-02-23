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
package org.curioswitch.gradle.protobuf;

import com.google.common.collect.ImmutableList;
import com.google.gradle.osdetector.OsDetectorPlugin;
import org.curioswitch.common.helpers.immutables.CurioStyle;
import org.curioswitch.gradle.protobuf.ProtobufExtension.LanguageSettings;
import org.curioswitch.gradle.protobuf.tasks.ExtractProtosTask;
import org.curioswitch.gradle.protobuf.tasks.GenerateProtoTask;
import org.curioswitch.gradle.protobuf.utils.SourceSetUtils;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.immutables.value.Value.Immutable;

public class ProtobufPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(BasePlugin.class);
    project.getPlugins().apply(OsDetectorPlugin.class);

    ProtobufExtension extension = ProtobufExtension.createAndAdd(project);
    project
        .getConfigurations()
        .create(
            "protobufTools",
            protobufTools -> {
              protobufTools.setCanBeResolved(true);
              protobufTools.setCanBeConsumed(false);
              protobufTools.setVisible(false);
            });

    SourceSetTasks mainTasks = configureSourceSet("main", project, extension);

    project
        .getPlugins()
        .withType(
            JavaBasePlugin.class,
            plugin -> {
              extension.getLanguages().maybeCreate("java");

              JavaPluginConvention convention =
                  project.getConvention().getPlugin(JavaPluginConvention.class);
              convention
                  .getSourceSets()
                  .all(
                      sourceSet -> {
                        if (sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
                          configureExtractIncludeTask(
                              mainTasks.extractIncludeProtos(), sourceSet, project);
                          configureSourceSetOutput(
                              sourceSet, mainTasks.generateProto(), extension, project);
                        } else {
                          SourceSetTasks tasks =
                              configureSourceSet(sourceSet.getName(), project, extension);
                          configureExtractIncludeTask(
                              tasks.extractIncludeProtos(), sourceSet, project);
                          configureSourceSetOutput(
                              sourceSet, tasks.generateProto(), extension, project);
                        }
                      });
            });
  }

  private static void configureSourceSetOutput(
      SourceSet sourceSet,
      TaskProvider<GenerateProtoTask> generateProto,
      ProtobufExtension extension,
      Project project) {
    project.afterEvaluate(
        unused ->
            extension
                .getSources()
                .getByName(
                    sourceSet.getName(),
                    source -> {
                      sourceSet.getExtensions().add("proto", source);
                      source.getSrcDirs().forEach(sourceSet.getResources()::srcDir);
                    }));

    extension
        .getLanguages()
        .configureEach(
            language -> {
              String compileTaskName = sourceSet.getCompileTaskName(language.getName());
              var compileTask = project.getTasks().findByName(compileTaskName);
              if (compileTask != null) {
                compileTask.dependsOn(generateProto);
              }

              sourceSet
                  .getJava()
                  .srcDir(
                      extension
                          .getOutputBaseDir()
                          .map(
                              outputBaseDir ->
                                  SourceSetUtils.getLanguageOutputDir(
                                      language, outputBaseDir, sourceSet.getName())));
            });
  }

  private static void configureExtractIncludeTask(
      TaskProvider<ExtractProtosTask> task, SourceSet sourceSet, Project project) {
    task.configure(
        t ->
            t.getFiles()
                .from(
                    project
                        .getConfigurations()
                        // NOTE: Must be runtime, not compile, classpath since proto files are
                        // resources and not part of Java compilation.
                        .getByName(sourceSet.getRuntimeClasspathConfigurationName())));
  }

  private static SourceSetTasks configureSourceSet(
      String sourceSetName, Project project, ProtobufExtension extension) {
    NamedDomainObjectProvider<SourceDirectorySet> sources =
        extension.getSources().register(sourceSetName);

    Configuration protobufConfiguration =
        project
            .getConfigurations()
            .create(
                SourceSetUtils.getConfigName(sourceSetName, "protobuf"),
                c -> {
                  c.setVisible(false);
                  c.setTransitive(true);
                  c.setExtendsFrom(ImmutableList.of());
                });
    TaskProvider<ExtractProtosTask> extract =
        project
            .getTasks()
            .register(
                "extract" + SourceSetUtils.getTaskSuffix(sourceSetName) + "Proto",
                ExtractProtosTask.class,
                t -> {
                  t.getFiles().from(protobufConfiguration);
                  t.setDestDir(project.file("build/extracted-protos/" + sourceSetName));
                });

    TaskProvider<ExtractProtosTask> extractInclude =
        project
            .getTasks()
            .register(
                "extractInclude" + SourceSetUtils.getTaskSuffix(sourceSetName) + "Proto",
                ExtractProtosTask.class,
                t -> t.setDestDir(project.file("build/extracted-include-protos/" + sourceSetName)));

    TaskProvider<GenerateProtoTask> generateProto =
        project
            .getTasks()
            .register(
                "generate" + SourceSetUtils.getTaskSuffix(sourceSetName) + "Proto",
                GenerateProtoTask.class,
                sourceSetName,
                extension);

    // To ensure languages are added in order, we have to make sure this is hooked up eagerly.
    var languages = project.getObjects().listProperty(LanguageSettings.class).empty();
    extension.getLanguages().all(languages::add);

    generateProto.configure(
        t -> {
          t.dependsOn(extract, extractInclude);

          t.getSources().source(sources.get()).srcDir(extract.get().getDestDir());

          t.include(extractInclude.get().getDestDir());
          t.setLanguages(languages);
        });
    return ImmutableSourceSetTasks.builder()
        .extractProtos(extract)
        .extractIncludeProtos(extractInclude)
        .generateProto(generateProto)
        .build();
  }

  @Immutable
  @CurioStyle
  interface SourceSetTasks {
    TaskProvider<ExtractProtosTask> extractProtos();

    TaskProvider<ExtractProtosTask> extractIncludeProtos();

    TaskProvider<GenerateProtoTask> generateProto();
  }
}
