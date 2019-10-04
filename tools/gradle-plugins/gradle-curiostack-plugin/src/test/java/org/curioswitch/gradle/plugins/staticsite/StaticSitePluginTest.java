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

import static org.curioswitch.gradle.testing.assertj.CurioGradleAssertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import org.curioswitch.gradle.testing.ResourceProjects;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DisabledIfEnvironmentVariable(named = "CLOUDBUILD_BUILD_ID", matches = ".+")
class StaticSitePluginTest {

  private Path projectDir;

  @BeforeAll
  void copyProject() {
    projectDir = ResourceProjects.fromResources("test-projects/gradle-static-site-plugin");
  }

  @Test
  void normal() {
    assertThat(
            GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("build", "--stacktrace")
                .withEnvironment(ImmutableMap.of())
                .withPluginClasspath())
        .builds()
        .tasksDidSucceed(":site1:buildSite", ":site2:buildSite", ":portal:mergeSite");

    assertThat(projectDir.resolve("portal/build/site/index.html"))
        .hasSameContentAs(projectDir.resolve("portal/src/index.html"));
    assertThat(projectDir.resolve("portal/build/site/index.js"))
        .hasSameContentAs(projectDir.resolve("portal/src/index.js"));
    assertThat(projectDir.resolve("portal/build/site/site1/index.html"))
        .hasSameContentAs(projectDir.resolve("site1/build/site/index.html"));
    assertThat(projectDir.resolve("portal/build/site/site2/index.html"))
        .hasSameContentAs(projectDir.resolve("site2/build/customout/index.html"));
  }
}
