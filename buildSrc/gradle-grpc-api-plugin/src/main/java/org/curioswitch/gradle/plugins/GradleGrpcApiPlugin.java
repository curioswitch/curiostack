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

package org.curioswitch.gradle.plugins;

import com.google.protobuf.gradle.ExecutableLocator;
import com.google.protobuf.gradle.GenerateProtoTask.PluginOptions;
import com.google.protobuf.gradle.ProtobufConfigurator;
import com.google.protobuf.gradle.ProtobufConfigurator.JavaGenerateProtoTaskCollection;
import com.google.protobuf.gradle.ProtobufConvention;
import com.google.protobuf.gradle.ProtobufPlugin;
import groovy.lang.Closure;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import java.util.Map;
import org.codehaus.groovy.runtime.CurriedClosure;
import org.codehaus.groovy.runtime.GStringImpl;
import org.codehaus.groovy.runtime.MethodClosure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginConvention;

/**
 * A simple gradle plugin that configures the protobuf-gradle-plugin with appropriate
 * defaults for a GRPC API definition.
 *
 * <p>To use, just apply this plugin after setting archivesBaseName (it does not work to
 * apply it before because the protobuf plugin delays execution and won't be able to pick
 * it up).
 *
 * <p>The project will be configured as a Java library with the GRPC dependencies, and
 * the protobuf compiler will generate both Java code and a descriptor set with source code\
 * info for using in documentation services.
 */
public class GradleGrpcApiPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    // TODO(choko): Use class reference after fixing the fact that we only get gradle api 3.1
    // here for some reason.
    project.getPluginManager().apply("java-library");
    project.getPluginManager().apply(ProtobufPlugin.class);
    Map<String, String> managedVersions =
        project.getExtensions().getByType(DependencyManagementExtension.class).getManagedVersions();

    String grpcVersion = managedVersions.get("io.grpc:grpc-core");

    project.getDependencies().add("implementation", "io.grpc:grpc-core:" + grpcVersion);
    project.getDependencies().add("implementation", "io.grpc:grpc-protobuf:" + grpcVersion);
    project.getDependencies().add("implementation", "io.grpc:grpc-stub:" + grpcVersion);

    ProtobufConfigurator protobuf =
        project.getConvention().getPlugin(ProtobufConvention.class).getProtobuf();
    // We generate into the apt directory since the gradle-apt-plugin provides good integration
    // with IntelliJ and no need to reinvent the wheel.
    protobuf.generatedFilesBaseDir = project.getBuildDir() + "/generated/source/apt";
    @SuppressWarnings("unchecked")
    Closure<ExecutableLocator> configureLocatorClosure =
        new MethodClosure(this, "configureLocator");
    protobuf.protoc(new CurriedClosure<>(
        configureLocatorClosure,
        "com.google.protobuf:protoc:" + managedVersions.get("com.google.protobuf:protoc")
    ));

    @SuppressWarnings("unchecked")
    Closure<NamedDomainObjectContainer<ExecutableLocator>> configurePluginLocatorClosure =
        new MethodClosure(this, "configurePluginLocator");
    protobuf.plugins(new CurriedClosure<>(
        configurePluginLocatorClosure,
        "io.grpc:protoc-gen-grpc-java:" + managedVersions.get("io.grpc:grpc-core"),
        "grpc"
    ));

    String archivesBaseName =
        project.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName();
    String descriptorSetOutputPath =
        project.getBuildDir() + "/resources/main/META-INF/armeria/grpc/" + project.getGroup()
            + "." + archivesBaseName + ".dsc";
    @SuppressWarnings("unchecked")
    Closure<JavaGenerateProtoTaskCollection> configureGenerateProtoTasksClosure =
        new MethodClosure(this, "configureGenerateProtoTasks");
    protobuf.generateProtoTasks(new CurriedClosure<>(
        configureGenerateProtoTasksClosure,
        descriptorSetOutputPath
    ));

    protobuf.runTaskConfigClosures();
  }

  public void configureGenerateProtoTasks(String descriptorSetOutputPath,
      JavaGenerateProtoTaskCollection tasks) {
    tasks.all().forEach(task -> {
      task.getBuiltins().getByName("java").setOutputSubDir("");
      task.getPlugins().create("grpc").setOutputSubDir("");
    });
    tasks.ofSourceSet("main").forEach(task -> {
      task.getOutputs().file(descriptorSetOutputPath);
      task.generateDescriptorSet = true;
      task.descriptorSetOptions.includeSourceInfo = true;
      task.descriptorSetOptions.includeImports = true;
      task.descriptorSetOptions.path = new GStringImpl(
          new Object[] {},
          new String[] { descriptorSetOutputPath });
    });
  }

  public void configureBuiltins(PluginOptions options) {
    options.setOutputSubDir("");
  }

  public void configurePluginLocator(
      String artifact,
      String plugin,
      NamedDomainObjectContainer<ExecutableLocator> locators) {
    locators.create(plugin).setArtifact(artifact);
  }

  public void configureLocator(String artifact, ExecutableLocator locator) {
    locator.setArtifact(artifact);
  }
}
