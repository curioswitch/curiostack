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
import com.moowork.gradle.node.NodePlugin;
import com.moowork.gradle.node.npm.NpmTask;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import java.io.File;
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
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;

/**
 * A simple gradle plugin that configures the protobuf-gradle-plugin with appropriate defaults for a
 * GRPC API definition.
 *
 * <p>The project will be configured as a Java library with the GRPC dependencies, and the protobuf
 * compiler will generate both Java code and a descriptor set with source code\ info for using in
 * documentation services.
 */
public class GrpcApiPlugin implements Plugin<Project> {

  private static final String CURIOSTACK_BASE_NODE_DEV_VERSION = "0.0.1";
  private static final String GOOGLE_PROTOBUF_VERSION = "3.5.0";
  private static final String GRPC_WEB_CLIENT_VERSION = "0.3.1";
  private static final String TS_PROTOC_GEN_VERSION = "0.4.0";
  private static final String TYPES_GOOGLE_PROTOBUF_VERSION = "3.2.7";
  private static final String TYPESCRIPT_VERSION = "2.6.2";

  private static final String RESOLVED_PLUGIN_SCRIPT_TEMPLATE =
      "#!|NODE_PATH|\n" + "" + "require('|IMPORTED_MODULE|');";

  private static final String PACKAGE_JSON_TEMPLATE;
  private static final String TSCONFIG_TEMPLATE;

  static {
    try {
      PACKAGE_JSON_TEMPLATE =
          Resources.toString(
              Resources.getResource("grpcapi/package-template.json"), StandardCharsets.UTF_8);
      TSCONFIG_TEMPLATE =
          Resources.toString(
              Resources.getResource("grpcapi/tsconfig-template.json"), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read package-template.json", e);
    }
  }

  private static final List<String> GRPC_DEPENDENCIES =
      Collections.unmodifiableList(Arrays.asList("grpc-core", "grpc-protobuf", "grpc-stub"));

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaLibraryPlugin.class);
    project.getPluginManager().apply(NodePlugin.class);

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
          protobuf.setGeneratedFilesBaseDir(project.getBuildDir() + "/generated/source/proto");
          project
              .getPlugins()
              .withType(
                  IdeaPlugin.class,
                  plugin -> {
                    IdeaModule module = plugin.getModel().getModule();
                    File generatedDir = project.file(protobuf.getGeneratedFilesBaseDir());
                    File mainDir = new File(generatedDir, "main");
                    File testDir = new File(generatedDir, "test");
                    module.getSourceDirs().add(mainDir);
                    module.getGeneratedSourceDirs().add(mainDir);
                    module.getTestSourceDirs().add(testDir);
                    module.getGeneratedSourceDirs().add(testDir);
                  });

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
                                  .file("node_modules/.bin/protoc-gen-ts-resolved")
                                  .getAbsolutePath());
                      locators
                          .create("flow")
                          .setPath(
                              project
                                  .file("node_modules/.bin/protoc-gen-flow-resolved")
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

          // We expect using yarn workspaces, so these should not be necessary even for web
          // projects.
          project.getTasks().getByName("yarn").setEnabled(false);
          project.getTasks().getByName("yarnSetup").setEnabled(false);

          if (!config.web()) {
            // There isn't a good way to control the application of the node plugin via the web
            // property, so just disable some popular automatic tasks.
            project.getTasks().getByName("nodeSetup").setEnabled(false);
          } else {
            NpmTask installTsProtocGen =
                project.getTasks().create("installTsProtocGen", NpmTask.class);
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
                    .dependsOn("installTsProtocGen")
                    .doFirst(
                        t -> {
                          String nodePath =
                              project
                                  .getExtensions()
                                  .getByType(NodeExtension.class)
                                  .getVariant()
                                  .getNodeExec();
                          writeResolvedScript(
                              project, nodePath, "protoc-gen-ts-resolved", "./protoc-gen-ts");
                        });
            addResolvedPluginScript
                .getOutputs()
                .files(
                    ImmutableMap.of(
                        "protoc-gen-ts-resolved", "node_modules/.bin/protoc-gen-ts-resolved"));

            String packageName =
                config.webPackageName().isEmpty()
                    ? project
                        .getConvention()
                        .getPlugin(BasePluginConvention.class)
                        .getArchivesBaseName()
                    : config.webPackageName();
            Path packageJsonPath =
                Paths.get(project.getBuildDir().getAbsolutePath(), "web", "package.json");
            Path indexJsPath =
                Paths.get(project.getBuildDir().getAbsolutePath(), "web", "index.ts");
            Path tsConfigPath =
                Paths.get(project.getBuildDir().getAbsolutePath(), "web", "tsconfig.json");
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
                                    .replaceFirst("\\|TYPESCRIPT_VERSION\\|", TYPESCRIPT_VERSION)
                                    .getBytes(StandardCharsets.UTF_8));
                            Files.write(
                                tsConfigPath,
                                TSCONFIG_TEMPLATE
                                    .replaceFirst(
                                        "\\|BASE_TSCONFIG\\|",
                                        project
                                            .getRootProject()
                                            .file("tsconfig.json")
                                            .getAbsolutePath())
                                    .getBytes(StandardCharsets.UTF_8));
                            Files.write(indexJsPath, new byte[0]);
                          } catch (IOException e) {
                            throw new UncheckedIOException("Could not write package.json.", e);
                          }
                        });
            addPackageJson
                .getOutputs()
                .files(
                    ImmutableMap.of(
                        "PACKAGE_JSON", packageJsonPath.toFile(),
                        "INDEX_TS", indexJsPath.toFile(),
                        "TS_CONFIG", tsConfigPath.toFile()));

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
    Path path;
    try {
      path =
          Files.write(
              Paths.get(
                  project.getProjectDir().getAbsolutePath(), "node_modules/.bin/" + outputFilename),
              RESOLVED_PLUGIN_SCRIPT_TEMPLATE
                  .replaceFirst("\\|NODE_PATH\\|", nodePath)
                  .replaceFirst("\\|IMPORTED_MODULE\\|", importedModule)
                  .getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write resolved plugin script.", e);
    }
    path.toFile().setExecutable(true);
  }
}
