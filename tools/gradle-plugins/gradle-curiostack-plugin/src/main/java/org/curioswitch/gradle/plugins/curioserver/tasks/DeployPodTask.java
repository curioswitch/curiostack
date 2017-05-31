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

package org.curioswitch.gradle.plugins.curioserver.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStrategyBuilder;
import io.fabric8.kubernetes.api.model.extensions.RollingUpdateDeploymentBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.curioswitch.gradle.plugins.curioserver.DeploymentExtension;
import org.curioswitch.gradle.plugins.curioserver.ImmutableDeploymentExtension;
import org.curioswitch.gradle.plugins.curioserver.ImmutableDeploymentExtension.ImmutableDeploymentConfiguration;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class DeployPodTask extends DefaultTask {

  private String type;

  public DeployPodTask setType(String type) {
    this.type = type;
    return this;
  }

  @TaskAction
  public void exec() {
    ImmutableDeploymentExtension config =
        getProject().getExtensions().getByType(DeploymentExtension.class);

    final ImmutableDeploymentConfiguration deploymentConfig = config.getTypes().getByName(type);

    ImmutableList.Builder<EnvVar> envVars =
        ImmutableList.<EnvVar>builder()
            .addAll(
                deploymentConfig
                        .envVars()
                        .entrySet()
                        .stream()
                        .map((entry) -> new EnvVar(entry.getKey(), entry.getValue(), null))
                    ::iterator);
    if (!deploymentConfig.envVars().containsKey("JAVA_OPTS")) {
      envVars.add(
          new EnvVar(
              "JAVA_OPTS",
              "-Xms" + deploymentConfig.jvmHeapMb()
                  + "m -Xmx" + deploymentConfig.jvmHeapMb() + "m -Dconfig.resource=application-"
                  + type + ".conf",
              null));
    }

    Deployment deployment =
        new DeploymentBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withNamespace(deploymentConfig.namespace())
                    .withName(deploymentConfig.deploymentName())
                    .build())
            .withSpec(
                new DeploymentSpecBuilder()
                    .withReplicas(deploymentConfig.replicas())
                    .withStrategy(
                        new DeploymentStrategyBuilder()
                            .withRollingUpdate(
                                new RollingUpdateDeploymentBuilder()
                                    .withNewMaxUnavailable(0)
                                    .build())
                            .build())
                    .withTemplate(
                        new PodTemplateSpecBuilder()
                            .withMetadata(
                                new ObjectMetaBuilder()
                                    .addToLabels("name", deploymentConfig.deploymentName())
                                    .build())
                            .withSpec(
                                new PodSpecBuilder()
                                    .withContainers(
                                        new ContainerBuilder()
                                            .withResources(
                                                new ResourceRequirementsBuilder()
                                                    .withLimits(
                                                        ImmutableMap.of(
                                                            "cpu",
                                                                new Quantity(
                                                                    deploymentConfig.cpu()),
                                                            "memory",
                                                                new Quantity(
                                                                    deploymentConfig.memoryMb()
                                                                        + "Mi")))
                                                    .build())
                                            .withImage(deploymentConfig.image())
                                            .withName(deploymentConfig.deploymentName())
                                            .withEnv(envVars.build())
                                            .withImagePullPolicy("Always")
                                            .withReadinessProbe(
                                                new ProbeBuilder()
                                                    .withHttpGet(
                                                        new HTTPGetActionBuilder()
                                                            .withScheme("HTTPS")
                                                            .withPath(
                                                                deploymentConfig.healthCheckPath())
                                                            .withNewPort(
                                                                deploymentConfig.containerPort())
                                                            .build())
                                                    .withInitialDelaySeconds(30)
                                                    .withTimeoutSeconds(1)
                                                    .build())
                                            .withPorts(
                                                ImmutableList.of(
                                                    new ContainerPortBuilder()
                                                        .withContainerPort(
                                                            deploymentConfig.containerPort())
                                                        .withName("http")
                                                        .build()))
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    KubernetesClient client = new DefaultKubernetesClient();
    client.resource(deployment).createOrReplace();
  }
}
