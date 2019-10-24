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

package org.curioswitch.gradle.plugins.curiostack;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import org.curioswitch.gradle.plugins.gcloud.GcloudBuildCachePlugin;
import org.curioswitch.gradle.plugins.gcloud.buildcache.CloudStorageBuildCache;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.Settings;
import org.gradle.api.model.ObjectFactory;

public class CuriostackPlugin implements Plugin<Settings> {

  private final ObjectFactory objects;

  @Inject
  public CuriostackPlugin(ObjectFactory objects) {
    this.objects = checkNotNull(objects, "objects");
  }

  @Override
  public void apply(Settings settings) {
    var config = CuriostackExtension.createAndAdd(settings, objects);

    var pluginManagement = settings.getPluginManagement();
    pluginManagement
        .getResolutionStrategy()
        .eachPlugin(
            plugin -> {
              if ("org.curioswitch".equals(plugin.getRequested().getId().getNamespace())) {
                String curiostackVersion =
                    settings
                        .getStartParameter()
                        .getProjectProperties()
                        .get("org.curioswitch.curiostack.version");
                plugin.useModule(
                    "org.curioswitch.curiostack:gradle-curiostack-plugin:" + curiostackVersion);
              }
            });

    configureRepositories(pluginManagement.getRepositories());
    configureRepositories(settings.getBuildscript().getRepositories());

    settings.getPlugins().apply(GcloudBuildCachePlugin.class);

    var buildCache = settings.getBuildCache();
    buildCache.getLocal().setEnabled(!System.getenv().containsKey("CI"));
    buildCache.remote(
        CloudStorageBuildCache.class,
        remote -> {
          remote.setBucket(config.getBuildCacheBucket());
          remote.setPush(System.getenv().containsKey("CI_MASTER"));
        });

    settings.apply(c -> c.from("project.settings.gradle.kts"));

    settings
        .getGradle()
        .rootProject(project -> {
          configureRepositories(project.getBuildscript().getRepositories());
          project.getPlugins().apply(CuriostackRootPlugin.class);
        });
  }

  private static void configureRepositories(RepositoryHandler repositories) {
    repositories.jcenter();
    repositories.gradlePluginPortal();
    repositories.maven(maven -> maven.setUrl("https://dl.bintray.com/curioswitch/curiostack"));
    repositories.mavenLocal();
  }
}
