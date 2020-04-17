/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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
package org.curioswitch.gradle.plugins.ci;

import org.curioswitch.gradle.helpers.immutables.ExtensionStyle;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.immutables.value.Value.Modifiable;

@Modifiable
@ExtensionStyle
public interface CiExtension extends HasPublicType {

  String NAME = "ci";

  static CiExtension createAndAdd(Project project) {
    var objects = project.getObjects();

    return project
        .getExtensions()
        .create(NAME, ModifiableCiExtension.class)
        .setReleaseTagPrefixes(
            project.container(
                ReleaseTagSettings.class, name -> ReleaseTagSettings.create(name, objects)))
        .setCodeCoverageExcludedProjects(project.getObjects().listProperty(String.class).empty());
  }

  default CiExtension releaseTagPrefixes(
      Action<? super NamedDomainObjectContainer<ReleaseTagSettings>> action) {
    action.execute(getReleaseTagPrefixes());
    return this;
  }

  NamedDomainObjectContainer<ReleaseTagSettings> getReleaseTagPrefixes();

  ListProperty<String> getCodeCoverageExcludedProjects();

  @Modifiable
  @ExtensionStyle
  interface ReleaseTagSettings extends Named, HasPublicType {

    static ReleaseTagSettings create(String name, ObjectFactory objects) {
      ReleaseTagSettings settings =
          objects
              .newInstance(ModifiableReleaseTagSettings.class)
              .setName(name)
              .setProjects(objects.listProperty(String.class).empty());
      return settings;
    }

    ListProperty<String> getProjects();

    default ReleaseTagSettings project(String project) {
      getProjects().add(project);
      return this;
    }

    default ReleaseTagSettings project(Provider<String> project) {
      getProjects().add(project);
      return this;
    }

    @Override
    default TypeOf<?> getPublicType() {
      return TypeOf.typeOf(ReleaseTagSettings.class);
    }
  }

  @Override
  default TypeOf<?> getPublicType() {
    return TypeOf.typeOf(CiExtension.class);
  }
}
