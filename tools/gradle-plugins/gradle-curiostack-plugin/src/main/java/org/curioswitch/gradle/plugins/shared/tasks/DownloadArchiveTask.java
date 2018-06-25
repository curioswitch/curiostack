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

package org.curioswitch.gradle.plugins.shared.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.List;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout;
import org.gradle.api.tasks.TaskAction;

/** A downloader of archives from an ivy-like repository. */
public class DownloadArchiveTask extends DefaultTask {

  private String baseUrl;
  private String artifactPattern;
  private String dependency;

  public final void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public final void setArtifactPattern(String artifactPattern) {
    this.artifactPattern = artifactPattern;
  }

  public final void setDependency(String dependency) {
    this.dependency = dependency;
  }

  @TaskAction
  void exec() {
    checkNotNull(baseUrl, "baseUrl must be set.");
    checkNotNull(artifactPattern, "artifactPattern must be set.");
    checkNotNull(dependency, "dependency must be set.");

    List<ArtifactRepository> repositoriesBackup = getProject().getRepositories();

    Dependency dependency = getProject().getDependencies().create(this.dependency);
    setRepository(baseUrl, artifactPattern);

    File archive = resolveAndFetchArchive(dependency);
    unpackArchive(archive, dependency);

    restoreRepositories(repositoriesBackup);
  }

  private File resolveAndFetchArchive(Dependency dependency) {
    var configuration = getProject().getConfigurations().detachedConfiguration(dependency);
    configuration.setTransitive(false);
    return configuration.resolve().iterator().next();
  }

  private void unpackArchive(File archive, Dependency dependency) {
    var project = getProject();
    project.copy(
        copy -> {
          if (archive.getAbsolutePath().endsWith(".zip")) {
            copy.from(project.zipTree(archive));
          } else {
            copy.from(project.tarTree(archive));
          }
          copy.into(
              CommandUtil.getCuriostackDir(project)
                  .resolve(dependency.getName())
                  .resolve(dependency.getVersion()));
        });
  }

  private void setRepository(String baseUrl, String artifactPattern) {
    var repositories = getProject().getRepositories();
    repositories.clear();
    repositories.ivy(
        repo -> {
          repo.setUrl(baseUrl);
          repo.layout(
              "pattern",
              (IvyPatternRepositoryLayout layout) -> {
                layout.artifact(artifactPattern);
              });
        });
  }

  private void restoreRepositories(List<ArtifactRepository> repositoriesBackup) {
    var project = getProject();
    project.getRepositories().clear();
    project.getRepositories().addAll(repositoriesBackup);
  }
}
