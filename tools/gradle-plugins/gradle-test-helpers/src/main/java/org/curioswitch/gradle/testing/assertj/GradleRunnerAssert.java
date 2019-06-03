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

package org.curioswitch.gradle.testing.assertj;

import org.assertj.core.api.AbstractAssert;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.gradle.testkit.runner.UnexpectedBuildSuccess;

/**
 * A {@link AbstractAssert} for asserting the result of a Gradle build executed with {@link
 * GradleRunner}.
 */
public class GradleRunnerAssert extends AbstractAssert<GradleRunnerAssert, GradleRunner> {

  GradleRunnerAssert(GradleRunner runner) {
    super(runner, GradleRunnerAssert.class);
  }

  /** Executes the build for the current runner and asserts that it succeeds. */
  public BuildResultAssert builds() {
    try {
      return new BuildResultAssert(actual.build());
    } catch (UnexpectedBuildFailure e) {
      failWithMessage(
          "Build expected to succeed, but failed with output:%s", e.getBuildResult().getOutput());
      // Can't reach here.
      throw new Error();
    }
  }

  /** Executes the build for the current runner and asserts that it fails. */
  public BuildResultAssert fails() {
    try {
      return new BuildResultAssert(actual.buildAndFail());
    } catch (UnexpectedBuildSuccess e) {
      failWithMessage(
          "Build expected to fail, but succeeded with output:%s", e.getBuildResult().getOutput());
      // Can't reach here.
      throw new Error();
    }
  }
}
