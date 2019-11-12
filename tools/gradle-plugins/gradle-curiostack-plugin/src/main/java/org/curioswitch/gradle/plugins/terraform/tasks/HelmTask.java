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
package org.curioswitch.gradle.plugins.terraform.tasks;

import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.curioswitch.gradle.plugins.gcloud.util.PlatformHelper;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.TaskAction;

public class HelmTask extends DefaultTask {

  private final ListProperty<String> args;

  public HelmTask() {
    setGroup("Helm");

    args = getProject().getObjects().listProperty(String.class).empty();

    args.add("--tiller-namespace");
    args.add("tiller-prod");
  }

  public HelmTask args(ListProperty<String> args) {
    this.args.addAll(args);
    return this;
  }

  public HelmTask args(String... args) {
    this.args.addAll(args);
    return this;
  }

  @TaskAction
  void exec() {
    var project = getProject();
    project.exec(
        exec -> {
          exec.executable(
              CommandUtil.getCuriostackDir(project)
                  .resolve("helm")
                  .resolve(ToolDependencies.getHelmVersion(project))
                  .resolve(new PlatformHelper().getOsName() + "-amd64")
                  .resolve("helm"));
          exec.args(args.get());
          exec.environment("HELM_HOME", getProject().file("build/helm").getAbsolutePath());
        });
  }
}
