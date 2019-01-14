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

import org.curioswitch.gradle.plugins.nodejs.NodePlugin;
import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask;
import org.curioswitch.gradle.plugins.staticsite.StaticSiteExtension.SiteProject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;

public class StaticSitePlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(NodePlugin.class);

    var config = StaticSiteExtension.create(project);

    var mergeSite = project.getTasks().register("mergeSite", Copy.class, t -> {
      t.from("src");
      t.into("build/site");

      for (SiteProject site : config.getSites().get()) {
        t.dependsOn(site.getProject().getTasks().named("assemble"));
        t.from(site.getBuildDir(), copy -> copy.into(site.getOutputSubDir()));
      }
    });

    var assemble = project.getTasks().named("assemble");
    assemble.configure(t -> t.dependsOn(mergeSite));

    var yarn = project.getRootProject().getTasks().named("yarn");
    project.getTasks().register("deploy", NodeTask.class, t -> {
      t.dependsOn(yarn, assemble);
      t.args("run", "firebase", "deploy");
    });


    project.getTasks().register("preview", NodeTask.class, t -> {
      t.dependsOn(yarn, assemble);
      t.args("run", "superstatic", "--port=8080");
    });
  }
}
