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

import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.process.ExecSpec;

public final class GoExecUtil {

  public static void goExec(ExecSpec exec, Project project, String command, Iterable<String> args) {
    var toolManager = DownloadedToolManager.get(project);

    exec.executable(toolManager.getBinDir("go").resolve(command));
    exec.args(args);
    exec.environment("GOROOT", toolManager.getToolDir("go").resolve("go"));
    exec.environment(
        "GOPATH", project.getExtensions().getByType(ExtraPropertiesExtension.class).get("gopath"));
    exec.environment("GOFLAGS", "-mod=readonly");

    toolManager.addAllToPath(exec);
  }

  private GoExecUtil() {}
}
