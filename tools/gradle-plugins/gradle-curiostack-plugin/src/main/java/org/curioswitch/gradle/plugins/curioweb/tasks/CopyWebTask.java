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

package org.curioswitch.gradle.plugins.curioweb.tasks;

import java.io.File;
import javax.annotation.Nullable;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public class CopyWebTask extends DefaultTask {

  private final Property<String> javaPackage;
  private final Property<String> storybookJavaPackage;

  public CopyWebTask() {
    var objects = getProject().getObjects();

    javaPackage = objects.property(String.class);
    storybookJavaPackage = objects.property(String.class);
  }

  @InputDirectory
  public File getWebDir() {
    return getProject().file("build/web");
  }

  @InputDirectory
  @Optional
  @Nullable
  public File getStorybookDir() {
    var f = getProject().file("build/storybook");
    if (f.exists()) {
      return f;
    }
    return null;
  }

  @Input
  public Property<String> getJavaPackage() {
    return javaPackage;
  }

  @Input
  @Optional
  public Property<String> getStorybookJavaPackage() {
    return storybookJavaPackage;
  }

  @OutputDirectory
  public File getOutputDir() {
    return getProject().file("build/javaweb");
  }

  @TaskAction
  public void exec() {
    getProject()
        .copy(
            copy -> {
              copy.from("build/web");
              copy.into("build/javaweb/" + javaPackage.get().replace('.', '/'));
            });
    if (storybookJavaPackage.isPresent()) {
      getProject()
          .copy(
              copy -> {
                copy.from("build/storybook");
                copy.into("build/javaweb/" + storybookJavaPackage.get().replace('.', '/'));
              });
    }
  }
}
