/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

import org.curioswitch.gradle.helpers.immutables.ExtensionStyle;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.immutables.value.Value.Modifiable;

@Modifiable
@ExtensionStyle
public interface GcloudExtension extends HasPublicType {

  String NAME = "gcloud";

  static GcloudExtension create(Project project) {
    var objects = project.getObjects();
    var extension =
        project
            .getExtensions()
            .create(NAME, ModifiableGcloudExtension.class)
            .setCloudRegion(objects.property(String.class))
            .setClusterBaseName(objects.property(String.class));
    extension.setClusterRegion(
        objects.property(String.class).convention(extension.getCloudRegion()));
    extension.setClusterName(
        objects
            .property(String.class)
            .convention(clusterResourceName(extension.getClusterBaseName(), "cluster")));
    extension.setClusterProject(
        objects.property(String.class).convention(extension.getClusterName()));
    extension.setContainerRegistry(
        objects
            .property(String.class)
            .convention(
                extension
                    .getCloudRegion()
                    .map(
                        value -> {
                          if (value.startsWith("asia")) {
                            return "asia.gcr.io";
                          } else if (value.startsWith("europe")) {
                            return "eu.gcr.io";
                          } else {
                            return "gcr.io";
                          }
                        })));
    extension.setBuildCacheStorageBucket(
        objects
            .property(String.class)
            .convention(clusterResourceName(extension.getClusterBaseName(), "gradle-build-cache")));
    return extension;
  }

  Property<String> getCloudRegion();

  Property<String> getClusterBaseName();

  Property<String> getClusterRegion();

  Property<String> getClusterName();

  Property<String> getClusterProject();

  Property<String> getContainerRegistry();

  Property<String> getBuildCacheStorageBucket();

  private static Provider<String> clusterResourceName(
      Provider<String> clusterBaseName, String resource) {
    return clusterBaseName.map(value -> value + '-' + resource);
  }

  @Override
  default TypeOf<?> getPublicType() {
    return TypeOf.typeOf(GcloudExtension.class);
  }
}
