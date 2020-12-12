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

package org.curioswitch.gradle.plugins.curiostack.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.adoptopenjdk.v3.api.AOV3Architecture;
import net.adoptopenjdk.v3.api.AOV3Error;
import net.adoptopenjdk.v3.api.AOV3Exception;
import net.adoptopenjdk.v3.api.AOV3ImageKind;
import net.adoptopenjdk.v3.api.AOV3JVMImplementation;
import net.adoptopenjdk.v3.api.AOV3ListBinaryAssetView;
import net.adoptopenjdk.v3.vanilla.AOV3Clients;
import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link org.gradle.api.Task} to update the Gradle wrapper to include functionality to
 * automatically download a JDK.
 */
public class UpdateGradleWrapperTask extends DefaultTask {

  @TaskAction
  public void exec() throws IOException {
    var rootProject = getProject().getRootProject();

    String releaseName = null;
    String urlLinux = null;
    String urlMac = null;
    String urlWindows = null;

    var clients = new AOV3Clients();
    final List<AOV3ListBinaryAssetView> releases;
    try (var client = clients.createClient()) {
      var errors = new ArrayList<AOV3Error>();
      releases =
          client
              .assetsForLatest(
                  errors::add,
                  new BigInteger(ToolDependencies.getOpenJdkVersion(rootProject)),
                  AOV3JVMImplementation.HOTSPOT)
              .execute();
    } catch (AOV3Exception | InterruptedException e) {
      throw new GradleException("Could not query for latest AdoptOpenJDK release.", e);
    }

    for (var release : releases) {
      releaseName = release.releaseName();
      var binary = release.binary();
      if (binary.architecture() != AOV3Architecture.X64
          || binary.imageType() != AOV3ImageKind.JDK) {
        continue;
      }
      switch (binary.operatingSystem()) {
        case LINUX:
          {
            urlLinux = binary.package_().link().toASCIIString();
            break;
          }
        case MAC:
          {
            urlMac = binary.package_().link().toASCIIString();
            break;
          }
        case WINDOWS:
          {
            urlWindows = binary.package_().link().toASCIIString();
            break;
          }
        default:
          // Ignore
      }
    }

    if (releaseName == null || urlLinux == null || urlMac == null || urlWindows == null) {
      throw new GradleException("Could not find OpenJDK packages: " + releases);
    }

    Map<String, String> templateVars =
        ImmutableMap.<String, String>builder()
            .put("dest_folder", "jdk-" + releaseName)
            .put("url_linux", urlLinux)
            .put("url_mac", urlMac)
            .put("url_windows", urlWindows)
            .put("dest_archive_name", "jdk-" + releaseName + ".tar.gz.or.zip")
            .put("version", releaseName)
            .build();

    Jinjava jinjava = new Jinjava(JinjavaConfig.newBuilder().withFailOnUnknownTokens(true).build());
    String rendered =
        jinjava.render(
            Resources.toString(
                Resources.getResource("curiostack/get-jdk.sh.tmpl"), StandardCharsets.UTF_8),
            templateVars);

    Files.writeString(
        rootProject.file("gradle/get-jdk.sh").toPath(), rendered, StandardCharsets.UTF_8);

    Files.writeString(rootProject.file(".gradle/jdk-release-name.txt").toPath(), releaseName);

    var gradlew = rootProject.file("gradlew").toPath();
    var gradleWrapperLines = Files.readAllLines(gradlew);
    if (gradleWrapperLines.stream().anyMatch(line -> line.contains(". ./gradle/get-jdk.sh"))) {
      return;
    }

    // First line is always shebang, skip it.
    int lineIndexAfterCopyright = 1;
    // Skip empty lines
    for (; lineIndexAfterCopyright < gradleWrapperLines.size(); lineIndexAfterCopyright++) {
      if (!gradleWrapperLines.get(lineIndexAfterCopyright).isEmpty()) {
        break;
      }
    }
    // Skip comment lines, they are all the license
    for (; lineIndexAfterCopyright < gradleWrapperLines.size(); lineIndexAfterCopyright++) {
      if (!gradleWrapperLines.get(lineIndexAfterCopyright).startsWith("#")) {
        break;
      }
    }
    // Found first empty line after license, insert our JDK download script and write it out.
    var linesWithGetJdk =
        ImmutableList.<String>builderWithExpectedSize(gradleWrapperLines.size() + 2);
    linesWithGetJdk.addAll(gradleWrapperLines.subList(0, lineIndexAfterCopyright));
    linesWithGetJdk.add("").add(". ./gradle/get-jdk.sh");
    linesWithGetJdk.addAll(
        gradleWrapperLines.subList(lineIndexAfterCopyright, gradleWrapperLines.size()));

    Files.writeString(gradlew, String.join("\n", linesWithGetJdk.build()));
  }
}
