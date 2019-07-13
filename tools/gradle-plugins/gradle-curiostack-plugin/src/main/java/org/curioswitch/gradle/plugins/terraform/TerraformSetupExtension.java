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

package org.curioswitch.gradle.plugins.terraform;

import static com.google.common.base.Preconditions.checkArgument;

import org.curioswitch.gradle.helpers.immutables.ExtensionStyle;
import org.gradle.api.Action;
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
public interface TerraformSetupExtension extends HasPublicType {

  String NAME = "terraformSetup";

  static TerraformSetupExtension create(Project project) {
    var objects = project.getObjects();

    var extension =
        project
            .getExtensions()
            .create(NAME, ModifiableTerraformSetupExtension.class)
            .setProviders(
                project.container(
                    CustomProvider.class, name -> CustomProvider.create(name, objects)));

    return extension;
  }

  default TerraformSetupExtension providers(
      Action<? super NamedDomainObjectContainer<CustomProvider>> action) {
    action.execute(getProviders());
    return this;
  }

  NamedDomainObjectContainer<CustomProvider> getProviders();

  @Modifiable
  @ExtensionStyle
  interface CustomProvider extends Named, HasPublicType {

    static CustomProvider create(String name, ObjectFactory objects) {
      checkArgument(
          name.startsWith("terraform-provider-"),
          "Custom Terraform provider names must start with terraform-provider-, but this one is "
              + name);
      ModifiableCustomProvider provider =
          objects
              .newInstance(ModifiableCustomProvider.class)
              .setName(name)
              .setGithubRepo(objects.property(String.class))
              .setVersion(objects.property(String.class));

      return provider;
    }

    /**
     * The GitHub repository to fetch provider source from to build. Should be in the format of
     * organization/repository, e.g., {@code curioswitch/terraform-provider-curio}.
     */
    Property<String> getGithubRepo();

    /**
     * The version of the provider to build. This must match a GitHub release for the provider,
     * e.g., {@code v1.2.3-foo}. Source code for the provider will be downloaded from {@code
     * https://github.com/organization/repository/archive/version.zip}.
     */
    Property<String> getVersion();

    @Override
    default TypeOf<?> getPublicType() {
      return TypeOf.typeOf(TerraformSetupExtension.class);
    }
  }

  @Override
  default TypeOf<?> getPublicType() {
    return TypeOf.typeOf(TerraformSetupExtension.class);
  }
}
