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

package org.curioswitch.gradle.plugins.curioserver;

import org.curioswitch.gradle.helpers.immutables.ExtensionStyle;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.immutables.value.Value.Modifiable;

@Modifiable
@ExtensionStyle
public interface ServerExtension extends HasPublicType {

  String NAME = "server";

  static ServerExtension createAndAdd(Project project) {
    var objects = project.getObjects();

    ModifiableServerExtension extension =
        project
            .getExtensions()
            .create(NAME, ModifiableServerExtension.class)
            .setImagePrefix(objects.property(String.class))
            .setImageTag(objects.property(String.class).value("latest"))
            .setBaseName(objects.property(String.class))
            .setBaseImage(objects.property(String.class).value("curiostack/java-cloud-runner:14"));
    extension.setDeployments(
        project.container(
            AutoDeployment.class, name -> AutoDeployment.create(name, objects, extension)));

    return extension;
  }

  Property<String> getImagePrefix();

  Property<String> getImageTag();

  Property<String> getBaseName();

  Property<String> getBaseImage();

  NamedDomainObjectContainer<AutoDeployment> getDeployments();

  @Modifiable
  @ExtensionStyle
  interface AutoDeployment extends Named, HasPublicType {

    static ModifiableAutoDeployment create(
        String name, ObjectFactory objects, ServerExtension serverExtension) {
      var deployment =
          objects
              .newInstance(ModifiableAutoDeployment.class)
              .setName(name)
              .setNamespace(objects.property(String.class).value("default"))
              .setDeployment(objects.property(String.class))
              .setAutoDeploy(objects.property(Boolean.class).value(false));

      deployment
          .getDeployment()
          .set(serverExtension.getBaseName().map(baseName -> baseName + '-' + name));

      return deployment;
    }

    Property<String> getNamespace();

    Property<String> getDeployment();

    Property<Boolean> getAutoDeploy();

    @Override
    default TypeOf<?> getPublicType() {
      return TypeOf.typeOf(AutoDeployment.class);
    }
  }

  @Override
  default TypeOf<?> getPublicType() {
    return TypeOf.typeOf(ServerExtension.class);
  }
}
