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

package org.curioswitch.gradle.protobuf;

import java.io.File;
import org.curioswitch.gradle.helpers.immutables.ExtensionStyle;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
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
            .setSources(
                project.container(
                    SourceDirectorySet.class,
                    name -> {
                      var directorySet =
                          objects.sourceDirectorySet(name, name).srcDir("src/" + name + "/proto");
                      directorySet.include("**/*.proto");
                      return directorySet;
                    }))
            .setOutputBaseDir(objects.property(File.class))
            .setProtoc(Executable.create(objects))
            .setDescriptorSetOptions(DescriptorSetOptions.create(objects));
    extension.setLanguages(
        project.container(
            LanguageSettings.class, name -> LanguageSettings.create(name, project.getObjects())));

    extension.getOutputBaseDir().set(project.file("build/generated/proto"));

    return extension;
  }

  default ProtobufExtension sources(
      Action<? super NamedDomainObjectContainer<SourceDirectorySet>> action) {
    action.execute(getSources());
    return this;
  }

  NamedDomainObjectContainer<SourceDirectorySet> getSources();

  Property<File> getOutputBaseDir();

  default ProtobufExtension protoc(Action<? super Executable> action) {
    action.execute(getProtoc());
    return this;
  }

  Executable getProtoc();

  default ProtobufExtension languages(
      Action<? super NamedDomainObjectContainer<LanguageSettings>> action) {
    action.execute(getLanguages());
    return this;
  }

  NamedDomainObjectContainer<LanguageSettings> getLanguages();

  DescriptorSetOptions getDescriptorSetOptions();

  @Modifiable
  @ExtensionStyle
  interface Executable extends HasPublicType {
    static Executable create(ObjectFactory objects) {
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

    static ModifiableLanguageSettings create(String name, ObjectFactory objects) {
      ModifiableLanguageSettings settings =
          objects
              .newInstance(ModifiableLanguageSettings.class)
              .setName(name)
              .setOutputDir(objects.property(File.class))
              .setOptions(objects.listProperty(String.class).empty())
              .setPlugin(Executable.create(objects));

      return settings;
    }

    Property<File> getOutputDir();

    default LanguageSettings option(String option) {
      getOptions().add(option);
      return this;
    }

    default LanguageSettings option(Provider<String> option) {
      getOptions().add(option);
      return this;
    }

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

  @Modifiable
  @ExtensionStyle
  interface DescriptorSetOptions {
    static DescriptorSetOptions create(ObjectFactory objects) {
      DescriptorSetOptions options =
          objects
              .newInstance(ModifiableDescriptorSetOptions.class)
              .setEnabled(objects.property(Boolean.class))
              .setPath(objects.property(File.class))
              .setIncludeSourceInfo(objects.property(Boolean.class))
              .setIncludeImports(objects.property(Boolean.class));

      options.getEnabled().set(false);
      options.getIncludeSourceInfo().set(false);
      options.getIncludeImports().set(false);

      return options;
    }

    Property<Boolean> getEnabled();

    Property<File> getPath();

    Property<Boolean> getIncludeSourceInfo();

    Property<Boolean> getIncludeImports();
  }

  @Override
  default TypeOf<?> getPublicType() {
    return TypeOf.typeOf(ProtobufExtension.class);
  }
}
