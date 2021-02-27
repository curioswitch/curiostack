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
package org.curioswitch.gradle.plugins.nodejs;

import com.google.common.base.Splitter;
import java.util.List;
import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Delete;

public class NodePlugin implements Plugin<Project> {

  private static final Splitter YARN_TASK_SPLITTER = Splitter.on('_');

  @Override
  public void apply(Project project) {
    project.getRootProject().getPlugins().apply(NodeSetupPlugin.class);
    project.getPlugins().apply(BasePlugin.class);

    project
        .getTasks()
        .withType(Delete.class)
        .named("clean")
        .configure(t -> t.delete("node_modules"));

    project
        .getTasks()
        .addRule(
            "Pattern: \"yarn_<command>\": Executes a Yarn command.",
            taskName -> {
              if (taskName.startsWith("yarn_")) {
                project
                    .getTasks()
                    .create(
                        taskName,
                        NodeTask.class,
                        t -> {
                          List<String> tokens = YARN_TASK_SPLITTER.splitToList(taskName);
                          t.args(tokens.subList(1, tokens.size()));
                        });
              }
            });
  }
}
