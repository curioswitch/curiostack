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
import com.google.protobuf.gradle.ProtobufConfigurator;
import com.google.protobuf.gradle.ProtobufConfigurator.JavaGenerateProtoTaskCollection;
import com.google.protobuf.gradle.ProtobufConvention;
import com.google.protobuf.gradle.ProtobufPlugin;
import com.moowork.gradle.node.NodeExtension;
import com.moowork.gradle.node.NodePlugin;
import com.moowork.gradle.node.yarn.YarnInstallTask;
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
import org.codehaus.groovy.runtime.GStringImpl;
import org.curioswitch.gradle.common.LambdaClosure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.JavaLibraryPlugin;

/**
 * A simple gradle plugin that configures the protobuf-gradle-plugin with appropriate defaults for a
 * GRPC API definition.
 *
 * <p>The project will be configured as a Java library with the GRPC dependencies, and the protobuf
 * compiler will generate both Java code and a descriptor set with source code\ info for using in
 * documentation services.
 */
public class GrpcApiPlugin implements Plugin<Project> {

  private static final String RESOLVED_PLUGIN_SCRIPT_TEMPLATE
      = "#!|NODE_PATH|\n"
      + ""
      + "require('../ts-protoc-gen/lib/ts_index');";

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
          // We generate into the apt directory since the gradle-apt-plugin provides good integration
          // with IntelliJ and no need to reinvent the wheel.
          protobuf.generatedFilesBaseDir = project.getBuildDir() + "/generated/source/apt";
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
                      locators.create("ts").setPath("node_modules/.bin/protoc-gen-ts-resolved");
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
                                task.getBuiltins().create("js")
                                    .option("import_style=commonjs,binary")
                                    .setOutputSubDir("../../web");
                                task.getPlugins().create("ts")
                                    .option("service=true")
                                    .setOutputSubDir("../../web");
                              }
                            });
                    tasks
                        .ofSourceSet("main")
                        .forEach(
                            task -> {
                              task.getOutputs().file(descriptorSetOutputPath);
                              task.generateDescriptorSet = true;
                              task.descriptorSetOptions.includeSourceInfo = true;
                              task.descriptorSetOptions.includeImports = true;
                              task.descriptorSetOptions.path =
                                  new GStringImpl(
                                      new Object[] {}, new String[] {descriptorSetOutputPath});
                            });
                  }));


        });

    // Add the protobuf plugin last to make sure our afterEvaluate runs before it.
    project.getPluginManager().apply(ProtobufPlugin.class);

    // Additional configuration of tasks created by protobuf plugin.
    project.afterEvaluate(p -> {
      ImmutableGrpcExtension config = project.getExtensions().getByType(GrpcExtension.class);

      if (config.web()) {

        project.getTasks().getByName(YarnInstallTask.NAME, t -> {
          YarnInstallTask yarn = (YarnInstallTask) t;
          yarn.setArgs(ImmutableList.of("--ignore-scripts"));
        });

        // gradle-protobuf-plugin does not allow manipulating PATH for protoc invocation, so there's no way
        // to point it at our downloaded nodejs. We go ahead and create our own plugin executable with the
        // nodejs path resolved.
        Task addResolvedPluginScript = project.getTasks().create("addResolvedPluginScript")
            .dependsOn("yarn")
            .doFirst(t -> {
              String nodePath = project.getExtensions().getByType(NodeExtension.class).getVariant()
                  .getNodeExec();
              Path path;
              try {
                path = Files.write(
                    Paths.get(
                        project.getProjectDir().getAbsolutePath(),
                        "node_modules/.bin/protoc-gen-ts-resolved"),
                    RESOLVED_PLUGIN_SCRIPT_TEMPLATE.replaceFirst("\\|NODE_PATH\\|", nodePath).getBytes(
                        StandardCharsets.UTF_8)
                );
              } catch (IOException e) {
                throw new UncheckedIOException("Could not write resolved plugin script.", e);
              }
              path.toFile().setExecutable(true);
            });
        project.getTasks().getByName("generateProto").dependsOn(addResolvedPluginScript);
      }
    });
  }
}
