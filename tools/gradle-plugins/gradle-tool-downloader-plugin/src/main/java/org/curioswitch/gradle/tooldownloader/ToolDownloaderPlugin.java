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

package org.curioswitch.gradle.tooldownloader;

import com.google.common.base.Ascii;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.tasks.DownloadToolTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ToolDownloaderPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    var toolManager = new DownloadedToolManager(project);
    var platformHelper = new PlatformHelper();

    var extensionContainer =
        project.container(
            ModifiableToolDownloaderExtension.class,
            name -> ToolDownloaderExtension.create(name, project));
    project.getExtensions().add("tools", extensionContainer);

    extensionContainer.configureEach(
        tool -> {
          toolManager.register(tool.getName(), tool.getVersion());

          String capitalized =
              Ascii.toUpperCase(tool.getName().charAt(0)) + tool.getName().substring(1);
          project
              .getTasks()
              .register(
                  "toolsDownload" + capitalized,
                  DownloadToolTask.class,
                  tool,
                  platformHelper,
                  toolManager);
        });
  }
}
