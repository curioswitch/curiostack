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

package org.curioswitch.gradle.plugins.curiostack.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class SetupGitHooks extends DefaultTask {

  private static final String HOOK_FILE =
      "#!/bin/sh\n\n"
          + "echo 'Running pre-push check of affected targets. If you want to skip this, run "
          + "git push --no-verify instead.'\n"
          + "./gradlew continuousBuild -Pci=true";

  public SetupGitHooks() {
    getOutputs().file(".git/hooks/pre-push");
  }

  @TaskAction
  public void exec() {
    try {
      Files.write(
              Paths.get(".git", "hooks", "pre-push"), HOOK_FILE.getBytes(StandardCharsets.UTF_8))
          .toFile()
          .setExecutable(true);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write pre-push hook.", e);
    }
  }
}
