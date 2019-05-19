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

package org.curioswitch.gradle.plugins.ci;

import static org.curioswitch.gradle.plugins.ci.CurioGenericCiPlugin.CI_STATE_PROPERTY;
import static org.curioswitch.gradle.plugins.ci.CurioGenericCiPlugin.COMMON_RELEASE_BRANCH_ENV_VARS;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.curioswitch.common.helpers.immutables.CurioStyle;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.immutables.value.Value.Immutable;

/** Contains state about the current build on a CI if it is running on CI. */
@Immutable
@CurioStyle
public interface CiState {

  static CiState createAndAdd(Project project) {
    String branch = getCiBranchOrTag();
    String revisionId =
        Strings.nullToEmpty(
            (String) project.getRootProject().findProperty("curiostack.revisionId"));

    boolean isMasterBuild = "true".equals(System.getenv("CI_MASTER"));
    boolean isReleaseBuild = branch.startsWith("RELEASE_");

    var revisionTags = new ImmutableList.Builder<String>();
    if (!revisionId.isEmpty()) {
      revisionTags.add(revisionId);
    }
    if (isReleaseBuild) {
      revisionTags.add(branch);
    }
    if (isMasterBuild) {
      revisionTags.add("latest");
    }

    CiState state =
        ImmutableCiState.builder()
            .isCi("true".equals(System.getenv("CI")) || "true".equals(project.findProperty("ci")))
            .isMasterBuild(isMasterBuild)
            .isReleaseBuild(isReleaseBuild)
            .isLocalBuild(System.getenv("CI") == null)
            .revisionId(revisionId)
            .branch(branch)
            .revisionTags(revisionTags.build())
            .build();

    project.getExtensions().getByType(ExtraPropertiesExtension.class).set(CI_STATE_PROPERTY, state);

    return state;
  }

  /** Returns whether this build is happening on CI or mimics the behavior of CI. */
  boolean isCi();

  /** Returns whether this is a master build on CI. */
  boolean isMasterBuild();

  /** Returns whether this is a release build on CI. */
  boolean isReleaseBuild();

  /**
   * Returns whether this is a local build on a developer's machine. This is true even if it is
   * mimicing the behavior of CI locally.
   */
  boolean isLocalBuild();

  /**
   * Returns the revision ID for this build (e.g., git commit) if it is on CI, or empty otherwise.
   */
  String getRevisionId();

  /** Returns the branch for this build if it is on CI or empty otherwise. */
  String getBranch();

  /**
   * Returns tags to associate with this build, usually used when tagging docker images for
   * deployment.
   */
  List<String> getRevisionTags();

  private static String getCiBranchOrTag() {
    // Not quite "generic" but should satisfy 99% of cases.
    for (String key : COMMON_RELEASE_BRANCH_ENV_VARS) {
      String branch = System.getenv(key);
      if (!Strings.isNullOrEmpty(branch)) {
        return branch;
      }
    }
    String jenkinsBranch = System.getenv("GIT_BRANCH");
    if (!Strings.isNullOrEmpty(jenkinsBranch)) {
      // Usually has remote too
      int slashIndex = jenkinsBranch.indexOf('/');
      return slashIndex >= 0 ? jenkinsBranch.substring(slashIndex + 1) : jenkinsBranch;
    }
    return "";
  }
}
