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
package org.curioswitch.gradle.tooldownloader;

import static com.google.common.base.Preconditions.checkState;

import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.helpers.task.TaskUtil;
import org.curioswitch.gradle.tooldownloader.tasks.DownloadToolTask;
import org.curioswitch.gradle.tooldownloader.tasks.SetupTask;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;

public class ToolDownloaderPlugin implements Plugin<Project> {

  private NamedDomainObjectContainer<ToolDownloaderExtension> tools;
  private DownloadedToolManager toolManager;

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null,
        "gradle-tool-downloader-plugin can only be applied to the root project.");

    var platformHelper = new PlatformHelper();

    tools =
        project.container(
            ToolDownloaderExtension.class, name -> ToolDownloaderExtension.create(name, project));
    project.getExtensions().add("tools", tools);

    toolManager = new DownloadedToolManager(project, tools);

    project
        .getExtensions()
        .getByType(ExtraPropertiesExtension.class)
        .set("toolManager", toolManager);

    var downloadAll = project.getTasks().register("toolsDownloadAll");
    var setupAll = project.getTasks().register("toolsSetupAll");

    tools.configureEach(
        tool -> {
          var taskSuffix = TaskUtil.toTaskSuffix(tool.getName());
          var task =
              project
                  .getTasks()
                  .register(
                      "toolsDownload" + taskSuffix,
                      DownloadToolTask.class,
                      tool,
                      platformHelper,
                      toolManager);
          var setup =
              project
                  .getTasks()
                  .register("toolsSetup" + taskSuffix, SetupTask.class, tool.getName());
          setup.configure(t -> t.dependsOn(task));
          downloadAll.configure(t -> t.dependsOn(task));
          setupAll.configure(t -> t.dependsOn(setup));
        });
  }

  public ToolDownloaderPlugin registerToolIfAbsent(
      String name, Action<ToolDownloaderExtension> configurer) {
    var tool = tools.maybeCreate(name);
    if (tool.getVersion().getOrNull() == null) {
      configurer.execute(tool);
    }
    return this;
  }

  public DownloadedToolManager toolManager() {
    return toolManager;
  }

  public NamedDomainObjectContainer<ToolDownloaderExtension> tools() {
    return tools;
  }
}
