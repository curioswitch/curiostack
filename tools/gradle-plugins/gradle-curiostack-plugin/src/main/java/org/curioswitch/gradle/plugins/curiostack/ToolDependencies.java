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

package org.curioswitch.gradle.plugins.curiostack;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import org.gradle.api.Project;

public class ToolDependencies {

  private static Map<String, String> DEFAULT_VERSIONS =
      ImmutableMap.<String, String>builder()
          .put("bom", "0.0.12")
          .put("claat", "2.2.0")
          .put("gcloud", "259.0.0")
          .put("golang", "1.12.9")
          .put("google-java-format", "1.7")
          .put("gradle", "5.6")
          .put("helm", "2.10.0")
          .put("miniconda", "4.5.12")
          .put("node", "11.13.0")
          .put("openjdk8", "jdk8u222-b10")
          .put("terraform", "0.12.6")
          .put("terraform-gsuite-provider", "0.1.23")
          .put("yarn", "1.17.3")
          .build();

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

  public static String getHelmVersion(Project project) {
    return getVersion("helm", project);
  }

  public static String getMinicondaVersion(Project project) {
    return getVersion("miniconda", project);
  }

  public static String getNodeVersion(Project project) {
    return getVersion("node", project);
  }

  public static String getOpenJdk8Version(Project project) {
    return getVersion("openjdk8", project);
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
    return DEFAULT_VERSIONS.get(tool);
  }

  private static String getVersion(String tool, Project project) {
    return Objects.requireNonNullElse(
        (String) project.getRootProject().findProperty("org.curioswitch.curiostack.tools." + tool),
        DEFAULT_VERSIONS.get(tool));
  }

  private ToolDependencies() {}
}
