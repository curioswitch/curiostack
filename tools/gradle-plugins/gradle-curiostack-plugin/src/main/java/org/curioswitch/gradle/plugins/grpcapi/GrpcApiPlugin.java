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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.protobuf.gradle.ExecutableLocator;
import com.google.protobuf.gradle.GenerateProtoTask.DescriptorSetOptions;
import com.google.protobuf.gradle.ProtobufConfigurator;
import com.google.protobuf.gradle.ProtobufConfigurator.JavaGenerateProtoTaskCollection;
import com.google.protobuf.gradle.ProtobufConvention;
import com.google.protobuf.gradle.ProtobufPlugin;
import com.moowork.gradle.node.NodeExtension;
import com.moowork.gradle.node.npm.NpmTask;
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
import org.codehaus.groovy.runtime.GStringImpl;
import org.curioswitch.gradle.common.LambdaClosure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
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

  private static final String CURIOSTACK_BASE_NODE_DEV_VERSION = "0.0.5";
  private static final String GOOGLE_PROTOBUF_VERSION = "3.5.0";
  private static final String GRPC_WEB_CLIENT_VERSION = "0.6.2";
  private static final String TS_PROTOC_GEN_VERSION = "0.7.3";
  private static final String TYPES_GOOGLE_PROTOBUF_VERSION = "3.2.7";

  private static final String RESOLVED_PLUGIN_SCRIPT_TEMPLATE =
      "#!|NODE_PATH|\n" + "" + "require('|IMPORTED_MODULE|');";

  private static final String RESOLVED_PLUGIN_CMD_TEMPLATE =
      "@echo off\r\n\"|NODE_PATH|\" \"%~dp0\\protoc-gen-ts-resolved\" %*";

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

    project.getExtensions().create(ImmutableGrpcExtension.NAME, GrpcExtension.class);

    GRPC_DEPENDENCIES.forEach(dep -> project.getDependencies().add("api", "io.grpc:" + dep));

    project.afterEvaluate(
        p -> {
          ImmutableGrpcExtension config = project.getExtensions().getByType(GrpcExtension.class);

          Map<String, String> managedVersions =
              project
                  .getExtensions()
                  .getByType(DependencyManagementExtension.class)
                  .getManagedVersions();

          ProtobufConfigurator protobuf =
              project.getConvention().getPlugin(ProtobufConvention.class).getProtobuf();

          protobuf.protoc(
              LambdaClosure.of(
                  (ExecutableLocator locator) ->
                      locator.setArtifact(
                          "com.google.protobuf:protoc:"
                              + managedVersions.get("com.google.protobuf:protoc"))));

          protobuf.plugins(
              LambdaClosure.of(
                  (NamedDomainObjectContainer<ExecutableLocator> locators) -> {
                    locators
                        .create("grpc")
                        .setArtifact(
                            "io.grpc:protoc-gen-grpc-java:"
                                + managedVersions.get("io.grpc:grpc-core"));
                    if (config.web()) {
                      locators
                          .create("ts")
                          .setPath(
                              project
                                  .file(
                                      "node_modules/.bin/protoc-gen-ts-resolved"
                                          + (IS_WINDOWS ? ".cmd" : ""))
                                  .getAbsolutePath());
                    }
                  }));

          String archivesBaseName =
              project.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName();
          String descriptorSetOutputPath =
              project.getBuildDir()
                  + "/resources/main/META-INF/armeria/grpc/"
                  + project.getGroup()
                  + "."
                  + archivesBaseName
                  + ".dsc";
          protobuf.generateProtoTasks(
              LambdaClosure.of(
                  (JavaGenerateProtoTaskCollection tasks) -> {
                    tasks
                        .all()
                        .forEach(
                            task -> {
                              task.getBuiltins().getByName("java").setOutputSubDir("");
                              task.getPlugins().create("grpc").setOutputSubDir("");
                              if (config.web()) {
                                // We generate web protos into build/web to make it easier to
                                // reference from other projects.
                                task.getBuiltins()
                                    .create("js")
                                    .option("import_style=commonjs,binary")
                                    .setOutputSubDir("../../../../web");
                                task.getPlugins()
                                    .create("ts")
                                    .option("service=true")
                                    .setOutputSubDir("../../../../web");
                              }
                            });
                    tasks
                        .ofSourceSet("main")
                        .forEach(
                            task -> {
                              task.getOutputs().file(descriptorSetOutputPath);
                              task.setGenerateDescriptorSet(true);
                              DescriptorSetOptions options = task.getDescriptorSetOptions();
                              options.setIncludeSourceInfo(true);
                              options.setIncludeImports(true);
                              options.setPath(
                                  new GStringImpl(
                                      new Object[] {}, new String[] {descriptorSetOutputPath}));
                            });
                  }));
        });

    // Add the protobuf plugin last to make sure our afterEvaluate runs before it.
    project.getPluginManager().apply(ProtobufPlugin.class);

    // Additional configuration of tasks created by protobuf plugin.
    project.afterEvaluate(
        p -> {
          ImmutableGrpcExtension config = project.getExtensions().getByType(GrpcExtension.class);

          if (config.web()) {
            String currentProjectPath = project.getPath().replace(':', '_');
            NpmTask installTsProtocGen =
                project
                    .getRootProject()
                    .getTasks()
                    .create("installTsProtocGen_" + currentProjectPath, NpmTask.class);
            installTsProtocGen.setWorkingDir(project.getProjectDir());
            installTsProtocGen.setArgs(
                ImmutableList.of("install", "--no-save", "ts-protoc-gen@" + TS_PROTOC_GEN_VERSION));
            installTsProtocGen.getInputs().property("ts-protoc-gen-version", TS_PROTOC_GEN_VERSION);
            installTsProtocGen.getOutputs().dir("node_modules/ts-protoc-gen");

            // gradle-protobuf-plugin does not allow manipulating PATH for protoc invocation, so
            // there's no way
            // to point it at our downloaded nodejs. We go ahead and create our own plugin
            // executable with the
            // nodejs path resolved.
            Task addResolvedPluginScript =
                project
                    .getTasks()
                    .create("addResolvedPluginScript")
                    .dependsOn(installTsProtocGen)
                    .doFirst(
                        t -> {
                          String nodePath =
                              project
                                  .getRootProject()
                                  .getExtensions()
                                  .getByType(NodeExtension.class)
                                  .getVariant()
                                  .getNodeExec();
                          writeResolvedScript(
                              project,
                              nodePath,
                              "protoc-gen-ts-resolved",
                              "ts-protoc-gen/lib/index");
                        });
            addResolvedPluginScript
                .getOutputs()
                .files(
                    ImmutableMap.of(
                        "protoc-gen-ts-resolved",
                        "node_modules/.bin/protoc-gen-ts-resolved",
                        "protoc-gen-ts-resolved-cmd",
                        "node_modules/.bin/protoc-gen-ts-resolved.cmd"));

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

            Task generateProto = project.getTasks().getByName("generateProto");
            generateProto.dependsOn(addResolvedPluginScript).finalizedBy(addPackageJson);

            project.getRootProject().getTasks().findByName("yarn").dependsOn(generateProto);

            // Unclear why sometimes compileTestJava fails with "no source files" instead of being
            // skipped (usually when activating web), but it's not that hard to at least check the
            // source set directory.
            SourceSetContainer sourceSets =
                project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
            if (sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getAllJava().isEmpty()) {
              project.getTasks().getByName("compileTestJava").setEnabled(false);
            }
          }
        });
  }

  private static void writeResolvedScript(
      Project project, String nodePath, String outputFilename, String importedModule) {
    try {
      Path path =
          Files.write(
              Paths.get(
                  project.getProjectDir().getAbsolutePath(), "node_modules/.bin/" + outputFilename),
              RESOLVED_PLUGIN_SCRIPT_TEMPLATE
                  .replace("|NODE_PATH|", nodePath)
                  .replace("|IMPORTED_MODULE|", importedModule)
                  .getBytes(StandardCharsets.UTF_8));
      path.toFile().setExecutable(true);
      path =
          Files.write(
              Paths.get(
                  project.getProjectDir().getAbsolutePath(),
                  "node_modules/.bin/" + outputFilename + ".cmd"),
              RESOLVED_PLUGIN_CMD_TEMPLATE
                  .replace("|NODE_PATH|", nodePath)
                  .getBytes(StandardCharsets.UTF_8));
      path.toFile().setExecutable(true);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write resolved plugin script.", e);
    }
  }
}
