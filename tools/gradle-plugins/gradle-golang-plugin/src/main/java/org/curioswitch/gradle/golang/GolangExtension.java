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
package org.curioswitch.gradle.golang;

import java.nio.file.Path;
import org.curioswitch.gradle.helpers.immutables.ExtensionStyle;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.immutables.value.Value.Modifiable;

@Modifiable
@ExtensionStyle
public interface GolangExtension extends HasPublicType {

  String NAME = "golang";

  static ModifiableGolangExtension createAndAdd(Project project) {
    var objects = project.getObjects();
    var extension =
        project
            .getExtensions()
            .create(NAME, ModifiableGolangExtension.class)
            .setExecutableName(objects.property(String.class))
            .setGoOses(objects.listProperty(String.class).empty())
            .setGoArchs(objects.listProperty(String.class).empty())
            .setConda(objects.property(String.class).value("miniconda-build"))
            .setJib(Jib.create(objects));
    return extension;
  }

  Property<String> getExecutableName();

  ListProperty<String> getGoOses();

  ListProperty<String> getGoArchs();

  Property<String> getConda();

  @Modifiable
  @ExtensionStyle
  interface Jib {
    static Jib create(ObjectFactory objects) {
      return objects
          .newInstance(ModifiableJib.class)
          .setBaseImage(objects.property(String.class))
          .setTargetImage(objects.property(String.class))
          .setAdditionalTags(objects.listProperty(String.class).empty())
          .setUser(objects.property(String.class))
          .setVolumes(objects.listProperty(String.class).empty())
          .setPorts(objects.listProperty(Integer.class).empty())
          .setArgs(objects.listProperty(String.class).empty())
          .setWorkingDir(objects.property(String.class))
          .setCredentialHelper(objects.property(Path.class))
          .setEnvironmentVariables(objects.mapProperty(String.class, String.class).empty());
    }

    Property<String> getBaseImage();

    Property<String> getTargetImage();

    ListProperty<String> getAdditionalTags();

    Property<String> getUser();

    ListProperty<String> getVolumes();

    ListProperty<Integer> getPorts();

    ListProperty<String> getArgs();

    Property<String> getWorkingDir();

    Property<Path> getCredentialHelper();

    MapProperty<String, String> getEnvironmentVariables();
  }

  Jib getJib();

  default GolangExtension jib(Action<? super Jib> action) {
    action.execute(getJib());
    return this;
  }

  @Override
  default TypeOf<?> getPublicType() {
    return TypeOf.typeOf(GolangExtension.class);
  }
}
