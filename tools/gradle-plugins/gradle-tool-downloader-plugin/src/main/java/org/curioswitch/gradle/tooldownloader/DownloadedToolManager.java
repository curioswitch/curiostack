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

package org.curioswitch.gradle.tooldownloader;

import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

public class DownloadedToolManager {

  private final Path curiostackDir;

  private final ConcurrentHashMap<String, Provider<String>> toolVersions;

  @Inject
  public DownloadedToolManager(Project project) {
    toolVersions = new ConcurrentHashMap<>();

    curiostackDir = project.getGradle().getGradleUserHomeDir().toPath().resolve("curiostack");
  }

  void register(String tool, Provider<String> version) {
    toolVersions.put(tool, version);
  }

  public Path getToolDir(String tool) {
    checkState(toolVersions.containsKey(tool), "Tool %s not registered.", tool);
    return curiostackDir.resolve(tool).resolve(toolVersions.get(tool).get());
  }
}
