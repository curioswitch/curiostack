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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.tools.jib.gradle.BuildImageTask;
import com.google.cloud.tools.jib.gradle.DockerContextTask;
import com.google.cloud.tools.jib.gradle.JibExtension;
import com.google.cloud.tools.jib.gradle.JibPlugin;
import com.google.cloud.tools.jib.image.ImageFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.gorylenko.GitPropertiesPlugin;
import groovy.lang.GroovyObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.curioswitch.gradle.plugins.gcloud.tasks.KubectlTask;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.ExtraPropertiesExtension;

/**
 * A simple {@link Plugin} to reduce boilerplate when defining server projects. Contains common
 * logic for building and deploying executables.
 */
public class CurioServerPlugin implements Plugin<Project> {

  private static final Splitter DOCKER_IMAGE_SPLITTER = Splitter.on('/');
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(ApplicationPlugin.class);
    project.getPluginManager().apply(GitPropertiesPlugin.class);
    project.getPluginManager().apply(JibPlugin.class);
    project
        .getExtensions()
        .create(ImmutableDeploymentExtension.NAME, DeploymentExtension.class, project);

    project.getNormalization().getRuntimeClasspath().ignore("git.properties");

    // We don't use distributions so don't build them. Users can still reenable in afterevaluate if
    // they really need it.
    project.getTasks().named("distTar").configure(t -> t.setEnabled(false));
    project.getTasks().named("distZip").configure(t -> t.setEnabled(false));

    var jib = project.getExtensions().getByType(JibExtension.class);
    var jibBuildRelease =
        project
            .getTasks()
            .register(
                "jibBuildRelease",
                BuildImageTask.class,
                t -> t.getAsDynamicObject().setProperty("jibExtension", jib));

    var jibTask = project.getTasks().named("jib");
    var patchAlpha = project.getTasks().register("patchAlpha", KubectlTask.class);
    patchAlpha.configure(t -> t.mustRunAfter(jibTask));
    if (System.getenv("CI_MASTER") != null) {
      project.getTasks().named("build").configure(t -> t.dependsOn(jibTask, patchAlpha));
    }

    project
        .getTasks()
        .withType(
            BuildImageTask.class,
            t -> {
              t.dependsOn(project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME));
              t.dependsOn(project.getRootProject().getTasks().getByName("gcloudSetup"));
            });

    project.afterEvaluate(
        p -> {
          ImmutableDeploymentExtension config =
              project.getExtensions().getByType(DeploymentExtension.class);

          project
              .getTasks()
              .withType(
                  BuildImageTask.class,
                  t ->
                      t.doFirst(
                          unused -> {
                            var output = new ByteArrayOutputStream();
                            project.exec(
                                exec -> {
                                  exec.setExecutable(
                                      DownloadedToolManager.get(project)
                                          .getBinDir("gcloud")
                                          .resolve("docker-credential-gcr"));
                                  exec.args("get");

                                  String registry =
                                      DOCKER_IMAGE_SPLITTER
                                          .splitToList(config.imagePrefix())
                                          .get(0);
                                  exec.setStandardInput(
                                      new ByteArrayInputStream(
                                          registry.getBytes(StandardCharsets.UTF_8)));
                                  exec.setStandardOutput(output);
                                });
                            try {
                              var creds = OBJECT_MAPPER.readTree(output.toByteArray());
                              jib.to(
                                  to ->
                                      to.auth(
                                          auth -> {
                                            auth.setUsername(creds.get("Username").asText());
                                            auth.setPassword(creds.get("Secret").asText());
                                          }));
                            } catch (IOException e) {
                              throw new UncheckedIOException("Could not parse credentials.", e);
                            }
                          }));

          String archivesBaseName =
              project.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName();

          var appPluginConvention =
              project.getConvention().getPlugin(ApplicationPluginConvention.class);
          appPluginConvention.setApplicationName(archivesBaseName);

          jib.from(from -> from.setImage("openjdk:10-jre-slim"));
          String image = config.imagePrefix() + config.baseName() + ":" + config.imageTag();
          jib.to(to -> to.setImage(image));
          jib.container(
              container -> {
                container.setFormat(ImageFormat.Docker);
                container.setMainClass(appPluginConvention.getMainClassName());
                container.setPorts(ImmutableList.of("8080"));
              });
          project
              .getTasks()
              .withType(
                  DockerContextTask.class,
                  t -> t.setTargetDir(project.file("build/jib").getAbsolutePath()));

          jibBuildRelease.configure(
              t -> {
                t.doFirst(
                    unused ->
                        t.setTargetImage(
                            config.imagePrefix()
                                + config.baseName()
                                + ":"
                                + project
                                    .getRootProject()
                                    .getExtensions()
                                    .getByType(ExtraPropertiesExtension.class)
                                    .get("curiostack.releaseBranch")));
                t.doLast(unused -> t.setTargetImage(image));
              });

          String revisionId =
              (String) project.getRootProject().findProperty("curiostack.revisionId");
          if (revisionId != null) {
            project
                .getTasks()
                .register(
                    "jibBuildRevision",
                    BuildImageTask.class,
                    t -> {
                      t.getAsDynamicObject().setProperty("jibExtension", jib);
                      t.doFirst(
                          unused ->
                              t.setTargetImage(
                                  config.imagePrefix() + config.baseName() + ":" + revisionId));
                      t.doLast(unused -> t.setTargetImage(image));
                      jibTask.configure(jibT -> jibT.dependsOn(t));
                    });
            var alpha = config.getTypes().getByName("alpha");
            patchAlpha.configure(
                t -> {
                  t.setArgs(
                      ImmutableList.of(
                          "--namespace=" + alpha.namespace(),
                          "patch",
                          "deployment/" + alpha.deploymentName(),
                          "-p",
                          "{\"spec\": "
                              + "{\"template\": {\"metadata\": {\"labels\": {\"revision\": \""
                              + revisionId
                              + "\" }}}}}"));
                  t.setIgnoreExitValue(true);
                });
          }

          GroovyObject docker = project.getExtensions().getByType(DockerExtension.class);
          DockerJavaApplication javaApplication =
              (DockerJavaApplication) docker.getProperty("javaApplication");
          javaApplication.setBaseImage("openjdk:10-jre-slim");
        });
    project.getPluginManager().apply(DockerJavaApplicationPlugin.class);
  }
}
