/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.plugins.gcloud.tasks;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableGcloudExtension;
import org.curioswitch.gradle.plugins.gcloud.PlatformConfig;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;

public class SetupTask extends DefaultTask {

  public static final String NAME = "gcloudDownloadSdk";

  private final ImmutableGcloudExtension config;
  private final PlatformConfig platformConfig;
  private final List<ArtifactRepository> repositoriesBackup;

  public SetupTask() {
    this.config = getProject().getExtensions().getByType(GcloudExtension.class);
    platformConfig = config.platformConfig();
    repositoriesBackup = new ArrayList<>(getProject().getRepositories());

    onlyIf(
        unused -> {
          Path sdkDir = CommandUtil.getGcloudSdkDir(getProject());
          if (!Files.exists(sdkDir)) {
            return true;
          }
          try {
            return !Files.readAllLines(sdkDir.resolve("VERSION")).contains(config.version());
          } catch (IOException e) {
            throw new UncheckedIOException("Could not read VERSION file.", e);
          }
        });
  }

  @TaskAction
  public void exec() {
    addGcloudRepository();
    unpackArchive();
    restoreRepositories();
  }

  @Input
  public String getInput() {
    return platformConfig.dependency();
  }

  @OutputDirectories
  public Map<String, Path> getSdkDir() {
    return ImmutableMap.of("gcloud-sdk", CommandUtil.getGcloudSdkDir(getProject()));
  }

  private void unpackArchive() {
    Path gcloudSdkDir = CommandUtil.getGcloudSdkDir(getProject());
    if (Files.exists(gcloudSdkDir)) {
      try (Stream<Path> s = Files.walk(gcloudSdkDir)) {
        s.sorted(Comparator.reverseOrder())
            .forEach(
                p -> {
                  try {
                    Files.delete(p);
                  } catch (IOException e) {
                    throw new UncheckedIOException("Could not delete sdk file.", e);
                  }
                });
      } catch (IOException e) {
        throw new UncheckedIOException("Could not walk sdk dir.", e);
      }
    }
    getProject()
        .copy(
            copy -> {
              if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                copy.from(getProject().zipTree(resolveAndFetchArchive()));
              } else {
                copy.from(getProject().tarTree(resolveAndFetchArchive()));
              }
              copy.into(CommandUtil.getGcloudDir(getProject()));
            });
  }

  private File resolveAndFetchArchive() {
    Dependency dep = getProject().getDependencies().create(platformConfig.dependency());
    Configuration conf = getProject().getConfigurations().detachedConfiguration(dep);
    conf.setTransitive(false);
    return conf.resolve().iterator().next();
  }

  private void addGcloudRepository() {
    getProject().getRepositories().clear();
    getProject()
        .getRepositories()
        .ivy(
            repo -> {
              repo.setUrl(config.distBaseUrl());
              repo.layout(
                  "pattern",
                  layout -> {
                    IvyPatternRepositoryLayout ivyLayout = (IvyPatternRepositoryLayout) layout;
                    ivyLayout.artifact("[artifact](-[revision]-[classifier]).[ext]");
                    ivyLayout.ivy("[revision]/ivy.xml");
                  });
            });
  }

  private void restoreRepositories() {
    getProject().getRepositories().clear();
    getProject().getRepositories().addAll(repositoriesBackup);
  }
}
