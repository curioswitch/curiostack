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
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStrategyBuilder;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValueBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressTLSBuilder;
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
                    ::iterator)
            .addAll(
                deploymentConfig
                        .secretEnvVars()
                        .entrySet()
                        .stream()
                        .map(
                            (entry) ->
                                new EnvVar(
                                    entry.getKey(),
                                    null,
                                    new EnvVarSourceBuilder()
                                        .withSecretKeyRef(
                                            new SecretKeySelectorBuilder()
                                                .withName(entry.getValue().get(0))
                                                .withKey(entry.getValue().get(1))
                                                .build())
                                        .build()))
                    ::iterator);
    if (!deploymentConfig.envVars().containsKey("JAVA_OPTS")) {
      envVars.add(
          new EnvVar(
              "JAVA_OPTS",
              "-Xms"
                  + deploymentConfig.jvmHeapMb()
                  + "m -Xmx"
                  + deploymentConfig.jvmHeapMb()
                  + "m -Dconfig.resource=application-"
                  + type
                  + ".conf",
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
                            .withType("RollingUpdate")
                            .withRollingUpdate(
                                new RollingUpdateDeploymentBuilder()
                                    .withNewMaxUnavailable(0)
                                    .build())
                            .build())
                    .withSelector(
                        new LabelSelectorBuilder()
                            .withMatchLabels(
                                ImmutableMap.of("name", deploymentConfig.deploymentName()))
                            .build())
                    .withTemplate(
                        new PodTemplateSpecBuilder()
                            .withMetadata(
                                new ObjectMetaBuilder()
                                    .withLabels(
                                        ImmutableMap.of(
                                            "name", deploymentConfig.deploymentName(),
                                            "revision",
                                                System.getenv()
                                                    .getOrDefault("REVISION_ID", "none")))
                                    .withAnnotations(ImmutableMap.<String, String>builder()
                                        .put("prometheus.io/scrape", "true")
                                        .put("prometheus.io/scheme", "https")
                                        .put("prometheus.io/path", "/internal/metrics")
                                        .put("prometheus.io/port", String.valueOf(deploymentConfig.containerPort()))
                                        .build())
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
                                            .withVolumeMounts(
                                                new VolumeMountBuilder()
                                                    .withName("tls")
                                                    .withMountPath("/etc/tls")
                                                    .withReadOnly(true)
                                                    .build(),
                                                new VolumeMountBuilder()
                                                    .withName("rpcacls")
                                                    .withMountPath("/etc/rpcacls")
                                                    .withReadOnly(true)
                                                    .build())
                                            .build())
                                    .withVolumes(
                                        new VolumeBuilder()
                                            .withName("tls")
                                            .withSecret(
                                                new SecretVolumeSourceBuilder()
                                                    .withSecretName("server-tls")
                                                    .build())
                                            .build(),
                                        new VolumeBuilder()
                                            .withName("rpcacls")
                                            .withConfigMap(
                                                new ConfigMapVolumeSourceBuilder()
                                                    .withName("rpcacls")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    Service service =
        new ServiceBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(deploymentConfig.deploymentName())
                    .withNamespace(deploymentConfig.namespace())
                    .withAnnotations(ImmutableMap.<String, String>builder()
                        .put("service.alpha.kubernetes.io/app-protocols", "{\"https\":\"HTTPS\"}")
                        .put("prometheus.io/scrape", "true")
                        .put("prometheus.io/scheme", "https")
                        .put("prometheus.io/path", "/internal/metrics")
                        .put("prometheus.io/port", String.valueOf(deploymentConfig.containerPort()))
                        .put("prometheus.io/probe", "true")
                        .build())
                    .build())
            .withSpec(
                new ServiceSpecBuilder()
                    .withPorts(
                        new ServicePortBuilder()
                            .withPort(deploymentConfig.containerPort())
                            .withName("https")
                            .build())
                    .withSelector(ImmutableMap.of("name", deploymentConfig.deploymentName()))
                    .withType(deploymentConfig.externalHost() != null ? "NodePort" : "ClusterIP")
                    .build())
            .build();

    KubernetesClient client = new DefaultKubernetesClient();

    client.resource(deployment).createOrReplace();
    try {
      if (client.resource(service).fromServer().get() == null) {
        client.resource(service).createOrReplace();
      }
    } catch (ClassCastException e) {
      // TODO(choko): Kubernetes client is throwing this on get() for some reason. Seems like a bug
      // but it works to skip existing services to just live with it for now, but try to fix it.
    }

    if (deploymentConfig.externalHost() != null) {
      Ingress ingress =
          new IngressBuilder()
              .withMetadata(
                  new ObjectMetaBuilder()
                      .withNamespace(deploymentConfig.namespace())
                      .withName(deploymentConfig.deploymentName())
                      .withAnnotations(
                          ImmutableMap.of(
                              "kubernetes.io/tls-acme", "true",
                              "kubernetes.io/ingress.class", "gce"))
                      .build())
              .withSpec(
                  new IngressSpecBuilder()
                      .withTls(
                          new IngressTLSBuilder()
                              .withSecretName(deploymentConfig.deploymentName() + "-tls")
                              .withHosts(deploymentConfig.externalHost())
                              .build())
                      .withRules(
                          new IngressRuleBuilder()
                              .withHost(deploymentConfig.externalHost())
                              .withHttp(
                                  new HTTPIngressRuleValueBuilder()
                                      .withPaths(
                                          new HTTPIngressPathBuilder()
                                              .withPath("/*")
                                              .withBackend(
                                                  new IngressBackendBuilder()
                                                      .withServiceName(
                                                          deploymentConfig.deploymentName())
                                                      .withServicePort(
                                                          new IntOrString(
                                                              deploymentConfig.containerPort()))
                                                      .build())
                                              .build())
                                      .build())
                              .build())
                      .build())
              .build();

      client.resource(ingress).createOrReplace();
    }
  }
}
