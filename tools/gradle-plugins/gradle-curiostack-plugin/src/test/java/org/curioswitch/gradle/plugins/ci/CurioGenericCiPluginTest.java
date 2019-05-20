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

package org.curioswitch.gradle.plugins.ci;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CurioGenericCiPluginTest {

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class MasterBranchNoDiffs {

    private Path projectDir;

    @BeforeAll
    void copyProject(@TempDir Path projectDir) throws Exception {
      this.projectDir = projectDir;

      copyProjectFromResources("test-projects/gradle-curio-generic-ci-plugin/normal", projectDir);
    }

    @Test
    void noCi() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir.toFile())
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of())
              .withPluginClasspath()
              .buildAndFail();

      assertThat(result.getOutput()).contains("Task 'continuousBuild' not found in root project");
    }

    @Test
    void ciNotMaster() {
      var result =
          GradleRunner.create()
              .withProjectDir(projectDir.toFile())
              .withArguments("continuousBuild")
              .withEnvironment(ImmutableMap.of("CI", "true"))
              .withPluginClasspath()
              .build();

      assertThat(result.task(":library1:build")).isNull();
      assertThat(result.task(":server1:build")).isNull();
    }
  }

  private static void copyProjectFromResources(String resourcePath, Path projectDir)
      throws Exception {
    var resourceDir = Paths.get(Resources.getResource(resourcePath).toURI());
    try (var stream = Files.walk(resourceDir)) {
      stream
          .filter(path -> !path.equals(resourceDir))
          .forEach(
              path -> {
                var destPath = projectDir.resolve(resourceDir.relativize(path));
                try {
                  if (Files.isDirectory(path)) {
                    Files.createDirectory(destPath);
                  } else {
                    Files.copy(path, destPath);
                  }
                } catch (IOException e) {
                  throw new UncheckedIOException("Could not copy test project file.", e);
                }
              });
    }

    var projectFiles = projectDir.toFile().listFiles();
    checkNotNull(projectFiles);
    for (var file : projectFiles) {
      if (file.getName().equals("dot-git")) {
        if (!file.renameTo(new File(file.getParent(), ".git"))) {
          throw new IllegalStateException("Could not rename dot-git to .git.");
        }
        break;
      }
    }
  }
}
