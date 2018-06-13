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

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import org.curioswitch.gradle.plugins.curioserver.DeploymentExtension;
import org.curioswitch.gradle.plugins.curioserver.ImmutableDeploymentExtension;
import org.curioswitch.gradle.plugins.curioserver.ImmutableDeploymentExtension.ImmutableDeploymentConfiguration;
import org.gradle.api.DefaultTask;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.tasks.TaskAction;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;

public class DeployConfigMapTask extends DefaultTask {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private String type;

  public DeployConfigMapTask setType(String type) {
    this.type = type;
    return this;
  }

  @TaskAction
  public void exec() {
    ImmutableDeploymentExtension config =
        getProject().getExtensions().getByType(DeploymentExtension.class);
    final ImmutableDeploymentConfiguration deploymentConfig = config.getTypes().getByName(type);

    Map<String, RpcAcl> aclMap =
        deploymentConfig
            .rpcAcls()
            .entrySet()
            .stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(), ImmutableRpcAcl.builder().rate(entry.getValue()).build()))
            .collect(toImmutableMap(Entry::getKey, Entry::getValue));
    final String serializedAcls;
    try {
      serializedAcls = OBJECT_MAPPER.writeValueAsString(aclMap);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException("Could not serialize acls.", e);
    }

    ConfigMap configMap =
        new ConfigMapBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName("rpcacls")
                    .withNamespace(deploymentConfig.namespace())
                    .build())
            .withData(ImmutableMap.of("rpcacls.json", serializedAcls))
            .build();
    KubernetesClient client = new DefaultKubernetesClient();
    client.resource(configMap).createOrReplace();
  }

  @Immutable
  @Style(
      visibility = ImplementationVisibility.PACKAGE,
      builderVisibility = BuilderVisibility.PACKAGE,
      defaultAsDefault = true)
  @JsonSerialize(as = ImmutableRpcAcl.class)
  interface RpcAcl {
    String rate();
  }
}
