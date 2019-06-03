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

import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.SoftAssertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;

/** {@link AbstractObjectAssert} for assertions on {@link BuildResult}. */
public class BuildResultAssert extends AbstractObjectAssert<BuildResultAssert, BuildResult> {

  BuildResultAssert(BuildResult buildResult) {
    super(buildResult, BuildResultAssert.class);
  }

  /** Asserts that the {@code tasks} succeeded during the build. */
  public BuildResultAssert tasksDidSucceed(String... tasks) {
    return tasksDidSucceed(ImmutableList.copyOf(tasks));
  }

  /** Asserts that the {@code tasks} succeeded during the build. */
  public BuildResultAssert tasksDidSucceed(Iterable<String> tasks) {
    tasksDidRun(tasks);
    SoftAssertions.assertSoftly(
        softly -> {
          for (var task : tasks) {
            BuildTask result = actual.task(task);
            softly
                .assertThat(result.getOutcome())
                .as("Task %s did not succeed", task)
                .isEqualTo(TaskOutcome.SUCCESS);
          }
        });
    return this;
  }

  /** Asserts that the {@code tasks} ran during the build. */
  public BuildResultAssert tasksDidRun(String... tasks) {
    return tasksDidRun(ImmutableList.copyOf(tasks));
  }

  /** Asserts that the {@code tasks} succeeded during the build. */
  public BuildResultAssert tasksDidRun(Iterable<String> tasks) {
    SoftAssertions.assertSoftly(
        softly -> {
          for (var task : tasks) {
            BuildTask result = actual.task(task);
            softly.assertThat(result).as("Task %s did not run", task).isNotNull();
          }
        });
    return this;
  }

  /** Asserts that the {@code tasks} ran during the build. */
  public BuildResultAssert tasksDidNotRun(String... tasks) {
    return tasksDidNotRun(ImmutableList.copyOf(tasks));
  }

  /** Asserts that the {@code tasks} succeeded during the build. */
  public BuildResultAssert tasksDidNotRun(Iterable<String> tasks) {
    SoftAssertions.assertSoftly(
        softly -> {
          for (var task : tasks) {
            BuildTask result = actual.task(task);
            softly.assertThat(result).as("Task %s did not run", task).isNull();
          }
        });
    return this;
  }

  /** Asserts that the build result has output that contains {@code content}. */
  public BuildResultAssert outputContains(String content) {
    assertThat(actual.getOutput()).contains(content);
    return this;
  }
}
