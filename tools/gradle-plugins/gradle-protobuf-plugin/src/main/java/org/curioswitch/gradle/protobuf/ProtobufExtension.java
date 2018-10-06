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

package org.curioswitch.gradle.protobuf;

import java.io.File;
import org.curioswitch.gradle.helpers.immutables.ExtensionStyle;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.immutables.value.Value.Modifiable;

@Modifiable
@ExtensionStyle
public interface ProtobufExtension extends HasPublicType {

  String NAME = "protobuf";

  static ModifiableProtobufExtension createAndAdd(Project project) {
    ObjectFactory objects = project.getObjects();

    ModifiableProtobufExtension extension =
        project
            .getExtensions()
            .create(NAME, ModifiableProtobufExtension.class)
            .setOutputBaseDir(objects.property(File.class))
            .setProtoc(Executable.create(objects));
    extension.setLanguages(
        project.container(
            ModifiableLanguageSettings.class,
            name -> LanguageSettings.create(name, project.getObjects(), extension)));

    extension.getOutputBaseDir().set(project.file("build/generated/proto"));

    return extension;
  }

  Property<File> getOutputBaseDir();

  default ProtobufExtension protoc(Action<? super Executable> action) {
    action.execute(getProtoc());
    return this;
  }

  Executable getProtoc();

  NamedDomainObjectContainer<? extends LanguageSettings> getLanguages();

  @Modifiable
  @ExtensionStyle
  interface Executable extends HasPublicType {
    static ModifiableExecutable create(ObjectFactory objects) {
      return objects
          .newInstance(ModifiableExecutable.class)
          .setPath(objects.property(File.class))
          .setArtifact(objects.property(String.class));
    }

    default boolean isPresent() {
      return getPath().isPresent() || getArtifact().isPresent();
    }

    Property<File> getPath();

    Property<String> getArtifact();

    @Override
    default TypeOf<?> getPublicType() {
      return TypeOf.typeOf(Executable.class);
    }
  }

  @Modifiable
  @ExtensionStyle
  interface LanguageSettings extends Named, HasPublicType {

    static ModifiableLanguageSettings create(
        String name, ObjectFactory objects, ProtobufExtension protobuf) {
      ModifiableLanguageSettings settings =
          objects
              .newInstance(ModifiableLanguageSettings.class)
              .setName(name)
              .setOutputDir(objects.property(File.class))
              .setOptions(objects.listProperty(String.class))
              .setPlugin(Executable.create(objects));

      settings
          .getOutputDir()
          .set(protobuf.getOutputBaseDir().map(baseDir -> new File(baseDir, name)));

      return settings;
    }

    Property<File> getOutputDir();

    ListProperty<String> getOptions();

    default LanguageSettings plugin(Action<? super Executable> action) {
      action.execute(getPlugin());
      return this;
    }

    Executable getPlugin();

    @Override
    default TypeOf<?> getPublicType() {
      return TypeOf.typeOf(LanguageSettings.class);
    }
  }

  @Override
  default TypeOf<?> getPublicType() {
    return TypeOf.typeOf(ProtobufExtension.class);
  }
}
