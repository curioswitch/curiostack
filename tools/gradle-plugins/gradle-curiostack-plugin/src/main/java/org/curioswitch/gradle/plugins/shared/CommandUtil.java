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

package org.curioswitch.gradle.plugins.shared;

import java.nio.file.Path;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Project;

public final class CommandUtil {

  public static Path getPythonDir(Project project) {
    return getCuriostackDir(project).resolve("python");
  }

  public static Path getPythonBinDir(Project project, String envName) {
    String binDir = Os.isFamily(Os.FAMILY_WINDOWS) ? "" : "bin";
    return getPythonDir(project).resolve("envs/" + envName + "/" + binDir);
  }

  public static Path getPythonExecutable(Project project, String envName) {
    return getPythonBinDir(project, envName).resolve("python");
  }

  public static Path getGcloudDir(Project project) {
    return getCuriostackDir(project).resolve("gcloud");
  }

  public static Path getGcloudSdkDir(Project project) {
    return getGcloudDir(project).resolve("google-cloud-sdk");
  }

  public static Path getGcloudSdkBinDir(Project project) {
    return getGcloudSdkDir(project).resolve("bin");
  }

  public static Path getNodeDir(Project project) {
    return getCuriostackDir(project).resolve("nodejs");
  }

  public static Path getNpmDir(Project project) {
    return getNodeDir(project).resolve("npm");
  }

  public static Path getYarnDir(Project project) {
    return getNodeDir(project).resolve("yarn");
  }

  public static Path getCuriostackDir(Project project) {
    return project.getGradle().getGradleUserHomeDir().toPath().resolve("curiostack");
  }

  private CommandUtil() {}
}
