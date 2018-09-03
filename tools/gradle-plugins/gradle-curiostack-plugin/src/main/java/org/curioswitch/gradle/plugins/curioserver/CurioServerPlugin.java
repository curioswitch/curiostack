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
import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.gorylenko.GitPropertiesPlugin;
import groovy.lang.GroovyObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.curioswitch.gradle.plugins.curioserver.ImmutableDeploymentExtension.ImmutableDeploymentConfiguration;
import org.curioswitch.gradle.plugins.curioserver.tasks.DeployConfigMapTask;
import org.curioswitch.gradle.plugins.curioserver.tasks.DeployPodTask;
import org.curioswitch.gradle.plugins.gcloud.tasks.KubectlTask;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
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

    var jib = project.getExtensions().getByType(JibExtension.class);
    var jibBuildRelease =
        project
            .getTasks()
            .create(
                "jibBuildRelease",
                BuildImageTask.class,
                t -> t.getAsDynamicObject().setProperty("jibExtension", jib));

    var patchAlpha = project.getTasks().create("patchAlpha", KubectlTask.class);

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
                                      CommandUtil.getGcloudSdkBinDir(project)
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

          jibBuildRelease.doFirst(
              unused ->
                  jibBuildRelease.setTargetImage(
                      config.imagePrefix()
                          + config.baseName()
                          + ":"
                          + project
                              .getRootProject()
                              .getExtensions()
                              .getByType(ExtraPropertiesExtension.class)
                              .get("curiostack.releaseBranch")));
          jibBuildRelease.doLast(unused -> jibBuildRelease.setTargetImage(image));

          String revisionId =
              (String) project.getRootProject().findProperty("curiostack.revisionId");
          if (revisionId != null) {
            project
                .getTasks()
                .create(
                    "jibBuildRevision",
                    BuildImageTask.class,
                    t -> {
                      t.getAsDynamicObject().setProperty("jibExtension", jib);
                      t.doFirst(
                          unused ->
                              t.setTargetImage(
                                  config.imagePrefix() + config.baseName() + ":" + revisionId));
                      t.doLast(unused -> t.setTargetImage(image));
                      project.getTasks().getByName("jib").dependsOn(t);
                    });
            var alpha = config.getTypes().getByName("alpha");
            patchAlpha.setArgs(
                ImmutableList.of(
                    "--namespace=" + alpha.namespace(),
                    "patch",
                    "deployment/" + alpha.deploymentName(),
                    "-p",
                    "{\"spec\": "
                        + "{\"template\": {\"metadata\": {\"labels\": {\"revision\": \""
                        + revisionId
                        + "\" }}}}}"));
            patchAlpha.setIgnoreExitValue(true);
          }

          GroovyObject docker = project.getExtensions().getByType(DockerExtension.class);
          DockerJavaApplication javaApplication =
              (DockerJavaApplication) docker.getProperty("javaApplication");
          javaApplication.setBaseImage("openjdk:10-jre-slim");

          for (ImmutableDeploymentConfiguration type : config.getTypes()) {
            String capitalized =
                Ascii.toUpperCase(type.getName().charAt(0)) + type.getName().substring(1);

            Path iapConfigPath = project.file("build/generated/configs/iap-config.yaml").toPath();
            var deployIapBackendConfigTask =
                project
                    .getTasks()
                    .create(
                        "deployIapBackendConfig" + capitalized,
                        KubectlTask.class,
                        t -> {
                          t.setDescription("Deploy IAP backend config to namespace.");
                          t.doFirst(
                              unused -> {
                                String backendConfig =
                                    getFilledTemplate(
                                        "curioserver/iap-backend-config-template.yaml",
                                        ImmutableMap.of("NAMESPACE", type.namespace()));
                                project.mkdir("build/generated/configs");
                                try {
                                  Files.write(
                                      iapConfigPath,
                                      backendConfig.getBytes(StandardCharsets.UTF_8));
                                } catch (IOException e) {
                                  throw new UncheckedIOException(
                                      "Could not write filled template.", e);
                                }
                              });
                          var args = project.getObjects().listProperty(String.class);
                          args.add("apply");
                          args.add("-f");
                          args.add(iapConfigPath.toString());
                          t.setArgs(args);
                        });

            Path cdnConfigPath = project.file("build/generated/configs/iap-config.yaml").toPath();
            var deployCdnBackendConfigTask =
                project
                    .getTasks()
                    .create(
                        "deployCdnBackendConfig" + capitalized,
                        KubectlTask.class,
                        t -> {
                          t.setDescription("Deploy CDN backend config to namespace.");
                          t.doFirst(
                              unused -> {
                                String backendConfig =
                                    getFilledTemplate(
                                        "curioserver/cdn-backend-config-template.yaml",
                                        ImmutableMap.of("NAMESPACE", type.namespace()));
                                project.mkdir("build/generated/configs");
                                try {
                                  Files.write(
                                      cdnConfigPath,
                                      backendConfig.getBytes(StandardCharsets.UTF_8));
                                } catch (IOException e) {
                                  throw new UncheckedIOException(
                                      "Could not write filled template.", e);
                                }
                              });
                          var args = project.getObjects().listProperty(String.class);
                          args.add("apply");
                          args.add("-f");
                          args.add(cdnConfigPath.toString());
                          t.setArgs(args);
                        });

            DeployConfigMapTask deployConfigMapTask =
                project
                    .getTasks()
                    .create("deployConfigMap" + capitalized, DeployConfigMapTask.class)
                    .setType(type.getName());
            project
                .getTasks()
                .create("deploy" + capitalized, DeployPodTask.class)
                .setType(type.getName())
                .dependsOn(
                    deployIapBackendConfigTask, deployCdnBackendConfigTask, deployConfigMapTask);
          }
        });
    project.getPluginManager().apply(DockerJavaApplicationPlugin.class);
  }

  private static String getFilledTemplate(String resource, Map<String, String> replacements) {
    String template;
    try {
      template = Resources.toString(Resources.getResource(resource), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not open resource " + resource, e);
    }
    for (var entry : replacements.entrySet()) {
      template = template.replace("|" + entry.getKey() + "|", entry.getValue());
    }
    return template;
  }
}
