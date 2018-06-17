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

package org.curioswitch.gradle.plugins.terraform;

import com.google.common.collect.ImmutableList;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.curioswitch.gradle.plugins.terraform.tasks.ConvertConfigsToJsonTask;
import org.curioswitch.gradle.plugins.terraform.tasks.TerraformTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;

public class TerraformPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(BasePlugin.class);

    String convertedConfigsPath = project.getBuildDir().toPath().resolve("terraform").toAbsolutePath().toString();
    Path plansPath = project.getProjectDir().toPath().resolve("plans");

    var convertConfigs =
        project.getTasks().create(ConvertConfigsToJsonTask.NAME, ConvertConfigsToJsonTask.class);
    convertConfigs.dependsOn(BasePlugin.CLEAN_TASK_NAME);

    var terraformInit =
        project
            .getTasks()
            .create(
                "terraformInit",
                TerraformTask.class,
                t -> {
                  t.setArgs(ImmutableList.of("init", "-input=false", convertedConfigsPath));
                  t.dependsOn(convertConfigs);
                });

    var terraformPlan =
        project
            .getTasks()
            .create(
                "terraformPlan",
                TerraformTask.class,
                t -> {
                  t.doFirst(unused -> project.mkdir(plansPath));
                  t.setArgs(ImmutableList.of("plan", "-out=" + plansPath.resolve("tfplan").toString(), "-input=false", "-no-color", convertedConfigsPath));
                  t.dependsOn(convertConfigs, terraformInit);
                  t.setExecCustomizer(exec -> {
                    try {
                      exec.setStandardOutput(new FileOutputStream(project.file(plansPath.resolve("plan.txt"))));
                    } catch (FileNotFoundException e) {
                      throw new UncheckedIOException("Could not write plan.", e);
                    }
                  });
                });

    var terraformApply = project.getTasks().create("terraformApply", TerraformTask.class, t -> {
      t.setArgs(ImmutableList.of("apply", "-input=false", plansPath.resolve("tfplan").toString()));
    });
  }
}
