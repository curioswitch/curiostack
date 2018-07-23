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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.curioswitch.gradle.plugins.helm.tasks.HelmTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TerraformOutputTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;

public class HelmPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.evaluationDependsOn(":cluster:terraform");
    HelmExtension config = HelmExtension.createAndAdd(project);

    var tillerCaCertFile = project.file("build/helm/tiller-client-ca.cert");
    var tillerClientCertFile = project.file("build/helm/tiller-client.cert");
    var tillerClientKeyFile = project.file("build/helm/tiller-client.key");
    var tillerServerCertFile = project.file("build/helm/tiller-server.cert");
    var tillerServerKeyFile = project.file("build/helm/tiller-server.key");

    var outputTillerCaCertTask =
        createTerraformOutputTask(
            project, "outputTillerCaCertTask", "tiller-ca-cert", tillerCaCertFile);
    var outputTillerClientCertTask =
        createTerraformOutputTask(
            project, "outputTillerClientCertTask", "tiller-client-cert", tillerClientCertFile);
    var outputTillerClientKeyTask =
        createTerraformOutputTask(
            project, "outputTillerClientKeyTask", "tiller-client-key", tillerClientKeyFile);
    var outputTillerSrverCertTask =
        createTerraformOutputTask(
            project, "outputTillerServerCertTask", "tiller-server-cert", tillerServerCertFile);
    var outputTillerServerKeyTask =
        createTerraformOutputTask(
            project, "outputTillerServerKeyTask", "tiller-server-key", tillerServerKeyFile);

    project
        .getTasks()
        .create(
            "helmInstall",
            HelmTask.class,
            t -> {
              t.setDescription("Installs the package onto the server.");

              var args = project.getObjects().listProperty(String.class);
              args.add("install");
              args.add("--namespace");
              args.add(config.getNamespace());
              args.add("--name");
              args.add(config.getName());
              args.add("--version");
              args.add(config.getVersion());
              args.add("-f");
              args.add(project.file("values.yaml").getAbsolutePath());
              args.add(config.getChart());
              t.addArgs(args);
            });
    project
        .getTasks()
        .create(
            "helmUpgrade",
            HelmTask.class,
            t -> {
              t.setDescription("Upgrades the configuration of the installed package.");

              var args = project.getObjects().listProperty(String.class);
              args.add("upgrade");
              args.add(config.getName());
              args.add("--version");
              args.add(config.getVersion());
              args.add("-f");
              args.add(project.file("values.yaml").getAbsolutePath());
              args.add(config.getChart());
              t.addArgs(args);
            });
    project
        .getTasks()
        .create(
            "helmDelete",
            HelmTask.class,
            t -> {
              t.setDescription("Deletes this package from the server.");

              var args = project.getObjects().listProperty(String.class);
              args.add("delete");
              args.add(config.getName());
              args.add("--purge");
              t.addArgs(args);
            });

    project
        .getTasks()
        .addRule(
            "Pattern: \"helm_<command>\": Executes a helm command.",
            taskName -> {
              if (!taskName.startsWith("helm_")) {
                return;
              }
              HelmTask task = project.getTasks().create(taskName, HelmTask.class);
              List<String> tokens = Splitter.on('_').splitToList(taskName);
              var args = project.getObjects().listProperty(String.class);
              args.set(Iterables.skip(tokens, 1));
              task.addArgs(args);
            });

    var helmTillerInit =
        project
            .getTasks()
            .create(
                "helmTillerInit",
                HelmTask.class,
                t -> {
                  t.setDescription("Initializes or upgrades tiller, helm's server-side component.");

                  t.dependsOn(outputTillerCaCertTask);
                  t.dependsOn(outputTillerSrverCertTask);
                  t.dependsOn(outputTillerServerKeyTask);

                  var args = project.getObjects().listProperty(String.class);
                  args.add("init");
                  args.add("--wait");
                  args.add("--history-max=5");
                  args.add("--service-account=tiller");
                  args.add("--tiller-tls");
                  args.add("--tiller-tls-verify");
                  args.add("--tiller-tls-cert=" + tillerServerCertFile.getAbsolutePath());
                  args.add("--tiller-tls-key=" + tillerServerKeyFile.getAbsolutePath());
                  args.add("--tls-ca-cert=" + tillerCaCertFile.getAbsolutePath());
                  args.add("--upgrade");
                  t.addArgs(args);
                });

    var helmClientInit =
        project
            .getTasks()
            .create(
                "helmClientInit",
                HelmTask.class,
                t -> {
                  t.setDescription("Prepares the helm client for execution.");

                  var args = project.getObjects().listProperty(String.class);
                  args.add("init");
                  args.add("--client-only");
                  t.addArgs(args);
                });

    project
        .getTasks()
        .withType(
            HelmTask.class,
            t -> {
              t.dependsOn(":gcloudDownloadHelm");

              if (t.getPath().equals(helmTillerInit.getPath())
                  || t.getPath().equals(helmClientInit.getPath())) {
                return;
              }

              if (!t.getPath().equals(helmClientInit.getPath())) {
                t.dependsOn(helmClientInit);
              }

              t.dependsOn(outputTillerCaCertTask);
              t.dependsOn(outputTillerClientCertTask);
              t.dependsOn(outputTillerClientKeyTask);

              var args = project.getObjects().listProperty(String.class);
              args.add("--tls");
              args.add("--tls-cert=" + tillerClientCertFile.getAbsolutePath());
              args.add("--tls-key=" + tillerClientKeyFile.getAbsolutePath());
              args.add("--tls-ca-cert=" + tillerCaCertFile.getAbsolutePath());
              t.addArgs(args);
            });
  }

  private static Task createTerraformOutputTask(
      Project project, String taskName, String outputName, File outputFile) {
    var terraformProject = project.getRootProject().findProject(":cluster:terraform");
    checkNotNull(terraformProject);
    try {
      return terraformProject.getTasks().getByName(taskName);
    } catch (UnknownTaskException e) {
      // fall through
    }
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
