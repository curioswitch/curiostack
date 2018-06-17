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

package org.curioswitch.gradle.plugins.gcloud.tasks;

import javax.inject.Inject;
import org.curioswitch.gradle.plugins.curiostack.StandardDependencies;
import org.curioswitch.gradle.plugins.gcloud.util.PlatformHelper;
import org.curioswitch.gradle.plugins.shared.tasks.DownloadArchiveTask;

public class DownloadTerraformTask extends DownloadArchiveTask {

  public static final String NAME = "gcloudDownloadTerraform";

  private final PlatformHelper platformHelper;

  @Inject
  public DownloadTerraformTask(PlatformHelper platformHelper) {
    this.platformHelper = platformHelper;

    setBaseUrl("https://releases.hashicorp.com/");
    setArtifactPattern("[artifact]/[revision]/[artifact]_[revision]_[classifier].[ext]");
    setDependency(
        "com.hashicorp:terraform:"
            + StandardDependencies.TERRAFORM_VERSION
            + ":"
            + getClassifier()
            + "@zip");
  }

  private String getClassifier() {
    final String arch;
    switch (platformHelper.getOsArch()) {
      case "arm":
        arch = "arm";
        break;
      case "x86_64":
        arch = "amd64";
        break;
      case "x86":
        arch = "386";
        break;
      default:
        throw new IllegalStateException("Unsupported architecture.");
    }
    return platformHelper.getOsName() + "_" + arch;
  }
}
