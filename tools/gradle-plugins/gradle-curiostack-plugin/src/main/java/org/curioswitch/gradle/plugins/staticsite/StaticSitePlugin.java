/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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

import com.google.common.collect.ImmutableList;
import org.curioswitch.gradle.plugins.ci.CurioGenericCiPlugin;
import org.curioswitch.gradle.plugins.gcloud.tasks.GcloudTask;
import org.curioswitch.gradle.plugins.nodejs.NodePlugin;
import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask;
import org.curioswitch.gradle.plugins.staticsite.StaticSiteExtension.SiteProject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public class StaticSitePlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(NodePlugin.class);

    var config = StaticSiteExtension.create(project);

    var mergeSite =
        project
            .getTasks()
            .register(
                "mergeSite",
                Copy.class,
                t -> {
                  t.from("src");
                  t.into("build/site");

                  for (SiteProject site : config.getSites().get()) {
                    site.getProject()
                        .getPlugins()
                        .withType(
                            LifecycleBasePlugin.class,
                            unused ->
                                t.dependsOn(
                                    site.getProject()
                                        .getTasks()
                                        .named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)));
                    t.from(site.getBuildDir(), copy -> copy.into(site.getOutputSubDir()));
                  }
                });

    var assemble = project.getTasks().named("assemble");
    assemble.configure(t -> t.dependsOn(mergeSite));

    var yarn = project.getRootProject().getTasks().named("yarn");

    var deployAlpha =
        project
            .getTasks()
            .register(
                "deployAlpha",
                GcloudTask.class,
                t -> {
                  t.dependsOn(assemble);
                  // TODO(choko): Remove this major hack - the command line has two --project flags
                  // and we're just lucky the later is used.
                  t.args(
                      config
                          .getAppEngineProject()
                          .map(
                              appEngineProject ->
                                  ImmutableList.of(
                                      "app", "deploy", "--project=" + appEngineProject)));
                });

    var deployProd =
        project
            .getTasks()
            .register(
                "deployProd",
                NodeTask.class,
                t -> {
                  t.dependsOn(yarn, assemble);
                  t.args(
                      config
                          .getFirebaseProject()
                          .map(
                              firebaseProject ->
                                  ImmutableList.of(
                                      "run", "firebase", "--project", firebaseProject, "deploy")));
                });

    project
        .getTasks()
        .register(
            "preview",
            NodeTask.class,
            t -> {
              t.dependsOn(yarn, assemble);
              t.args("run", "superstatic", "--port=8080");
            });

    CurioGenericCiPlugin.addToReleaseBuild(project, deployProd);

    project.afterEvaluate(
        unused -> {
          if (config.getAutoDeployAlpha().get()) {
            CurioGenericCiPlugin.addToMasterBuild(project, deployAlpha);
          }
        });
  }
}
