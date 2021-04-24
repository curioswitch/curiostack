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

package org.curioswitch.gradle.golang;

import org.curioswitch.gradle.conda.CondaBuildEnvPlugin;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;

public class GolangSetupPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(CondaBuildEnvPlugin.class);
    project
        .getPlugins()
        .withType(
            ToolDownloaderPlugin.class,
            plugin -> {
              plugin.registerToolIfAbsent(
                  "go",
                  tool -> {
                    tool.getVersion().set("1.16");
                    tool.getBaseUrl().set("https://dl.google.com/go/");
                    tool.getArtifactPattern().set("[artifact][revision].[classifier].[ext]");
                    tool.getPathSubDirs().add("go/bin");
                    tool.getAdditionalCachedDirs().add("gopath");
                  });
            });

    project
        .getExtensions()
        .getByType(ExtraPropertiesExtension.class)
        .set(
            "gopath",
            project
                .getGradle()
                .getGradleUserHomeDir()
                .toPath()
                .resolve("curiostack")
                .resolve("gopath"));
  }
}
