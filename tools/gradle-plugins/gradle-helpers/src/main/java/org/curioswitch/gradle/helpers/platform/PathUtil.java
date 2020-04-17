/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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
package org.curioswitch.gradle.helpers.platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Utilities for working with file paths. */
public final class PathUtil {

  /**
   * Returns a {@link String} representation of the {@link Path} that can be included in a bash
   * invocation.
   */
  public static String toBashString(Path path) {
    return toBashString(path.toAbsolutePath().toString());
  }

  /**
   * Returns a {@link String} representation of the {@link String} that can be included in a bash
   * invocation. The {@code path} is often an interpolation.
   */
  public static String toBashString(String path) {
    var helper = new PlatformHelper();
    if (helper.getOs() != OperatingSystem.WINDOWS) {
      return path;
    }
    if (path.contains("$")) {
      // Path is an interpolation, only option is to delegate to cygwin
      return "$(cygpath " + path + ")";
    }
    path = path.replace('\\', '/');
    int colonIndex = path.indexOf(':');
    if (colonIndex < 0) {
      return path;
    }
    return '/' + path.substring(0, colonIndex) + path.substring(colonIndex + 1);
  }

  /**
   * Returns the name appended with a platform specific exe extension. This currently just adds .exe
   * to the name on Windows.
   */
  public static String getExeName(String name) {
    var helper = new PlatformHelper();
    if (helper.getOs() == OperatingSystem.WINDOWS) {
      return name + ".exe";
    } else {
      return name;
    }
  }

  /**
   * Returns a path that can be used to execute bash by Gradle, taking into account OS-specific
   * conventions.
   */
  public static String getBashExecutable() {
    String bashExecutable = System.getenv("MSYS_BASH_PATH");
    if (bashExecutable != null) {
      return bashExecutable;
    }
    // Optimistically try the default msys location if env variable isn't set (e.g., IntelliJ).
    bashExecutable = "C:\\tools\\msys64\\usr\\bin\\bash.exe";
    if (Files.exists(Paths.get(bashExecutable))) {
      return bashExecutable;
    }
    // This is usually Git bash on CI, but WSL bash on a desktop machine which will not work.
    // TODO(choko): Find a more robust way of doing this.
    return "bash";
  }

  private PathUtil() {}
}
