/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.plugins.grpcapi;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.curioswitch.gradle.plugins.nodejs.NodePlugin;
import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask;
import org.curioswitch.gradle.protobuf.ProtobufExtension;
import org.curioswitch.gradle.protobuf.ProtobufPlugin;
import org.curioswitch.gradle.protobuf.tasks.GenerateProtoTask;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * A simple gradle plugin that configures the protobuf-gradle-plugin with appropriate defaults for a
 * GRPC API definition.
 *
 * <p>The project will be configured as a Java library with the GRPC dependencies, and the protobuf
 * compiler will generate both Java code and a descriptor set with source code\ info for using in
 * documentation services.
 */
public class GrpcApiPlugin implements Plugin<Project> {

  private static final boolean IS_WINDOWS = Os.isFamily(Os.FAMILY_WINDOWS);

  private static final String CURIOSTACK_BASE_NODE_DEV_VERSION = "0.0.9";
  private static final String GOOGLE_PROTOBUF_VERSION = "3.6.1";
  private static final String GRPC_WEB_CLIENT_VERSION = "0.6.3";
  private static final String TS_PROTOC_GEN_VERSION = "0.7.6";
  private static final String TYPES_GOOGLE_PROTOBUF_VERSION = "3.2.7";

  private static final String PACKAGE_JSON_TEMPLATE;

  static {
    try {
      PACKAGE_JSON_TEMPLATE =
          Resources.toString(
              Resources.getResource("grpcapi/package-template.json"), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read package-template.json", e);
    }
  }

  private static final List<String> GRPC_DEPENDENCIES =
      Collections.unmodifiableList(Arrays.asList("grpc-core", "grpc-protobuf", "grpc-stub"));

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaLibraryPlugin.class);
    project.getPluginManager().apply(ProtobufPlugin.class);
    project.getPluginManager().apply(NodePlugin.class);

    project.getExtensions().create(ImmutableGrpcExtension.NAME, GrpcExtension.class);

    GRPC_DEPENDENCIES.forEach(dep -> project.getDependencies().add("api", "io.grpc:" + dep));

    ProtobufExtension protobuf = project.getExtensions().getByType(ProtobufExtension.class);

    Map<String, String> managedVersions =
        project.getExtensions().getByType(DependencyManagementExtension.class).getManagedVersions();

    protobuf
        .getProtoc()
        .getArtifact()
        .set("com.google.protobuf:protoc:" + managedVersions.get("com.google.protobuf:protoc"));
    protobuf
        .getLanguages()
        .register(
            "grpc",
            language ->
                language
                    .getPlugin()
                    .getArtifact()
                    .set(
                        "io.grpc:protoc-gen-grpc-java:"
                            + managedVersions.get("io.grpc:grpc-core")));

    project.afterEvaluate(
        p -> {
          ImmutableGrpcExtension config = project.getExtensions().getByType(GrpcExtension.class);

          String archivesBaseName =
              project.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName();
          var descriptorOptions = protobuf.getDescriptorSetOptions();
          descriptorOptions
              .getPath()
              .set(
                  project.file(
                      "build/resources/main/META-INF/armeria/grpc/"
                          + project.getGroup()
                          + "."
                          + archivesBaseName
                          + ".dsc"));
          descriptorOptions.getEnabled().set(true);
          descriptorOptions.getIncludeSourceInfo().set(true);
          descriptorOptions.getIncludeImports().set(true);

          if (config.web()) {
            protobuf
                .getLanguages()
                .register(
                    "js",
                    language -> {
                      language.option("import_style=commonjs,binary");
                      language.getOutputDir().set(project.file("build/web"));
                    });
            protobuf
                .getLanguages()
                .register(
                    "ts",
                    language -> {
                      language
                          .getPlugin()
                          .getPath()
                          .set(
                              project.file(
                                  "node_modules/.bin/protoc-gen-ts" + (IS_WINDOWS ? ".cmd" : "")));
                      language.option("service=true");
                      language.getOutputDir().set(project.file("build/web"));
                    });
            project
                .getTasks()
                .withType(GenerateProtoTask.class)
                .configureEach(
                    t ->
                        t.execOverride(
                            exec -> DownloadedToolManager.get(project).addAllToPath(exec)));
          }
        });

    // Additional configuration of tasks created by protobuf plugin.
    project.afterEvaluate(
        p -> {
          ImmutableGrpcExtension config = project.getExtensions().getByType(GrpcExtension.class);

          if (config.web()) {
            var installTsProtocGen =
                project
                    .getTasks()
                    .register(
                        "installTsProtocGen",
                        NodeTask.class,
                        t -> {
                          t.setCommand("npm");
                          t.args("install", "--no-save", "ts-protoc-gen@" + TS_PROTOC_GEN_VERSION);
                          t.getInputs().property("ts-protoc-gen-version", TS_PROTOC_GEN_VERSION);
                          t.getOutputs().dirs(ImmutableMap.of("nodeModules", "node_modules"));
                          t.dependsOn(project.getTasks().named(BasePlugin.CLEAN_TASK_NAME));
                        });

            String packageName =
                config.webPackageName().isEmpty()
                    ? project
                        .getConvention()
                        .getPlugin(BasePluginConvention.class)
                        .getArchivesBaseName()
                    : config.webPackageName();
            Path packageJsonPath =
                Paths.get(project.getBuildDir().getAbsolutePath(), "web", "package.json");
            Task addPackageJson =
                project
                    .getTasks()
                    .create("packageJson")
                    .dependsOn("generateProto")
                    .doFirst(
                        t -> {
                          try {
                            Files.write(
                                packageJsonPath,
                                PACKAGE_JSON_TEMPLATE
                                    .replaceFirst("\\|PACKAGE_NAME\\|", packageName)
                                    .replaceFirst(
                                        "\\|TYPES_GOOGLE_PROTOBUF_VERSION\\|",
                                        TYPES_GOOGLE_PROTOBUF_VERSION)
                                    .replaceFirst(
                                        "\\|GOOGLE_PROTOBUF_VERSION\\|", GOOGLE_PROTOBUF_VERSION)
                                    .replaceFirst(
                                        "\\|GRPC_WEB_CLIENT_VERSION\\|", GRPC_WEB_CLIENT_VERSION)
                                    .replaceFirst(
                                        "\\|CURIOSTACK_BASE_NODE_DEV_VERSION\\|",
                                        CURIOSTACK_BASE_NODE_DEV_VERSION)
                                    .getBytes(StandardCharsets.UTF_8));
                          } catch (IOException e) {
                            throw new UncheckedIOException("Could not write package.json.", e);
                          }
                        });
            addPackageJson
                .getOutputs()
                .files(ImmutableMap.of("PACKAGE_JSON", packageJsonPath.toFile()));

            project
                .getTasks()
                .named("generateProto")
                .configure(t -> t.dependsOn(installTsProtocGen).finalizedBy(addPackageJson));

            // Unclear why sometimes compileTestJava fails with "no source files" instead of being
            // skipped (usually when activating web), but it's not that hard to at least check the
            // source set directory.
            SourceSetContainer sourceSets =
                project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
            if (sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getAllJava().isEmpty()) {
              project.getTasks().named("compileTestJava").configure(t -> t.setEnabled(false));
              project.getTasks().named("test").configure(t -> t.setEnabled(false));
            }
          }
        });
  }
}
