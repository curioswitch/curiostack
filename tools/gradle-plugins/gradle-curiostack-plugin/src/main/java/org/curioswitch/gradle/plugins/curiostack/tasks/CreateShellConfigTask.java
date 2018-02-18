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

package org.curioswitch.gradle.plugins.curiostack.tasks;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.diffplug.common.io.Files;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public class CreateShellConfigTask extends DefaultTask {

  List<Path> paths = ImmutableList.of();

  public CreateShellConfigTask path(Path path) {
    paths = ImmutableList.<Path>builder().addAll(paths).add(path).build();
    return this;
  }

  @Input
  public List<String> getPaths() {
    return paths.stream().map(Path::toAbsolutePath).map(Path::toString).collect(toImmutableList());
  }

  @OutputFile
  public Path getConfigPath() {
    return CommandUtil.getCuriostackDir(getProject()).resolve("curiostack_config");
  }

  @TaskAction
  public void exec() {
    String joined =
        getPaths()
            .stream()
            .map(
                path -> {
                  if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                    // Assume msys or cygwin for now.
                    return "/"
                        + path.substring(0, 1).toLowerCase()
                        + "/"
                        + path.substring("C:\\".length()).replace('\\', '/');
                  } else {
                    return path;
                  }
                })
            .collect(Collectors.joining(":"));
    try {
      Files.write(
          "export PATH=" + joined + ":$PATH\n" + "export CLOUDSDK_PYTHON_SITEPACKAGES=1",
          getConfigPath().toFile(),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write config.", e);
    }
  }
}
