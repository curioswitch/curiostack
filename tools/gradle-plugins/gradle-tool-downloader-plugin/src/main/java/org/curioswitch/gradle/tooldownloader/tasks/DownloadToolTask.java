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
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderExtension;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderExtension.OsValues;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

public class DownloadToolTask extends DefaultTask {

  private static final Splitter URL_SPLITTER = Splitter.on('/');

  // It is extremely hacky to use global state to propagate the Task to workers, but
  // it works so let's enjoy the speed.
  private static final ConcurrentHashMap<String, DownloadToolTask> TASKS =
      new ConcurrentHashMap<>();

  private final String name;
  private final Property<String> artifact;
  private final Property<String> version;
  private final Property<String> baseUrl;
  private final Property<String> artifactPattern;
  private final ListProperty<String> additionalCacheDirs;
  private final OsValues osClassifiers;
  private final OsValues osExtensions;

  private final PlatformHelper platformHelper;
  private final DownloadedToolManager toolManager;

  private final WorkerExecutor workerExecutor;

  private Action<File> archiveExtractAction;

  @Inject
  public DownloadToolTask(
      ToolDownloaderExtension config,
      PlatformHelper platformHelper,
      DownloadedToolManager toolManager,
      WorkerExecutor workerExecutor) {
    this.platformHelper = platformHelper;
    this.toolManager = toolManager;
    this.workerExecutor = workerExecutor;

    setGroup("Tools");

    var objects = getProject().getObjects();
    artifact = objects.property(String.class);
    version = objects.property(String.class);
    baseUrl = objects.property(String.class);
    artifactPattern = objects.property(String.class);
    additionalCacheDirs = objects.listProperty(String.class);

    name = config.getName();
    artifact.set(config.getArtifact());
    version.set(config.getVersion());
    baseUrl.set(config.getBaseUrl());
    artifactPattern.set(config.getArtifactPattern());
    additionalCacheDirs.set(config.getAdditionalCachedDirs());
    osClassifiers = config.getOsClassifiers();
    osExtensions = config.getOsExtensions();

    onlyIf(unused -> !getToolDir().toFile().exists());
  }

  @Input
  public String getToolName() {
    return name;
  }

  @Inject
  public ListProperty<String> getAdditionalCacheDirs() {
    return additionalCacheDirs;
  }

  @Input
  public String getUrlPath() {
    var operatingSystem = platformHelper.getOs();
    return artifactPattern
        .get()
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
  public void exec() {
    checkNotNull(baseUrl.getOrNull(), "baseUrl must be set.");
    checkNotNull(artifactPattern.getOrNull(), "artifactPattern must be set");

    String mapKey = UUID.randomUUID().toString();
    TASKS.put(mapKey, this);

    workerExecutor.submit(
        DownloadArchive.class,
        config -> {
          config.setIsolationMode(IsolationMode.NONE);
          config.setDisplayName("Download " + name);
          config.params(mapKey);
        });
  }

  public static class DownloadArchive implements Runnable {

    private final String mapKey;

    @Inject
    public DownloadArchive(String mapKey) {
      this.mapKey = mapKey;
    }

    @Override
    public void run() {
      DownloadToolTask task = TASKS.remove(mapKey);

      File archiveDir = task.getTemporaryDir();
      String url = task.baseUrl.get() + task.getUrlPath();
      String archiveName = Iterables.getLast(URL_SPLITTER.splitToList(url));
      File archive = new File(archiveDir, archiveName);

      var download = new DownloadAction(task.getProject());
      try {
        download.src(url);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      download.dest(archive);
      try {
        download.execute();
      } catch (IOException e) {
        throw new UncheckedIOException("Could not download archive.", e);
      }

      if (task.archiveExtractAction != null) {
        task.archiveExtractAction.execute(archive);
      } else {
        unpackArchive(archive, task);
      }
    }

    private static void unpackArchive(File archive, DownloadToolTask task) {
      var project = task.getProject();

      boolean isTar = archive.getName().contains(".tar.");

      if (isTar && new PlatformHelper().getOs() != OperatingSystem.WINDOWS) {
        // Use tar command on unix since package might have symlinks. All unixes should have tar in
        // practice.
        project.exec(
            exec -> {
              exec.executable("tar");
              exec.args("-xf", archive.getAbsolutePath());
              exec.workingDir(task.getToolDir());
            });
      } else {
        project.copy(
            copy -> {
              if (archive.getName().endsWith(".zip")) {
                copy.from(project.zipTree(archive));
              } else if (archive.getName().contains(".tar.")) {
                copy.from(project.tarTree(archive));
              }
              copy.into(task.getToolDir());
            });
      }
    }
  }
}
