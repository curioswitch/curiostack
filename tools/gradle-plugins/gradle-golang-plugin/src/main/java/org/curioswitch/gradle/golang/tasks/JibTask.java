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
package org.curioswitch.gradle.golang.tasks;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.configuration.FilePermissions;
import com.google.cloud.tools.jib.configuration.LayerConfiguration;
import com.google.cloud.tools.jib.configuration.Port;
import com.google.cloud.tools.jib.filesystem.AbsoluteUnixPath;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import java.nio.file.Path;
import org.curioswitch.gradle.golang.GolangExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

public class JibTask extends DefaultTask {

  private final Property<Path> exePath;
  private final Property<String> baseImage;
  private final Property<String> targetImage;
  private final ListProperty<String> additionalTags;
  private final Property<String> user;
  private final ListProperty<String> volumes;
  private final ListProperty<Integer> ports;
  private final ListProperty<String> args;
  private final Property<String> workingDir;
  private final Property<Path> credentialHelper;

  public JibTask() {
    var objects = getProject().getObjects();

    exePath = objects.property(Path.class);
    baseImage = objects.property(String.class);
    targetImage = objects.property(String.class);
    additionalTags = objects.listProperty(String.class);
    user = objects.property(String.class);
    volumes = objects.listProperty(String.class);
    ports = objects.listProperty(Integer.class);
    args = objects.listProperty(String.class);
    workingDir = objects.property(String.class);
    credentialHelper = objects.property(Path.class);

    var jib = getProject().getExtensions().getByType(GolangExtension.class).getJib();
    baseImage.set(jib.getBaseImage());
    targetImage.set(jib.getTargetImage());
    additionalTags.set(jib.getAdditionalTags());
    user.set(jib.getUser());
    volumes.set(jib.getVolumes());
    ports.set(jib.getPorts());
    args.set(jib.getArgs());
    workingDir.set(jib.getWorkingDir());
    credentialHelper.set(jib.getCredentialHelper());

    onlyIf(unused -> getTargetImage().isPresent());
  }

  @InputFile
  public Property<Path> getExePath() {
    return exePath;
  }

  @Input
  public Property<String> getBaseImage() {
    return baseImage;
  }

  @Input
  public Property<String> getTargetImage() {
    return targetImage;
  }

  public JibTask setExePath(Path exePath) {
    this.exePath.set(exePath);
    return this;
  }

  public JibTask setExePath(Provider<Path> exePath) {
    this.exePath.set(exePath);
    return this;
  }

  @TaskAction
  public void exec() throws Exception {
    var baseImage = RegistryImage.named(this.baseImage.get());
    var targetImage = RegistryImage.named(this.targetImage.get());
    if (credentialHelper.isPresent()) {
      baseImage.addCredentialRetriever(
          CredentialRetrieverFactory.forImage(baseImage.toImageConfiguration().getImage())
              .dockerCredentialHelper(credentialHelper.get()));
      targetImage.addCredentialRetriever(
          CredentialRetrieverFactory.forImage(targetImage.toImageConfiguration().getImage())
              .dockerCredentialHelper(credentialHelper.get()));
    }

    var exePath = this.exePath.get();
    var containerize = Containerizer.to(targetImage);
    additionalTags.get().forEach(containerize::withAdditionalTag);
    JibContainerBuilder jib =
        Jib.from(baseImage)
            .addLayer(
                LayerConfiguration.builder()
                    .addEntry(
                        exePath,
                        AbsoluteUnixPath.get("/opt/bin/" + exePath.getFileName()),
                        FilePermissions.DEFAULT_FOLDER_PERMISSIONS)
                    .build())
            .setEntrypoint("/opt/bin/" + exePath.getFileName());
    if (user.isPresent()) {
      jib.setUser(user.get());
    }
    for (String volume : volumes.get()) {
      jib.addVolume(AbsoluteUnixPath.get(volume));
    }
    for (int port : ports.get()) {
      jib.addExposedPort(Port.tcp(port));
    }
    jib.setProgramArguments(args.get());
    if (workingDir.isPresent()) {
      jib.setWorkingDirectory(AbsoluteUnixPath.get(workingDir.get()));
    }
    jib.containerize(containerize);
  }
}
