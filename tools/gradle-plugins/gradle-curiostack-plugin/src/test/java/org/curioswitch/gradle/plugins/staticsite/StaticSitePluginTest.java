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

package org.curioswitch.gradle.plugins.staticsite;

import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StaticSitePluginTest {

  private Path projectDir;

  @BeforeAll
  void copyProject(@TempDir Path projectDir) throws Exception {
    copyProjectFromResources("test-projects/gradle-static-site-plugin", projectDir);

    this.projectDir = projectDir;
  }

  @Test
  void normal() {
    var result =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("build")
            .withPluginClasspath()
            .build();

    assertThat(result.task(":site1:buildSite").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.task(":site2:buildSite").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.task(":portal:mergeSite").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    assertThat(projectDir.resolve("portal/build/site/index.html"))
        .hasSameContentAs(projectDir.resolve("portal/src/index.html"));
    assertThat(projectDir.resolve("portal/build/site/index.js"))
        .hasSameContentAs(projectDir.resolve("portal/src/index.js"));
    assertThat(projectDir.resolve("portal/build/site/site1/index.html"))
        .hasSameContentAs(projectDir.resolve("site1/build/site/index.html"));
    assertThat(projectDir.resolve("portal/build/site/site2/index.html"))
        .hasSameContentAs(projectDir.resolve("site2/build/customout/index.html"));
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
  }
}
