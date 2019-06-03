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

package org.curioswitch.gradle.testing;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Utilities for creating temporary folders under the build directory of a Gradle project. Gradle
 * TestKit and JUnit5 temporary directories tend to have issues, and since we know when testing a
 * Gradle plugin it is always in the context of a Gradle project, we instead create them in the
 * build directory where they can be cleaned outside the context of the test - the test will not
 * attempt to delete the files.
 *
 * <p>These utilities use the {@code -Dorg.curioswitch.curiostack.testing.buildDir} system property
 * to know where the current project's build directory is. Add this property to your build by adding
 * something like the following to your build script.
 *
 * <pre>{@code
 * tasks.withType(Test::class) {
 *     jvmArgs("-Dorg.curioswitch.curiostack.testing.buildDir=${buildDir}")
 * }
 * }</pre>
 */
public final class GradleTempDirectories {

  /**
   * Returns the {@link Path} pointing to a newly created directory under the current Gradle
   * project's build directory starting with {@code prefix}.
   */
  public static Path create(String prefix) {
    String buildDir = System.getProperty("org.curioswitch.curiostack.testing.buildDir", "");
    checkState(
        !buildDir.isBlank(),
        "org.curioswitch.curiostack.testing.buildDir must be specified "
            + "to create a Gradle temp directory. Did you add the directive to your build script?");
    var tempDir = Paths.get(buildDir).resolve("tempdirs").resolve(prefix + "-" + UUID.randomUUID());
    try {
      return Files.createDirectories(tempDir);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not create temp directory.", e);
    }
  }

  private GradleTempDirectories() {}
}
