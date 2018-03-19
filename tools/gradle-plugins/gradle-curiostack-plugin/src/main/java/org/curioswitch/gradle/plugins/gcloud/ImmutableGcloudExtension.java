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

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Modifiable;
import org.immutables.value.Value.Style;

@Modifiable
@Style(create = "new", typeModifiable = "*", defaultAsDefault = true, typeAbstract = "Immutable*")
public interface ImmutableGcloudExtension {

  String NAME = "gcloud";

  @Value.Parameter
  Project gradleProject();

  default String cloudRegion() {
    throw new IllegalArgumentException("cloudRegion must be specified.");
  }

  default String clusterZone() {
    return cloudRegion() + "-a";
  }

  default List<String> clusterAdditionalZones() {
    return Collections.singletonList(cloudRegion() + "-c");
  }

  default int clusterNumNodesPerZone() {
    return 1;
  }

  @Nullable
  default String clusterKubernetesVersion() {
    return null;
  }

  default String clusterMachineType() {
    return "g1-small";
  }

  default String clusterBaseName() {
    throw new IllegalArgumentException("clusterBaseName must be specified.");
  }

  default String clusterName() {
    return clusterResourceName("cluster");
  }

  default String clusterProject() {
    return clusterResourceName("cluster");
  }

  default String containerRegistry() {
    if (cloudRegion().startsWith("asia")) {
      return "asia.gcr.io";
    } else if (cloudRegion().startsWith("europe")) {
      return "eu.gcr.io";
    } else {
      return "gcr.io";
    }
  }

  default String sourceRepository() {
    return clusterResourceName("source");
  }

  default String buildCacheStorageBucket() {
    return clusterResourceName("gradle-build-cache");
  }

  default boolean download() {
    return true;
  }

  default File workDir() {
    return Paths.get(gradleProject().getProjectDir().getAbsolutePath(), ".gradle", "gcloud")
        .toFile();
  }

  default String version() {
    return "193.0.0";
  }

  default String distBaseUrl() {
    return "https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/";
  }

  @Derived
  default PlatformConfig platformConfig() {
    return PlatformConfig.fromExtension(this);
  }

  default String clusterResourceName(String resource) {
    return clusterBaseName() + "-" + resource;
  }
}
