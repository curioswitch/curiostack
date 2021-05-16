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

// Includes work from:
/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.curioswitch.gradle.golang.tasks;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LayerConfiguration;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.Port;
import com.google.cloud.tools.jib.event.events.ProgressEvent;
import com.google.cloud.tools.jib.event.events.TimerEvent;
import com.google.cloud.tools.jib.event.progress.ProgressEventHandler;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import com.google.cloud.tools.jib.plugins.common.PropertyNames;
import com.google.cloud.tools.jib.plugins.common.TimerEventHandler;
import com.google.cloud.tools.jib.plugins.common.logging.ConsoleLoggerBuilder;
import com.google.cloud.tools.jib.plugins.common.logging.ProgressDisplayGenerator;
import com.google.cloud.tools.jib.plugins.common.logging.SingleThreadedExecutor;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.curioswitch.gradle.golang.GolangExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
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
  private final MapProperty<String, String> environmentVariables;

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
    environmentVariables = objects.mapProperty(String.class, String.class);

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
    environmentVariables.set(jib.getEnvironmentVariables());

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
    // Logging related code copied from GradleProjectProperties, which isn't public.
    var logger = getProject().getLogger();
    SingleThreadedExecutor singleThreadedExecutor = new SingleThreadedExecutor();
    ConsoleLoggerBuilder consoleLoggerBuilder =
        (isProgressFooterEnabled(getProject())
                ? ConsoleLoggerBuilder.rich(singleThreadedExecutor, false)
                : ConsoleLoggerBuilder.plain(singleThreadedExecutor).progress(logger::lifecycle))
            .lifecycle(logger::lifecycle);
    if (logger.isDebugEnabled()) {
      consoleLoggerBuilder.debug(logger::debug);
    }
    if (logger.isInfoEnabled()) {
      consoleLoggerBuilder.info(logger::info);
    }
    if (logger.isWarnEnabled()) {
      consoleLoggerBuilder.warn(logger::warn);
    }
    if (logger.isErrorEnabled()) {
      consoleLoggerBuilder.error(logger::error);
    }
    var consoleLogger = consoleLoggerBuilder.build();

    var baseImage = RegistryImage.named(this.baseImage.get());
    var targetImage = RegistryImage.named(this.targetImage.get());

    Consumer<LogEvent> logEventConsumer =
        logEvent -> consoleLogger.log(logEvent.getLevel(), logEvent.getMessage());

    if (credentialHelper.isPresent()) {
      baseImage.addCredentialRetriever(
          CredentialRetrieverFactory.forImage(
                  ImageReference.parse(this.baseImage.get()), logEventConsumer)
              .dockerCredentialHelper(credentialHelper.get()));
      targetImage.addCredentialRetriever(
          CredentialRetrieverFactory.forImage(
                  ImageReference.parse(this.targetImage.get()), logEventConsumer)
              .dockerCredentialHelper(credentialHelper.get()));
    }

    var exePath = this.exePath.get();
    var containerize =
        Containerizer.to(targetImage)
            .addEventHandler(LogEvent.class, logEventConsumer)
            .addEventHandler(
                TimerEvent.class,
                new TimerEventHandler(message -> consoleLogger.log(LogEvent.Level.DEBUG, message)))
            .addEventHandler(
                ProgressEvent.class,
                new ProgressEventHandler(
                    update -> {
                      List<String> footer =
                          ProgressDisplayGenerator.generateProgressDisplay(
                              update.getProgress(), update.getUnfinishedLeafTasks());
                      footer.add("");
                      consoleLogger.setFooter(footer);
                    }));
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

    environmentVariables.get().forEach(jib::addEnvironmentVariable);

    jib.containerize(containerize);
  }

  // Incompatible with switch expressions.
  @SuppressWarnings({"NullAway", "UnnecessaryParentheses"})
  private static boolean isProgressFooterEnabled(Project project) {
    if ("plain".equals(System.getProperty(PropertyNames.CONSOLE))) {
      return false;
    }

    // Enables progress footer when ANSI is supported (Windows or TERM not 'dumb').
    return switch (project.getGradle().getStartParameter().getConsoleOutput()) {
      case Plain -> false;
      case Auto -> Os.isFamily(Os.FAMILY_WINDOWS) || !"dumb".equals(System.getenv("TERM"));
      default -> true;
    };
  }
}
