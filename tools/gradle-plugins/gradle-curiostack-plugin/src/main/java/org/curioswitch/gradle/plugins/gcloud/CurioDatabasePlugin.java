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

package org.curioswitch.gradle.plugins.gcloud;

import com.bmuschko.gradle.docker.DockerRemoteApiPlugin;
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage;
import com.bmuschko.gradle.docker.tasks.image.Dockerfile;
import com.google.common.collect.ImmutableList;
import org.curioswitch.gradle.plugins.gcloud.tasks.DeployDevDbPodTask;
import org.curioswitch.gradle.plugins.gcloud.tasks.GcloudTask;
import org.flywaydb.gradle.FlywayExtension;
import org.flywaydb.gradle.FlywayPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;

public class CurioDatabasePlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project
        .getExtensions()
        .create(ImmutableDatabaseExtension.NAME, DatabaseExtension.class, project);

    project.getPlugins().apply(BasePlugin.class);
    project.getPlugins().apply(DockerRemoteApiPlugin.class);
    project.getPlugins().apply(FlywayPlugin.class);

    project.afterEvaluate(CurioDatabasePlugin::addTasks);
  }

  private static void addTasks(Project project) {
    ImmutableDatabaseExtension config = project.getExtensions().getByType(DatabaseExtension.class);

    FlywayExtension flyway = project.getExtensions().getByType(FlywayExtension.class);
    flyway.user = "dbadmin";
    flyway.password = config.devAdminPassword();
    flyway.schemas = new String[] {config.dbName()};

    Dockerfile generateDevDbDockerfile =
        project.getTasks().create("generateDevDbDockerfile", Dockerfile.class);
    generateDevDbDockerfile.from("mysql:5.7");
    generateDevDbDockerfile.environmentVariable("MYSQL_DATABASE", config.dbName());
    generateDevDbDockerfile.environmentVariable("MYSQL_USER", config.devAdminUser());
    String devAdminPassword = config.devAdminPassword();
    if (devAdminPassword != null) {
      generateDevDbDockerfile.environmentVariable("MYSQL_PASSWORD", devAdminPassword);
    }
    generateDevDbDockerfile.environmentVariable("MYSQL_RANDOM_ROOT_PASSWORD", "yes");
    generateDevDbDockerfile.environmentVariable("MYSQL_ONETIME_PASSWORD", "yes");

    DockerBuildImage buildDevDbDockerImage =
        project.getTasks().create("buildDevDbDockerImage", DockerBuildImage.class);
    buildDevDbDockerImage.dependsOn(generateDevDbDockerfile);
    buildDevDbDockerImage.setInputDir(generateDevDbDockerfile.getDestFile().getParentFile());
    buildDevDbDockerImage.setTag(config.devDockerImageTag());

    GcloudTask pushDevDbDockerImage =
        project.getTasks().create("pushDevDbDockerImage", GcloudTask.class);
    pushDevDbDockerImage.dependsOn(buildDevDbDockerImage);
    pushDevDbDockerImage.setArgs(
        ImmutableList.of("docker", "--", "push", config.devDockerImageTag()));

    project
        .getTasks()
        .create("deployDevDb", DeployDevDbPodTask.class)
        .dependsOn(pushDevDbDockerImage);
  }
}
