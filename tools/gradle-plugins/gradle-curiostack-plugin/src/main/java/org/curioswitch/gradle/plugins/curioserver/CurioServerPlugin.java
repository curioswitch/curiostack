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

package org.curioswitch.gradle.plugins.curioserver;

import com.bmuschko.gradle.docker.DockerExtension;
import com.bmuschko.gradle.docker.DockerJavaApplication;
import com.bmuschko.gradle.docker.DockerJavaApplicationPlugin;
import com.google.cloud.tools.jib.gradle.JibExtension;
import com.google.cloud.tools.jib.gradle.JibPlugin;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.gorylenko.GitPropertiesPlugin;
import groovy.lang.GroovyObject;
import org.curioswitch.gradle.plugins.curioserver.ImmutableDeploymentExtension.ImmutableDeploymentConfiguration;
import org.curioswitch.gradle.plugins.curioserver.tasks.DeployConfigMapTask;
import org.curioswitch.gradle.plugins.curioserver.tasks.DeployPodTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.api.plugins.BasePluginConvention;

/**
 * A simple {@link Plugin} to reduce boilerplate when defining server projects. Contains common
 * logic for building and deploying executables.
 */
public class CurioServerPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(ApplicationPlugin.class);
    project.getPluginManager().apply(GitPropertiesPlugin.class);
    project.getPluginManager().apply(JibPlugin.class);
    project
        .getExtensions()
        .create(ImmutableDeploymentExtension.NAME, DeploymentExtension.class, project);

    project.getNormalization().getRuntimeClasspath().ignore("git.properties");

    project.afterEvaluate(
        p -> {
          ImmutableDeploymentExtension config =
              project.getExtensions().getByType(DeploymentExtension.class);

          String archivesBaseName =
              project.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName();

          var appPluginConvention =
              project.getConvention().getPlugin(ApplicationPluginConvention.class);
          appPluginConvention.setApplicationName(archivesBaseName);

          var jib = project.getExtensions().getByType(JibExtension.class);
          jib.from(from -> from.setImage("openjdk:10-jre-slim"));
          jib.to(to -> to.setImage(config.imagePrefix() + config.baseName()));
          jib.container(
              container -> {
                container.setMainClass(appPluginConvention.getMainClassName());
                container.setPorts(ImmutableList.of("8080"));
                container.setJvmFlags(
                    ImmutableList.of(
                        "$JAVA_OPTS",
                        "$" + archivesBaseName.replace('-', '_').toUpperCase() + "_OPTS"));
              });

          GroovyObject docker = project.getExtensions().getByType(DockerExtension.class);
          DockerJavaApplication javaApplication =
              (DockerJavaApplication) docker.getProperty("javaApplication");
          javaApplication.setBaseImage("openjdk:10-jre-slim");

          project.getTasks().getByName("build").dependsOn("dockerDistTar");

          for (ImmutableDeploymentConfiguration type : config.getTypes()) {
            String capitalized =
                Ascii.toUpperCase(type.getName().charAt(0)) + type.getName().substring(1);
            DeployConfigMapTask deployConfigMapTask =
                project
                    .getTasks()
                    .create("deployConfigMap" + capitalized, DeployConfigMapTask.class)
                    .setType(type.getName());
            project
                .getTasks()
                .create("deploy" + capitalized, DeployPodTask.class)
                .setType(type.getName())
                .dependsOn(deployConfigMapTask);
          }
        });
    project.getPluginManager().apply(DockerJavaApplicationPlugin.class);
  }
}
