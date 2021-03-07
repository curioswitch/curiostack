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
package org.curioswitch.gradle.plugins.curiostack;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import org.gradle.api.Project;

public class ToolDependencies {

  private static Map<String, String> DEFAULT_VERSIONS =
      ImmutableMap.<String, String>builder()
          .put("awscli", "2.1.28")
          .put("bom", "0.7.0")
          .put("claat", "2.2.0")
          .put("gcloud", "329.0.0")
          .put("golang", "1.16")
          .put("google-java-format", "1.9")
          .put("gradle", "6.8.3")
          .put("miniconda", "Miniconda3-py39_4.9.2")
          .put("node", "14.16.0")
          .put("openjdk", "15")
          .put("pulumi", "2.21.2")
          .put("terraform", "0.14.7")
          .put("terraform-gsuite-provider", "0.1.58")
          .put("yarn", "1.22.5")
          .build();

  public static String getAwsCliVersion(Project project) {
    return getVersion("awscli", project);
  }

  public static String getBomVersion(Project project) {
    return getVersion("bom", project);
  }

  public static String getClaatVersion(Project project) {
    return getVersion("claat", project);
  }

  public static String getGcloudVersion(Project project) {
    return getVersion("gcloud", project);
  }

  public static String getGoogleJavaFormatVersion(Project project) {
    return getVersion("google-java-format", project);
  }

  public static String getGradleVersion(Project project) {
    return getVersion("gradle", project);
  }

  public static String getGolangVersion(Project project) {
    return getVersion("golang", project);
  }

  public static String getMinicondaVersion(Project project) {
    return getVersion("miniconda", project);
  }

  public static String getNodeVersion(Project project) {
    return getVersion("node", project);
  }

  public static String getOpenJdkVersion(Project project) {
    return getVersion("openjdk", project);
  }

  public static String getPulumiVersion(Project project) {
    return getVersion("pulumi", project);
  }

  public static String getTerraformVersion(Project project) {
    return getVersion("terraform", project);
  }

  public static String getTerraformGsuiteProviderVersion(Project project) {
    return getVersion("terraform-gsuite-provider", project);
  }

  public static String getYarnVersion(Project project) {
    return getVersion("yarn", project);
  }

  public static String getDefaultVersion(String tool) {
    return DEFAULT_VERSIONS.getOrDefault(tool, "");
  }

  private static String getVersion(String tool, Project project) {
    return Objects.requireNonNullElse(
        (String) project.getRootProject().findProperty("org.curioswitch.curiostack.tools." + tool),
        DEFAULT_VERSIONS.getOrDefault(tool, ""));
  }

  private ToolDependencies() {}
}
