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

package org.curioswitch.gradle.tooldownloader.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import de.undercouch.gradle.tasks.download.DownloadAction;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderExtension;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderExtension.OsValues;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public class DownloadToolTask extends DefaultTask {

  private static final Splitter URL_SPLITTER = Splitter.on('/');

  private final String name;
  private final Property<String> artifact;
  private final Property<String> version;
  private final Property<String> baseUrl;
  private final Property<String> artifactPattern;
  private final OsValues osClassifiers;
  private final OsValues osExtensions;

  private final PlatformHelper platformHelper;
  private final DownloadedToolManager toolManager;

  private Action<File> archiveExtractAction;

  @Inject
  public DownloadToolTask(
      ToolDownloaderExtension config,
      PlatformHelper platformHelper,
      DownloadedToolManager toolManager) {
    this.platformHelper = platformHelper;
    this.toolManager = toolManager;

    setGroup("Tools");

    var objects = getProject().getObjects();
    artifact = objects.property(String.class);
    version = objects.property(String.class);
    baseUrl = objects.property(String.class);
    artifactPattern = objects.property(String.class);

    name = config.getName();
    artifact.set(config.getArtifact());
    version.set(config.getVersion());
    baseUrl.set(config.getBaseUrl());
    artifactPattern.set(config.getArtifactPattern());
    osClassifiers = config.getOsClassifiers();
    osExtensions = config.getOsExtensions();

    onlyIf(unused -> !getToolDir().toFile().exists());
  }

  @Input
  public String getUrlPath() {
    var operatingSystem = platformHelper.getOs();
    return artifactPattern.get()
        .replace("[artifact]", artifact.get())
        .replace("[revision]", version.get())
        .replace("[classifier]", osClassifiers.getValue(operatingSystem))
        .replace("[ext]", osExtensions.getValue(operatingSystem));
  }

  @OutputDirectory
  public Path getToolDir() {
    return toolManager.getToolDir(name);
  }

  public DownloadToolTask setArchiveExtractAction(Action<File> archiveExtractAction) {
    this.archiveExtractAction = archiveExtractAction;
    return this;
  }

  @TaskAction
  public void exec() throws IOException {
    checkNotNull(baseUrl.get(), "baseUrl must be set.");
    checkNotNull(artifactPattern.get(), "artifactPattern must be set");

    File archiveDir = getTemporaryDir();
    String url = baseUrl.get() + getUrlPath();
    String archiveName = Iterables.getLast(URL_SPLITTER.splitToList(url));
    File archive = new File(archiveDir, archiveName);

    var download = new DownloadAction(getProject());
    download.src(url);
    download.dest(archive);
    download.execute();

    if (archiveExtractAction != null) {
      archiveExtractAction.execute(archive);
    } else {
      unpackArchive(archive);
    }
  }

  private void unpackArchive(File archive) {
    var project = getProject();
    project.copy(
        copy -> {
          if (archive.getName().endsWith(".zip")) {
            copy.from(project.zipTree(archive));
          } else if (archive.getName().contains(".tar.")) {
            copy.from(project.tarTree(archive));
          }
          copy.into(getToolDir());
        });
  }
}
