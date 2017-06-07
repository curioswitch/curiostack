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

package org.curioswitch.gradle.plugins.gcloud.tasks;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.curioswitch.gradle.plugins.gcloud.DatabaseExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableDatabaseExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class DeployDevDbPodTask extends DefaultTask {

  @TaskAction
  public void exec() {
    ImmutableDatabaseExtension config =
        getProject().getExtensions().getByType(DatabaseExtension.class);

    PersistentVolumeClaim volumeClaim =
        new PersistentVolumeClaimBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(config.devDbPodName() + "-pvc")
                    .withNamespace(config.devDbPodNamespace())
                    .build())
            .withSpec(
                new PersistentVolumeClaimSpecBuilder()
                    .withAccessModes("ReadWriteOnce")
                    .withResources(
                        new ResourceRequirementsBuilder()
                            .withRequests(ImmutableMap.of("storage", new Quantity("5Gi")))
                            .build())
                    .build())
            .build();

    Pod pod =
        new PodBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(config.devDbPodName())
                    .withLabels(ImmutableMap.of("name", config.devDbPodName()))
                    .withNamespace(config.devDbPodNamespace())
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
                                            new Quantity("0.1"),
                                            "memory",
                                            new Quantity("512Mi")))
                                    .build())
                            .withImage(config.devDockerImageTag())
                            .withName(config.devDbPodName())
                            .withImagePullPolicy("Always")
                            .withPorts(
                                new ContainerPortBuilder()
                                    .withContainerPort(3306)
                                    .withName("mysql")
                                    .build())
                            .withVolumeMounts(
                                new VolumeMountBuilder()
                                    .withName(config.devDbPodName() + "-data")
                                    .withMountPath("/var/lib/mysql")
                                    .build())
                            .withArgs("--ignore-db-dir=lost+found")
                            .build())
                    .withVolumes(
                        new VolumeBuilder()
                            .withName(config.devDbPodName() + "-data")
                            .withPersistentVolumeClaim(
                                new PersistentVolumeClaimVolumeSourceBuilder()
                                    .withClaimName(volumeClaim.getMetadata().getName())
                                    .build())
                            .build())
                    .build())
            .build();

    Service service =
        new ServiceBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(config.devDbPodName())
                    .withNamespace(config.devDbPodNamespace())
                    .build())
            .withSpec(
                new ServiceSpecBuilder()
                    .withPorts(
                        new ServicePortBuilder()
                            .withPort(3306)
                            .withTargetPort(new IntOrString(3306))
                            .build())
                    .withSelector(ImmutableMap.of("name", config.devDbPodName()))
                    .withType("LoadBalancer")
                    .withLoadBalancerSourceRanges(config.devDbIpRestrictions())
                    .build())
            .build();

    KubernetesClient client = new DefaultKubernetesClient();
    try {
      client.resource(volumeClaim).createOrReplace();
    } catch (Exception e) {
      // TODO(choko): Find a better way to idempotently setup.
      // Ignore
    }
    try {
      client.resourceList(pod).createOrReplace();
    } catch (Exception e) {
      // TODO(choko): Find a better way to idempotently setup.
      // Ignore
    }
    client.resource(service).createOrReplace();
  }
}
