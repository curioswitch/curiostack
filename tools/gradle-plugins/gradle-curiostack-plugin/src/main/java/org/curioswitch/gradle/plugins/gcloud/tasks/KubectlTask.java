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

package org.curioswitch.gradle.plugins.gcloud.tasks;

import java.io.File;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableGcloudExtension;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.TaskAction;

/** A {@link org.gradle.api.Task} that executes a kubectl command. */
public class KubectlTask extends DefaultTask {

  private static final String COMMAND = "kubectl";

  private final ListProperty<String> args;

  public KubectlTask() {
    setGroup("gcloud");

    args = getProject().getObjects().listProperty(String.class);
  }

  public KubectlTask setArgs(Iterable<String> args) {
    this.args.set(args);
    return this;
  }

  public KubectlTask setArgs(ListProperty<String> args) {
    this.args.set(args);
    return this;
  }

  @TaskAction
  public void exec() {
    ImmutableGcloudExtension config =
        getProject().getRootProject().getExtensions().getByType(GcloudExtension.class);

    String command = Os.isFamily(Os.FAMILY_WINDOWS) ? COMMAND + ".exe" : COMMAND;
    String executable = CommandUtil.getGcloudSdkBinDir(getProject()).resolve(command).toString();
    getProject()
        .exec(
            exec -> {
              exec.executable(executable);
              exec.args(args.get());
              if (config.download()) {
                exec.environment(
                    "PATH",
                    CommandUtil.getGcloudSdkBinDir(getProject())
                        + File.pathSeparator
                        + exec.getEnvironment().get("PATH"));
              }
              exec.setStandardInput(System.in);
            });
  }
}
