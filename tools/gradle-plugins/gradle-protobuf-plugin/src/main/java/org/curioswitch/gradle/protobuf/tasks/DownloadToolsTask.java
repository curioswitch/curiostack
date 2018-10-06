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

package org.curioswitch.gradle.protobuf.tasks;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.gradle.osdetector.OsDetector;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import org.curioswitch.gradle.protobuf.ProtobufExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

public class DownloadToolsTask extends DefaultTask {

  public static final String NAME = "protoDownloadTools";

  private static final Splitter COORDINATE_SPLITTER = Splitter.on('@');
  private static final Splitter ARTIFACT_SPLITTER = Splitter.on(':');

  private final ListProperty<String> artifacts;

  private final Map<String, File> toolPaths;

  @Inject
  public DownloadToolsTask(ProtobufExtension config) {
    artifacts = getProject().getObjects().listProperty(String.class);

    toolPaths = new HashMap<>();

    artifacts.add(config.getProtoc().getArtifact());

    onlyIf(unused -> artifacts.isPresent());
  }

  @Input
  public ListProperty<String> getArtifacts() {
    return artifacts;
  }

  @OutputFiles
  public Map<String, File> getToolPaths() {
    return toolPaths;
  }

  public DownloadToolsTask artifact(String artifact) {
    artifacts.add(artifact);
    return this;
  }

  public DownloadToolsTask artifact(Provider<String> artifact) {
    artifacts.add(artifact);
    return this;
  }

  @TaskAction
  public void exec() {
    RepositoryHandler repositories = getProject().getRepositories();
    List<ArtifactRepository> currentRepositories = ImmutableList.copyOf(repositories);
    // Make sure Maven Central is present as a repository since it's the usual place to
    // get protoc, even for non-Java projects. We restore to the previous state after the task.
    repositories.mavenCentral();

    List<String> artifacts = this.artifacts.get();

    Dependency[] dependencies =
        artifacts
            .stream()
            .map(
                artifact -> {
                  checkArgument(!artifact.isEmpty(), "artifact must not be empty");

                  List<String> coordinateParts = COORDINATE_SPLITTER.splitToList(artifact);

                  List<String> artifactParts =
                      ARTIFACT_SPLITTER.splitToList(coordinateParts.get(0));

                  ImmutableMap.Builder<String, String> depParts =
                      ImmutableMap.builderWithExpectedSize(5);

                  // Do a loose matching to allow for the possibility of dependency management
                  // manipulation.
                  if (artifactParts.size() > 0) {
                    depParts.put("group", artifactParts.get(0));
                  }
                  if (artifactParts.size() > 1) {
                    depParts.put("name", artifactParts.get(1));
                  }
                  if (artifactParts.size() > 2) {
                    depParts.put("version", artifactParts.get(2));
                  }

                  if (artifactParts.size() > 3) {
                    depParts.put("classifier", artifactParts.get(3));
                  } else {
                    depParts.put(
                        "classifier",
                        getProject().getExtensions().getByType(OsDetector.class).getClassifier());
                  }

                  if (coordinateParts.size() > 1) {
                    depParts.put("ext", coordinateParts.get(1));
                  } else {
                    depParts.put("ext", "exe");
                  }

                  return getProject().getDependencies().create(depParts.build());
                })
            .toArray(Dependency[]::new);
    Configuration configuration =
        getProject().getConfigurations().detachedConfiguration(dependencies);

    // Resolve once to download all tools in parallel.
    configuration.resolve();

    // This will not redownload.
    ResolvedConfiguration resolved = configuration.getResolvedConfiguration();
    Streams.zip(
        artifacts.stream(),
        Arrays.stream(dependencies),
        (artifact, dep) -> {
          Set<File> files =
              resolved.getFiles(
                  d -> {
                    // Dependency.contentEquals doesn't match for some reason...
                    return Objects.equals(dep.getGroup(), d.getGroup())
                        && dep.getName().equals(d.getName())
                        && Objects.equals(dep.getVersion(), d.getVersion());
                  });
          checkState(files.size() == 1);
          toolPaths.put(artifact, Iterables.getOnlyElement(files));
          return null;
        }
    );

    repositories.clear();
    repositories.addAll(currentRepositories);
  }
}
