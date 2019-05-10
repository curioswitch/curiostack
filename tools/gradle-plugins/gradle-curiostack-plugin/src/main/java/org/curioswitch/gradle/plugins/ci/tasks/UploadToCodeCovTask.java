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

package org.curioswitch.gradle.plugins.ci.tasks;

import com.google.common.base.Splitter;
import com.linecorp.armeria.client.HttpClient;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.curioswitch.gradle.conda.exec.CondaExecUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public class UploadToCodeCovTask extends DefaultTask {

  private static final Splitter REMOTE_REF_SPLITTER = Splitter.on('/');

  @OutputFile
  public File getCodeCovReportFile() {
    return getProject().file("build/codecov-report.txt");
  }

  @TaskAction
  public void exec() throws Exception {
    var codeCovUploader =
        HttpClient.of("https://codecov.io").get("/bash").aggregate().join().content();
    File tempDir = getTemporaryDir();
    tempDir.createNewFile();

    Path codeCovScript = Paths.get(tempDir.getAbsolutePath(), "codecov.sh");

    Files.write(codeCovScript, codeCovUploader.array());

    codeCovScript.toFile().setExecutable(true);

    getProject()
        .exec(
            exec -> {
              exec.setCommandLine(
                  codeCovScript.toAbsolutePath().toString(),
                  "-d > ",
                  getCodeCovReportFile().getAbsolutePath());

              exec.setIgnoreExitValue(true);

              CondaExecUtil.condaExec(exec, getProject());
            });

    getProject()
        .exec(
            exec -> {
              // Ideally we would just upload the dump, but codecov seems to mix logging output and
              // the report when dumping to stdout which doesn't parse correctly. Luckilty the dump
              // is fast enough that it's not a big deal.
              exec.setCommandLine(codeCovScript.toAbsolutePath().toString());

              String prRemoteRef = System.getenv("GITHUB_PR_REMOTE_REF");
              if (prRemoteRef != null) {
                // Format is refs/pull/${pull.number}/head, just do a simple parse
                String prNumber = REMOTE_REF_SPLITTER.splitToList(prRemoteRef).get(2);
                exec.environment("VCS_PULL_REQUEST", prNumber);
              }

              String buildId = System.getenv("CLOUDBUILD_BUILD_ID");
              if (buildId != null) {
                exec.environment("CI_BUILD_ID", buildId);
              }

              exec.setIgnoreExitValue(true);

              CondaExecUtil.condaExec(exec, getProject());
            });
  }
}
