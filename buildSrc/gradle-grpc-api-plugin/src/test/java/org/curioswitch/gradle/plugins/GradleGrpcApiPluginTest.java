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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.gradle.ExecutableLocator;
import com.google.protobuf.gradle.GenerateProtoTask;
import com.google.protobuf.gradle.ProtobufConfigurator;
import com.google.protobuf.gradle.ProtobufConvention;
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import java.io.File;
import org.curioswitch.gradle.common.LambdaClosure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.project.DefaultProject;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

public class GradleGrpcApiPluginTest {

  @Test
  public void normal() throws Exception {
    Project project = ProjectBuilder.builder()
        .withName("api")
        .build();
    project.setGroup("org.curioswitch.test");

    project.getPluginManager().apply(DependencyManagementPlugin.class);
    DependencyManagementExtension dependencyManagement =
        project.getExtensions().getByType(DependencyManagementExtension.class);
    dependencyManagement.dependencies(handler -> {
      handler.dependency("io.grpc:grpc-core:5.0.0");
      handler.dependency("io.grpc:grpc-protobuf:5.0.0");
      handler.dependency("io.grpc:grpc-stub:5.0.0");
      handler.dependency("com.google.protobuf:protoc:6.0.0");
    });

    project.getPluginManager().apply(GradleGrpcApiPlugin.class);
    project.setProperty("archivesBaseName", "curio-test-api");

    ((DefaultProject) project).evaluate();

    Task task = project.getTasks().findByName("generateProto");
    assertThat(task).isInstanceOf(GenerateProtoTask.class).isNotNull();
    GenerateProtoTask generateProtoTask = (GenerateProtoTask) task;
    // 2 output dirs for java and grpc, but they are the same in our config.
    String outputDir = project.getBuildDir().getAbsolutePath() + "/generated/source/apt/main";
    assertThat(generateProtoTask.getAllOutputDirs().stream().map(File::getAbsolutePath))
        .containsExactly(outputDir, outputDir);
    assertThat(generateProtoTask.generateDescriptorSet).isTrue();
    assertThat(generateProtoTask.descriptorSetOptions.includeSourceInfo).isTrue();
    assertThat(generateProtoTask.descriptorSetOptions.includeImports).isTrue();
    assertThat(generateProtoTask.descriptorSetOptions.path.toString()).isEqualTo(
        project.getBuildDir()
            + "/resources/main/META-INF/armeria/grpc/org.curioswitch.test.curio-test-api.dsc");

    ProtobufConfigurator protobuf =
        project.getConvention().getPlugin(ProtobufConvention.class).getProtobuf();
    protobuf.protoc(LambdaClosure.of((ExecutableLocator locator) ->
        assertThat(locator.getArtifact()).isEqualTo("com.google.protobuf:protoc:6.0.0")));
    protobuf.plugins(LambdaClosure.of((NamedDomainObjectContainer<ExecutableLocator> locators) ->
        assertThat(locators.getByName("grpc").getArtifact()).isEqualTo(
            "io.grpc:protoc-gen-grpc-java:5.0.0")));
  }
}
