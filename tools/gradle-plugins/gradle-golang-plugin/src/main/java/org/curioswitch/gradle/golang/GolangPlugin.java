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

package org.curioswitch.gradle.golang;

import com.google.common.base.Splitter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.curioswitch.gradle.golang.tasks.GoTask;
import org.curioswitch.gradle.golang.tasks.GolangExtension;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.tasks.DownloadToolTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Rule;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public class GolangPlugin implements Plugin<Project> {

  private static final Splitter TASK_NAME_SPLITTER = Splitter.on('_');

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(BasePlugin.class);

    var golang = GolangExtension.createAndAdd(project);

    project
        .getRootProject()
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin ->
                plugin.registerToolIfAbsent(
                    "go",
                    tool -> {
                      tool.getVersion().set("1.11");
                      tool.getBaseUrl().set("https://dl.google.com/go/");
                      tool.getArtifactPattern().set("[artifact][revision].[classifier].[ext]");
                      tool.getPathSubDirs().add("go/bin");
                    }));

    project
        .getTasks()
        .addRule(
            new Rule() {
              @Override
              public String getDescription() {
                return "Pattern: \"go_<command>\": Executes a go command.";
              }

              @Override
              public void apply(String taskName) {
                if (taskName.startsWith("go_")) {
                  project
                      .getTasks()
                      .register(
                          taskName,
                          GoTask.class,
                          t -> {
                            List<String> tokens = TASK_NAME_SPLITTER.splitToList(taskName);
                            t.args(tokens.subList(1, tokens.size()));
                          });
                }
              }
            });

    project
        .getTasks()
        .withType(GoTask.class)
        .configureEach(
            t ->
                t.dependsOn(
                    project
                        .getRootProject()
                        .getTasks()
                        .withType(DownloadToolTask.class)
                        .named("toolsDownloadGo")));

    var checkFormat =
        project
            .getTasks()
            .register(
                "goCheck",
                GoTask.class,
                t -> {
                  t.command("gofmt");
                  t.args("-s", "-d", ".");
                  ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                  t.doLast(
                      unused -> {
                        if (stdOut.size() > 0) {
                          throw new GradleException(
                              "There were formatting violations. Run :goFormat to fix.\n"
                                  + stdOut.toString());
                        }
                      });
                  t.setExecCustomizer(exec -> exec.setStandardOutput(stdOut));
                });
    project
        .getTasks()
        .named(LifecycleBasePlugin.CHECK_TASK_NAME)
        .configure(t -> t.dependsOn(checkFormat));

    project
        .getTasks()
        .register(
            "goFormat",
            GoTask.class,
            t -> {
              t.command("gofmt");
              t.args("-s", "-w", ".");
            });
  }
}
