/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.plugins.gcloud;

import java.io.File;
import org.curioswitch.gradle.plugins.gcloud.util.PlatformHelper;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Immutable
@Style(
  visibility = ImplementationVisibility.PACKAGE,
  builderVisibility = BuilderVisibility.PACKAGE,
  defaultAsDefault = true
)
public interface PlatformConfig {

  static PlatformConfig fromExtension(ImmutableGcloudExtension config) {
    PlatformHelper platformHelper = new PlatformHelper();

    if (platformHelper.isWindows()) {
      config.gradleProject().getLogger().info("GCloud not supported on windows yet, skipping...");
    }

    String osName = platformHelper.getOsName();
    String osArch = platformHelper.getOsArch();

    return ImmutablePlatformConfig.builder()
        .dependency(
            "com.google:google-cloud-sdk:"
                + config.version()
                + ":"
                + osName
                + "-"
                + osArch
                + "@tar.gz")
        .sdkDir(
            new File(config.workDir(), "gcloud-" + config.version() + "-" + osName + "-" + osArch))
        .gcloudExec("gcloud")
        .build();
  }

  String gcloudExec();

  @Derived
  default File gcloudBinDir() {
    return new File(sdkDir(), "bin");
  }

  File sdkDir();

  String dependency();
}
