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

import org.curioswitch.common.testing.assertj.CurioAssertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

/**
 * Standard {@link CurioAssertions} along with assertions for working with Gradle tests.
 */
public class CurioGradleAssertions extends CurioAssertions {

  /**
   * Returns a {@link GradleRunnerAssert} to assert the result of the executed build.
   */
  public static GradleRunnerAssert assertThat(GradleRunner runner) {
    return new GradleRunnerAssert(runner);
  }

  /**
   * Returns a {@link BuildResultAssert} to assert the provided {@code result}.
   */
  public static BuildResultAssert assertThat(BuildResult result) {
    return new BuildResultAssert(result);
  }

  private CurioGradleAssertions() {}
}
