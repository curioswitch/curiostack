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
package org.curioswitch.gradle.tooldownloader.util;

import static com.google.common.base.Preconditions.checkNotNull;

import org.curioswitch.gradle.helpers.task.TaskUtil;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.tasks.DownloadToolTask;
import org.curioswitch.gradle.tooldownloader.tasks.SetupTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

/** Utilities for working with the configuration of a downloaded tool. */
public final class DownloadToolUtil {

  /**
   * Returns the {@link DownloadedToolManager} registered to the root project of this build. This is
   * the primary entry point for working with the downloaded tools.
   *
   * @param project A project in the build. The actually used project is the root project.
   */
  public static DownloadedToolManager getManager(Project project) {
    var toolManager =
        (DownloadedToolManager)
            project.getRootProject().getExtensions().getExtraProperties().get("toolManager");
    checkNotNull(toolManager, "toolManager not found. Did you apply the tool-downloader plugin?");
    return toolManager;
  }

  /**
   * Returns the {@link TaskProvider} for the download task for a tool. Most users should use {@link
   * #getSetupTask(Project, String)} which will also run any additional setup tasks.
   *
   * @param project A project in this build. The actually used project is the root project.
   * @param toolName The name of the tool.
   */
  public static TaskProvider<DownloadToolTask> getDownloadTask(Project project, String toolName) {
    return project
        .getRootProject()
        .getTasks()
        .withType(DownloadToolTask.class)
        .named("toolsDownload" + TaskUtil.toTaskSuffix(toolName));
  }

  /**
   * Returns the {@link TaskProvider} for the setup task for this a tool. Most {@link Task}s that
   * depend on a tool being present should declare a dependency on the returned {@link
   * TaskProvider}.
   *
   * @param project A project in this build. The actually used project is the root project.
   * @param toolName The name of the tool.
   */
  public static TaskProvider<? extends Task> getSetupTask(Project project, String toolName) {
    return project
        .getRootProject()
        .getTasks()
        .withType(SetupTask.class)
        .named("toolsSetup" + TaskUtil.toTaskSuffix(toolName));
  }

  private DownloadToolUtil() {}
}
