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
import com.google.protobuf.gradle.ExecutableLocator;
import com.google.protobuf.gradle.GenerateProtoTask.DescriptorSetOptions;
import com.google.protobuf.gradle.ProtobufConfigurator;
import com.google.protobuf.gradle.ProtobufConfigurator.JavaGenerateProtoTaskCollection;
import com.google.protobuf.gradle.ProtobufConvention;
import com.google.protobuf.gradle.ProtobufPlugin;
import com.moowork.gradle.node.NodeExtension;
import com.moowork.gradle.node.NodePlugin;
import com.moowork.gradle.node.yarn.YarnTask;
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

  private static final String TS_PROTOC_GEN_VERSION = "0.3.3";

  private static final String RESOLVED_PLUGIN_SCRIPT_TEMPLATE =
      "#!|NODE_PATH|\n" + "" + "require('../ts-protoc-gen/lib/ts_index');";

  private static final String PACKAGE_JSON_TEMPLATE =
      "{\n"
          + "  \"name\": \"|PACKAGE_NAME|\",\n"
          + "  \"version\": \"latest\",\n"
          + "  \"main\": \"index.js\",\n"
          + "  \"dependencies\": {\n"
          + "    \"@types/google-protobuf\": \"3.2.6\",\n"
          + "    \"google-protobuf\": \"3.3.0\",\n"
          + "    \"grpc-web-client\": \"0.3.0\",\n"
          + "    \"tsc-glob\": \"2.0.1\",\n"
          + "    \"typescript\": \"2.4.2\"\n"
          + "  }\n"
          + "}";

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
            YarnTask installTsProtocGen =
                project.getTasks().create("installTsProtocGen", YarnTask.class);
            installTsProtocGen.setArgs(
                ImmutableList.of(
                    "add", "--dev", "--no-lockfile", "ts-protoc-gen@" + TS_PROTOC_GEN_VERSION));
            installTsProtocGen.getInputs().property("version", TS_PROTOC_GEN_VERSION);
            installTsProtocGen.getOutputs().dir("build/ts-protoc-gen");

            // gradle-protobuf-plugin does not allow manipulating PATH for protoc invocation, so there's no way
            // to point it at our downloaded nodejs. We go ahead and create our own plugin executable with the
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
                          Path path;
                          try {
                            path =
                                Files.write(
                                    Paths.get(
                                        project.getProjectDir().getAbsolutePath(),
                                        "node_modules/.bin/protoc-gen-ts-resolved"),
                                    RESOLVED_PLUGIN_SCRIPT_TEMPLATE
                                        .replaceFirst("\\|NODE_PATH\\|", nodePath)
                                        .getBytes(StandardCharsets.UTF_8));
                          } catch (IOException e) {
                            throw new UncheckedIOException(
                                "Could not write resolved plugin script.", e);
                          }
                          path.toFile().setExecutable(true);
                        });

            String archivesBaseName =
                project.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName();
            Task addPackageJson =
                project
                    .getTasks()
                    .create("packageJson")
                    .dependsOn("generateProto")
                    .doFirst(
                        t -> {
                          try {
                            Files.write(
                                Paths.get(
                                    project.getBuildDir().getAbsolutePath(), "web", "package.json"),
                                PACKAGE_JSON_TEMPLATE
                                    .replaceFirst("\\|PACKAGE_NAME\\|", archivesBaseName)
                                    .getBytes(StandardCharsets.UTF_8));
                            Files.write(
                                Paths.get(
                                    project.getBuildDir().getAbsolutePath(), "web", "index.js"),
                                new byte[0]);
                          } catch (IOException e) {
                            throw new UncheckedIOException("Could not write package.json.", e);
                          }
                        });

            project
                .getTasks()
                .getByName("generateProto")
                .dependsOn(addResolvedPluginScript)
                .finalizedBy(addPackageJson);
          }
        });

    project.getTasks().getByName("compileTestJava").setEnabled(false);
  }
}
