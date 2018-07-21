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

package org.curioswitch.gradle.plugins.helm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UncheckedIOException;
import org.curioswitch.gradle.plugins.helm.tasks.HelmTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TerraformOutputTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class HelmPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project
        .getTasks()
        .create(
            "helmInit",
            HelmTask.class,
            t -> {
              var tillerCaCertFile = project.file("build/helm/tiller-ca.cert");
              var tillerServerCertFile = project.file("build/helm/tiller-server.cert");
              var tillerServerKeyFile = project.file("build/helm/tiller-server.key");

              t.dependsOn(
                  createTerraformOutputTask(
                      project, "outputTillerCaCertTask", "tiller-ca-cert", tillerCaCertFile));

              t.dependsOn(
                  createTerraformOutputTask(
                      project,
                      "outputTillerServerCertTask",
                      "tiller-server-cert",
                      tillerServerCertFile));

              t.dependsOn(
                  createTerraformOutputTask(
                      project,
                      "outputTillerServerKeyTask",
                      "tiller-server-key",
                      tillerServerKeyFile));

              t.setArgs(
                  ImmutableList.of(
                      "init",
                      "--service-account=tiller",
                      "--tiller-tls",
                      "--tiller-tls-verify",
                      "--tiller-tls-cert=" + tillerServerCertFile.getAbsolutePath(),
                      "--tiller-tls-key=" + tillerServerKeyFile.getAbsolutePath(),
                      "--tls-ca-cert=" + tillerCaCertFile.getAbsolutePath()));
            });
  }

  private static Task createTerraformOutputTask(
      Project project, String taskName, String outputName, File outputFile) {
    var terraformProject = project.getRootProject().findProject(":cluster:terraform");
    checkNotNull(terraformProject);
    return terraformProject
        .getTasks()
        .create(
            taskName,
            TerraformOutputTask.class,
            t -> {
              t.doFirst(unused -> project.mkdir(outputFile.getParent()));
              t.setArgs(ImmutableList.of("output", outputName));
              t.dependsOn(terraformProject.getTasks().getByName("terraformInit"));
              t.setExecCustomizer(
                  exec -> {
                    try {
                      exec.setStandardOutput(new FileOutputStream(outputFile));
                    } catch (FileNotFoundException e) {
                      throw new UncheckedIOException("Could not open file.", e);
                    }
                  });
            });
  }
}
