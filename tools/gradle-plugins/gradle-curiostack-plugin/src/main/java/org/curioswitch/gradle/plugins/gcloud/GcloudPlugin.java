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

package org.curioswitch.gradle.plugins.gcloud;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.curioswitch.gradle.plugins.curioserver.CurioServerPlugin;
import org.curioswitch.gradle.plugins.curioserver.DeploymentExtension;
import org.curioswitch.gradle.plugins.gcloud.tasks.CreateBuildCacheBucket;
import org.curioswitch.gradle.plugins.gcloud.tasks.GcloudTask;
import org.curioswitch.gradle.plugins.gcloud.tasks.SetupTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Rule;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePluginConvention;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A plugin that adds tasks for automatically downloading the gcloud sdk and running commands using
 * it from gradle. Python 2 will have to be available for gcloud sdk commands to work. Eventually,
 * most commands should be migrated to using the gcloud Rest APIs to remove this dependency.
 */
public class GcloudPlugin implements Plugin<Project> {

  private static final Logger logger = LoggerFactory.getLogger(GcloudPlugin.class);

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES))
          .setSerializationInclusion(Include.NON_EMPTY);

  @Override
  public void apply(Project project) {
    project.getExtensions().create(ImmutableGcloudExtension.NAME, GcloudExtension.class, project);

    project
        .getExtensions()
        .getExtraProperties()
        .set(GcloudTask.class.getSimpleName(), GcloudTask.class);

    project
        .getTasks()
        .addRule(
            new Rule() {
              @Override
              public String getDescription() {
                return "Pattern: \"gcloud_<command>\": Executes a Gcloud command.";
              }

              @Override
              public void apply(String taskName) {
                if (taskName.startsWith("gcloud_")) {
                  GcloudTask task = project.getTasks().create(taskName, GcloudTask.class);
                  List<String> tokens = ImmutableList.copyOf(taskName.split("_"));
                  task.setArgs(tokens.subList(1, tokens.size()));
                }
              }
            });

    project.afterEvaluate(
        p -> {
          SetupTask setupTask = project.getTasks().create(SetupTask.NAME, SetupTask.class);
          ImmutableGcloudExtension config =
              project.getExtensions().getByType(GcloudExtension.class);
          setupTask.setEnabled(config.download());

          project.allprojects(
              proj -> {
                proj.getPlugins()
                    .withType(
                        CurioServerPlugin.class,
                        unused -> {
                          DeploymentExtension deployment =
                              proj.getExtensions().getByType(DeploymentExtension.class);
                          deployment.setImagePrefix(
                              config.containerRegistry() + "/" + config.clusterProject() + "/");
                        });
              });

          project.allprojects(
              proj -> {
                proj.getPlugins()
                    .withType(
                        CurioDatabasePlugin.class,
                        unused -> {
                          DatabaseExtension database =
                              proj.getExtensions().getByType(DatabaseExtension.class);
                          database.setDevDockerImagePrefix(
                              config.containerRegistry() + "/" + config.clusterProject() + "/");
                        });
              });

          GcloudTask createClusterProject =
              project.getTasks().create("gcloudCreateClusterProject", GcloudTask.class);
          createClusterProject.setArgs(
              ImmutableList.of("alpha", "projects", "create", config.clusterProject()));

          // This task currently always fails, probably due to a bug in the SDK. It attempts
          // to create the same repo twice, and the second one fails with an error...
          GcloudTask createSourceRepo =
              project.getTasks().create("gcloudCreateSourceRepository", GcloudTask.class);
          createSourceRepo.setArgs(
              ImmutableList.of("alpha", "source", "repos", "create", config.sourceRepository()));

          GcloudTask createCluster =
              project.getTasks().create("gcloudCreateCluster", GcloudTask.class);
          List<String> createClusterArgs = new ArrayList<>();
          createClusterArgs.addAll(
              ImmutableList.of(
                  "beta",
                  "container",
                  "clusters",
                  "create",
                  config.clusterName(),
                  "--zone",
                  config.clusterZone(),
                  "--additional-zones",
                  String.join(",", config.clusterAdditionalZones()),
                  "--num-nodes",
                  Integer.toString(config.clusterNumNodesPerZone()),
                  "--enable-autorepair",
                  "--enable-autoupgrade",
                  "--machine-type",
                  config.clusterMachineType(),
                  "--scopes",
                  "cloud-platform,storage-rw,default,sql-admin,"
                      + "https://www.googleapis.com/auth/ndev.clouddns.readwrite"));
          if (config.clusterKubernetesVersion() != null) {
            createClusterArgs.add("--cluster-version");
            createClusterArgs.add(config.clusterKubernetesVersion());
          }
          createCluster.setArgs(Collections.unmodifiableList(createClusterArgs));

          GcloudTask loginToCluster =
              project.getTasks().create("gcloudLoginToCluster", GcloudTask.class);
          loginToCluster.setArgs(
              ImmutableList.of(
                  "container",
                  "clusters",
                  "get-credentials",
                  config.clusterName(),
                  "--zone",
                  config.clusterZone()));

          project.getTasks().create("createBuildCacheBucket", CreateBuildCacheBucket.class);
        });

    addGenerateCloudBuildTask(project);
  }

  private void addGenerateCloudBuildTask(Project rootProject) {
    Task generateCloudBuild = rootProject.getTasks().create("gcloudGenerateCloudBuild");
    String builderImage = "gcr.io/$PROJECT_ID/java-cloud-builder:latest";
    String kubeConfigEnv = "KUBECONFIG=/workspace/kubeconfig";
    generateCloudBuild.doLast(
        t -> {
          ImmutableGcloudExtension config =
              rootProject.getExtensions().getByType(GcloudExtension.class);
          List<CloudBuildStep> serverSteps =
              rootProject
                  .getAllprojects()
                  .stream()
                  .filter(proj -> proj.getPlugins().hasPlugin(CurioServerPlugin.class))
                  .flatMap(
                      proj -> {
                        String archivesBaseName =
                            proj.getConvention()
                                .getPlugin(BasePluginConvention.class)
                                .getArchivesBaseName();
                        String distId = "build-" + archivesBaseName + "-dist";
                        String imageId = "build-" + archivesBaseName + "-image";
                        String pushId = "push-" + archivesBaseName + "-image";
                        String imageTag =
                            config.containerRegistry()
                                + "/$PROJECT_ID/"
                                + archivesBaseName
                                + ":latest";
                        return Stream.of(
                            ImmutableCloudBuildStep.builder()
                                .id(distId)
                                .addWaitFor("refresh-build-image")
                                .name(builderImage)
                                .entrypoint("./gradlew")
                                .args(
                                    ImmutableList.of(
                                        proj.getTasks().getByName("dockerDistTar").getPath()))
                                .build(),
                            ImmutableCloudBuildStep.builder()
                                .id(imageId)
                                .addWaitFor(distId)
                                .name("gcr.io/cloud-builders/docker")
                                .args(
                                    ImmutableList.of(
                                        "build",
                                        "--tag=" + imageTag,
                                        Paths.get(rootProject.getProjectDir().getAbsolutePath())
                                            .relativize(
                                                Paths.get(
                                                    new File(proj.getBuildDir(), "docker")
                                                        .getAbsolutePath()))
                                            .toString()))
                                .build(),
                            ImmutableCloudBuildStep.builder()
                                .id(pushId)
                                .addWaitFor(imageId)
                                .name("gcr.io/cloud-builders/docker")
                                .args(ImmutableList.of("push", imageTag))
                                .build(),
                            ImmutableCloudBuildStep.builder()
                                .id("deploy-" + archivesBaseName)
                                .addWaitFor(pushId, "login-to-cluster")
                                .name(builderImage)
                                .entrypoint("./gradlew")
                                .args(
                                    ImmutableList.of(
                                        proj.getTasks().getByName("deployAlpha").getPath()))
                                .addEnv(kubeConfigEnv)
                                .build());
                      })
                  .collect(Collectors.toList());
          List<CloudBuildStep> steps = new ArrayList<>();
          steps.add(
              ImmutableCloudBuildStep.builder()
                  .id("refresh-build-image")
                  .addWaitFor("-")
                  .name("gcr.io/cloud-builders/docker")
                  .args(
                      ImmutableList.of(
                          "build",
                          "--tag=" + builderImage,
                          "--file=./tools/build-images/java-cloud-builder/Dockerfile",
                          "."))
                  .build());
          steps.add(
              ImmutableCloudBuildStep.builder()
                  .id("login-to-cluster")
                  .addWaitFor("-")
                  .name("gcr.io/cloud-builders/gcloud")
                  .entrypoint("bash")
                  .args(
                      ImmutableList.of(
                          "-c",
                          "gsutil cp gs://"
                              + config.clusterProject()
                              + "-kubepush-key/kubepush.json . \n"
                              + "gcloud auth activate-service-account --key-file ./kubepush.json \n"
                              + "gcloud container clusters get-credentials "
                              + config.clusterProject()
                              + " --zone "
                              + config.clusterZone()))
                  .addEnv("CLOUDSDK_CONTAINER_USE_CLIENT_CERTIFICATE=True", kubeConfigEnv)
                  .build());
          steps.addAll(serverSteps);
          HashMap<String, Object> cloudBuildConfig = new LinkedHashMap<>();
          cloudBuildConfig.put("steps", steps);
          cloudBuildConfig.put("images", ImmutableList.of(builderImage));
          try {
            OBJECT_MAPPER.writeValue(rootProject.file("cloudbuild.yaml"), cloudBuildConfig);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }

  @Immutable
  @Style(
    visibility = ImplementationVisibility.PACKAGE,
    builderVisibility = BuilderVisibility.PACKAGE,
    defaultAsDefault = true
  )
  @JsonSerialize(as = ImmutableCloudBuildStep.class)
  interface CloudBuildStep {

    String id();

    default List<String> waitFor() {
      return ImmutableList.of();
    }

    String name();

    @Nullable
    default String entrypoint() {
      return null;
    }

    List<String> args();

    default List<String> env() {
      return ImmutableList.of("CI=true");
    };
  }
}
