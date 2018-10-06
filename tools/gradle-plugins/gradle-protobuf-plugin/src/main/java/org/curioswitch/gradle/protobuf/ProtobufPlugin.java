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

package org.curioswitch.gradle.protobuf;

import com.google.common.collect.ImmutableList;
import com.google.gradle.osdetector.OsDetectorPlugin;
import org.curioswitch.gradle.protobuf.tasks.DownloadToolsTask;
import org.curioswitch.gradle.protobuf.tasks.ExtractProtosTask;
import org.curioswitch.gradle.protobuf.tasks.GenerateProtoTask;
import org.curioswitch.gradle.protobuf.utils.SourceSetUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

public class ProtobufPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(BasePlugin.class);
    project.getPlugins().apply(OsDetectorPlugin.class);

    ProtobufExtension extension = ProtobufExtension.createAndAdd(project);

    TaskProvider<DownloadToolsTask> downloadTools =
        project.getTasks().register(DownloadToolsTask.NAME, DownloadToolsTask.class, extension);
    downloadTools.configure(t -> t.artifact(extension.getProtoc().getArtifact()));

    extension
        .getLanguages()
        .configureEach(
            language -> {
              downloadTools.configure(t -> t.artifact(language.getPlugin().getArtifact()));
            });

    Configuration protobufConfiguration =
        project
            .getConfigurations()
            .create(
                "protobuf",
                c -> {
                  c.setVisible(false);
                  c.setTransitive(true);
                  c.setExtendsFrom(ImmutableList.of());
                });
    TaskProvider<ExtractProtosTask> extract =
        project
            .getTasks()
            .register(
                "protoExtract",
                ExtractProtosTask.class,
                t -> {
                  t.getFiles().from(protobufConfiguration);
                  t.setDestDir(project.file("build/extracted-protos/main"));
                });

    TaskProvider<ExtractProtosTask> extractInclude =
        project
            .getTasks()
            .register(
                "protoExtractInclude",
                ExtractProtosTask.class,
                t -> {
                  t.setDestDir(project.file("build/extracted-include-protos/main"));
                });

    TaskProvider<GenerateProtoTask> generateProto =
        project.getTasks().register("protoGenerate", GenerateProtoTask.class, t -> {
          t.dependsOn(extract, extractInclude, downloadTools);

          t.include(extract.get().getDestDir());
          t.include(extractInclude.get().getDestDir());
          t.setDownloadedTools(downloadTools.get().getToolPaths());
        });

    project
        .getPlugins()
        .withType(
            JavaBasePlugin.class,
            plugin -> {
              JavaPluginConvention convention =
                  project.getConvention().getPlugin(JavaPluginConvention.class);
              for (SourceSet sourceSet : convention.getSourceSets()) {
                if (sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
                  extractInclude.configure(
                      t ->
                          t.getFiles()
                              .from(
                                  project
                                      .getConfigurations()
                                      .getByName(
                                          SourceSetUtils.getConfigName(
                                              sourceSet.getName(), "compile")))
                              .from(sourceSet.getCompileClasspath()));
                }
              }
            });
  }
}
