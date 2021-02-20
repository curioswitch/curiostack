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

package org.curioswitch.gradle.plugins.terraform;

import static org.curioswitch.gradle.testing.assertj.CurioGradleAssertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import org.curioswitch.gradle.helpers.platform.PathUtil;
import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.curioswitch.gradle.testing.GradleTempDirectories;
import org.curioswitch.gradle.testing.ResourceProjects;
import org.gradle.api.Project;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class TerraformSetupPluginTest {

  private Path projectDir;

  @BeforeAll
  void copyProject() {
    projectDir = ResourceProjects.fromResources("test-projects/gradle-curiostack-plugin/terraform");
  }

  // A default mock will always return null for findProperty which makes our tests independent of
  // default versions.
  @Mock private Project project;

  @BeforeEach
  void setUp() {
    when(project.getRootProject()).thenReturn(project);
  }

  @Test
  void normal() {
    Path gradleUserHome = GradleTempDirectories.create("tfhome");
    assertThat(
            GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("toolsSetupTerraform", "--stacktrace")
                .withPluginClasspath()
                .withTestKitDir(gradleUserHome.toFile()))
        .builds()
        .tasksDidSucceed(
            ":toolsDownloadTerraform",
            ":toolsSetupTerraform",
            ":toolsDownloadTerraformProviderGsuite",
            ":toolsSetupTerraformProviderGsuite",
            ":terraformBuildTerraformProviderK8s",
            ":terraformBuildTerraformProviderK8sNext",
            ":terraformCopyPlugins");

    var terraformDir = gradleUserHome.resolve("curiostack/terraform").resolve("0.12.6");

    assertThat(terraformDir.resolve(PathUtil.getExeName("terraform"))).exists();

    assertThat(
            terraformDir.resolve(
                PathUtil.getExeName(
                    "terraform-provider-gsuite_v"
                        + ToolDependencies.getTerraformGsuiteProviderVersion(project))))
        .exists();

    assertThat(terraformDir.resolve(PathUtil.getExeName("terraform-provider-k8s_v1.0.1-choko")))
        .exists();

    assertThat(
            terraformDir.resolve(PathUtil.getExeName("terraform-provider-k8s-next_v0.0.1-choko")))
        .exists();
  }
}
