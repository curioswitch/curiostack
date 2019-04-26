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

package org.curioswitch.gradle.release;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ReleasePlugin implements Plugin<Project> {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  @Override
  public void apply(Project project) {
    if (project.getParent() != null) {
      throw new IllegalStateException(
          "gradle-release-plugin can only be applied to the root project.");
    }

    if (!project.hasProperty("version") || "unspecified".equals(project.findProperty("version"))) {
      String version = determineVersion(project);
      project.setProperty("version", version);

      project.subprojects(
          proj -> {
            if (!proj.hasProperty("version")
                || "unspecified".equals(proj.findProperty("version"))) {
              proj.setProperty("version", version);
            }
          });

      project.getLogger().quiet("Automatically determined version: " + version);
    }
  }

  private static String determineVersion(Project project) {
    String revisionId;
    if (project.hasProperty("curiostack.revisionId")) {
      revisionId = (String) project.property("curiostack.revisionId");
    } else if (System.getenv().containsKey("REVISION_ID")) {
      revisionId = System.getenv("REVISION_ID");
    } else if (!Strings.isNullOrEmpty(System.getenv().get("TAG_NAME"))) {
      revisionId = System.getenv("TAG_NAME");
    } else if (!Strings.isNullOrEmpty(System.getenv().get("BRANCH_NAME"))) {
      revisionId = System.getenv("BRANCH_NAME");
    } else {
      try (Git git = Git.open(project.getRootDir())) {
        revisionId = git.getRepository().resolve(Constants.HEAD).getName().substring(0, 12);
      } catch (IOException e) {
        revisionId = "unknown";
      }
    }

    checkNotNull(revisionId);

    boolean isRelease = "true".equals(project.findProperty("curiostack.release"));

    if (isRelease) {
      return revisionId;
    } else {
      return "0.0.0-"
          + TIMESTAMP_FORMATTER.format(LocalDateTime.now(ZoneOffset.UTC))
          + "-"
          + revisionId;
    }
  }
}
