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

package org.curioswitch.gradle.plugins.testutil;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResourceProjects {

  public static void copyProjectFromResources(String resourcePath, Path projectDir)
      throws Exception {
    var resourceDir = Paths.get(Resources.getResource(resourcePath).toURI());
    try (var stream = Files.walk(resourceDir)) {
      stream
          .filter(path -> !path.equals(resourceDir))
          .forEach(
              path -> {
                var destPath = projectDir.resolve(resourceDir.relativize(path));
                try {
                  if (Files.isDirectory(path)) {
                    Files.createDirectory(destPath);
                  } else {
                    Files.copy(path, destPath);
                  }
                } catch (IOException e) {
                  throw new UncheckedIOException("Could not copy test project file.", e);
                }
              });
    }
  }

  private ResourceProjects() {}
}
