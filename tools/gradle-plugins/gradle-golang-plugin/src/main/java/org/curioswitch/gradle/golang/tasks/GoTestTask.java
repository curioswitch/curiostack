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

package org.curioswitch.gradle.golang.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.curioswitch.gradle.golang.GoExecUtil;
import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;
import org.gradle.workers.WorkerExecutor;

public class GoTestTask extends GoTask {

  private final Property<Boolean> coverage;
  private final List<Action<ExecSpec>> execCustomizers;

  @Inject
  public GoTestTask(WorkerExecutor executor) {
    super(executor);

    var objects = getProject().getObjects();

    coverage = objects.property(Boolean.class).value(false);
    execCustomizers = new ArrayList<>();
  }

  public GoTestTask coverage(boolean coverage) {
    this.coverage.set(coverage);
    return this;
  }

  public GoTestTask coverage(Provider<Boolean> coverage) {
    this.coverage.set(coverage);
    return this;
  }

  @Override
  public GoTestTask execCustomizer(Action<ExecSpec> execCustomizer) {
    this.execCustomizers.add(checkNotNull(execCustomizer, "execCustomizer"));
    return this;
  }

  @TaskAction
  @Override
  public void exec() throws Exception {
    boolean coverage = this.coverage.get();
    Path coverageFile = getProject().file("coverage.txt").toPath();
    if (coverage) {
      Files.writeString(coverageFile, "");
    }

    ByteArrayOutputStream listOutputStream = new ByteArrayOutputStream();

    getProject()
        .exec(
            exec -> {
              GoExecUtil.goExec(exec, getProject(), "go", ImmutableList.of("list", "./..."));

              exec.setStandardOutput(listOutputStream);

              execCustomizers.forEach(c -> c.execute(exec));
            });

    String packageList = listOutputStream.toString(StandardCharsets.UTF_8);

    packageList
        .lines()
        .filter(s -> !s.contains("vendor"))
        .forEach(
            pkg -> {
              var args = new ImmutableList.Builder<String>();
              args.add("test");
              if (coverage) {
                args.add("-coverprofile=profile.out", "-covermode=atomic");
              }
              args.add(pkg);
              getProject()
                  .exec(
                      exec -> {
                        GoExecUtil.goExec(exec, getProject(), "go", args.build());
                        execCustomizers.forEach(c -> c.execute(exec));
                      });

              if (coverage) {
                Path profile = getProject().file("profile.out").toPath();
                if (Files.exists(profile)) {
                  try {
                    Files.write(
                        coverageFile, Files.readAllBytes(profile), StandardOpenOption.APPEND);
                    Files.delete(profile);
                  } catch (IOException e) {
                    throw new UncheckedIOException("Could not read or delete profile.", e);
                  }
                }
              }
            });
  }
}
