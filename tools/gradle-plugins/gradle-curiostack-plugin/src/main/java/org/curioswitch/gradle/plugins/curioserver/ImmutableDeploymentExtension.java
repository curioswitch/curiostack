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

import com.google.common.collect.ImmutableMap;
import groovy.lang.Closure;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.immutables.value.Value;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Modifiable;
import org.immutables.value.Value.Style;

@Modifiable
@Style(create = "new", typeModifiable = "*", defaultAsDefault = true, typeAbstract = "Immutable*")
public interface ImmutableDeploymentExtension {

  String DEPLOYMENT_TYPES = "org.curioswitch.gradle.plugins.curioserver.deploymentTypes";

  String NAME = "deployment";

  @Value.Parameter
  Project gradleProject();

  default String imagePrefix() {
    throw new IllegalArgumentException("imagePrefix must be set");
  }

  @Lazy
  default String baseName() {
    return gradleProject()
        .getConvention()
        .getPlugin(BasePluginConvention.class)
        .getArchivesBaseName();
  }

  default boolean autoDeployAlpha() {
    return true;
  }

  @Modifiable
  @Style(create = "new", typeModifiable = "*", defaultAsDefault = true, typeAbstract = "Immutable*")
  interface ImmutableDeploymentConfiguration {

    @Value.Parameter
    String getName();

    @Nullable
    default String externalHost() {
      return null;
    }

    List<String> additionalServicePaths();

    default String namespace() {
      return "default";
    }

    default String deploymentName() {
      return "deployment";
    }

    default int replicas() {
      return 1;
    }

    default String cpu() {
      return "0.1";
    }

    default int memoryMb() {
      return 256;
    }

    default boolean request() {
      return false;
    }

    default String image() {
      return "deployment:latest";
    }

    default int jvmHeapMb() {
      return (int) (memoryMb() * 0.5);
    }

    default String healthCheckPath() {
      return "/internal/health";
    }

    default int containerPort() {
      return 8080;
    }

    default Map<String, String> envVars() {
      return ImmutableMap.of();
    }

    default Map<String, List<String>> secretEnvVars() {
      return ImmutableMap.of();
    }

    default Map<String, String> rpcAcls() {
      return ImmutableMap.of();
    }
  }

  default NamedDomainObjectContainer<DeploymentConfiguration> getTypes() {
    // We memoize the types ourselves into the project since a Modifiable will always call this
    // method even if we specify Derived or Lazy.
    ExtraPropertiesExtension props = gradleProject().getExtensions().getExtraProperties();
    if (props.has(DEPLOYMENT_TYPES)) {
      // We set the correctly typed value ourselves.
      @SuppressWarnings("unchecked")
      NamedDomainObjectContainer<DeploymentConfiguration> types =
          (NamedDomainObjectContainer<DeploymentConfiguration>) props.get(DEPLOYMENT_TYPES);
      return types;
    }

    NamedDomainObjectContainer<DeploymentConfiguration> types =
        gradleProject().container(DeploymentConfiguration.class);
    types
        .maybeCreate("alpha")
        .setNamespace(baseName() + "-dev")
        .setDeploymentName(baseName() + "-alpha")
        .setReplicas(1)
        .setCpu("0.1")
        .setMemoryMb(256)
        .setImage(imagePrefix() + baseName() + ":latest")
        .setRpcAcls(ImmutableMap.of("*", "*"));
    props.set(DEPLOYMENT_TYPES, types);
    return types;
  }

  default void types(Closure<?> configureClosure) {
    getTypes().configure(configureClosure);
  }
}
