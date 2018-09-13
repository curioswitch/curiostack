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

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderExtension;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderExtension.OsValues;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public class DownloadToolTask extends DefaultTask {

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
  public String getDependency() {
    var operatingSystem = platformHelper.getOs();
    return "org.curioswitch.curiostack.downloaded_tools:"
        + artifact.get()
        + ":"
        + version.get()
        + ":"
        + osClassifiers.getValue(operatingSystem)
        + "@"
        + osExtensions.getValue(operatingSystem);
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
  void exec() {
    checkNotNull(baseUrl.get(), "baseUrl must be set.");
    checkNotNull(artifactPattern.get(), "artifactPattern must be set");

    var currentRepositories = ImmutableList.copyOf(getProject().getRepositories());
    System.out.println(currentRepositories);
    setRepository();

    File archive = resolveAndFetchArchive(getProject().getDependencies().create(getDependency()));

    restoreRepositories(currentRepositories);

    if (archiveExtractAction != null) {
      archiveExtractAction.execute(archive);
    } else {
      unpackArchive(archive);
    }
  }

  private void setRepository() {
    var repositories = getProject().getRepositories();
    repositories.clear();
    repositories.ivy(
        repo -> {
          repo.setUrl(baseUrl.get());
          repo.layout(
              "pattern",
              (IvyPatternRepositoryLayout layout) -> layout.artifact(artifactPattern.get()));
        });
  }

  private File resolveAndFetchArchive(Dependency dependency) {
    var configuration = getProject().getConfigurations().detachedConfiguration(dependency);
    configuration.setTransitive(false);
    return configuration.resolve().iterator().next();
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

  private void restoreRepositories(List<ArtifactRepository> repositoriesBackup) {
    var repositories = getProject().getRepositories();
    repositories.clear();
    repositories.addAll(repositoriesBackup);
  }
}
