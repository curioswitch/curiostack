/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.tooldownloader;

import org.curioswitch.gradle.helpers.immutables.ExtensionStyle;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.immutables.value.Value.Modifiable;

@Modifiable
@ExtensionStyle
public interface ToolDownloaderExtension extends Named, HasPublicType {

  static ModifiableToolDownloaderExtension create(String name, Project project) {
    var objects = project.getObjects();

    var extension =
        objects
            .newInstance(ModifiableToolDownloaderExtension.class)
            .setName(name)
            .setArtifact(objects.property(String.class))
            .setVersion(objects.property(String.class))
            .setBaseUrl(objects.property(String.class))
            .setArtifactPattern(objects.property(String.class))
            .setPathSubDirs(objects.listProperty(String.class))
            .setAdditionalCachedDirs(objects.listProperty(String.class));

    extension.getArtifact().set(name);

    var osClassifiers =
        objects
            .newInstance(ModifiableOsValues.class)
            .setLinux(objects.property(String.class))
            .setMac(objects.property(String.class))
            .setWindows(objects.property(String.class));
    osClassifiers.getLinux().set("linux-amd64");
    osClassifiers.getMac().set("darwin-amd64");
    osClassifiers.getWindows().set("windows-amd64");
    extension.setOsClassifiers(osClassifiers);

    var osExtensions =
        objects
            .newInstance(ModifiableOsValues.class)
            .setLinux(objects.property(String.class))
            .setMac(objects.property(String.class))
            .setWindows(objects.property(String.class));
    osExtensions.getLinux().set("tar.gz");
    osExtensions.getMac().set("tar.gz");
    osExtensions.getWindows().set("zip");
    extension.setOsExtensions(osExtensions);

    return extension;
  }

  /** The artifact to download. */
  Property<String> getArtifact();

  /** The version of the tool to download. */
  Property<String> getVersion();

  /** The base URL to download tool packages from. Must follow Ivy repository conventions. */
  Property<String> getBaseUrl();

  /**
   * The Ivy artifact pattern used to locate tool packages, e.g.,
   * [artifact]-v[revision]-[classifier].[ext].
   */
  Property<String> getArtifactPattern();

  /** Subdirectories of the extract archive to add to PATH when executing tasks. */
  ListProperty<String> getPathSubDirs();

  /** Additional subdirectories of the curiostack path to cache along with the tool. */
  ListProperty<String> getAdditionalCachedDirs();

  /**
   * OS-specific values to use as the classifier when resolving the artifact pattern. Defaults to
   * linux-amd64, darwin-amd64, windows-amd64 on Linux, Mac OS X, and Windows respectively.
   */
  OsValues getOsClassifiers();

  /**
   * OS-specific values to use as the extension when resolving the artifact pattern. Defaults to
   * tar.gz on Linux and Mac OS X and zip on Windows.
   */
  OsValues getOsExtensions();

  @Modifiable
  @ExtensionStyle
  interface OsValues {

    default String getValue(OperatingSystem os) {
      switch (os) {
        case LINUX:
          return getLinux().get();
        case MAC_OSX:
          return getMac().get();
        case WINDOWS:
          return getWindows().get();
      }
      throw new IllegalArgumentException("Unsupported os: " + os);
    }

    /** A value to use on Linux. */
    Property<String> getLinux();

    /** A value to use on mac. */
    Property<String> getMac();

    /** A value to use on windows. */
    Property<String> getWindows();
  }

  @Override
  default TypeOf<?> getPublicType() {
    return TypeOf.typeOf(ToolDownloaderExtension.class);
  }
}
