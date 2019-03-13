/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class UpdateProjectSettingsTask extends DefaultTask {

  @TaskAction
  public void exec() throws IOException {
    List<Path> buildFiles = new ArrayList<>();

    Files.walkFileTree(
        getProject().getRootDir().toPath(),
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (dir.endsWith("node_modules") || dir.endsWith("build")) {
              return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Path relativePath = getProject().getRootDir().toPath().relativize(file);

            if (relativePath.toString().equals("build.gradle")) {
              return FileVisitResult.CONTINUE;
            }

            String filename = relativePath.getFileName().toString();

            if (filename.equals("build.gradle") || filename.equals("build.gradle.kts")) {
              buildFiles.add(relativePath);
              return FileVisitResult.SKIP_SIBLINGS;
            }
            return FileVisitResult.CONTINUE;
          }
        });

    List<String> projectPaths =
        buildFiles.stream()
            .map(
                path ->
                    Streams.stream(path.getParent())
                        .map(Path::toString)
                        .collect(Collectors.joining(":", ":", "")))
            .sorted()
            .collect(toImmutableList());

    List<String> newIncludes = new ArrayList<>();
    newIncludes.add("// curio-auto-generated DO NOT MANUALLY EDIT");
    projectPaths.stream().map(path -> "include(\"" + path + "\")").forEach(newIncludes::add);
    newIncludes.add("// curio-auto-generated DO NOT MANUALLY EDIT\n");

    Path projectSettings = getProject().file("project.settings.gradle.kts").toPath();
    if (!Files.exists(projectSettings)) {
      Files.writeString(
          projectSettings,
          String.join(System.lineSeparator(), newIncludes) + System.lineSeparator());
    } else {
      var lines = Files.readAllLines(projectSettings);
      var newLines = new ArrayList<String>();
      boolean skipping = false;
      for (var line : lines) {
        if (skipping) {
          if (line.startsWith("// curio-auto-generated")) {
            skipping = false;
          }
        } else {
          if (line.startsWith("// curio-auto-generated")) {
            skipping = true;
            newLines.addAll(newIncludes);
          } else {
            newLines.add(line);
          }
        }
      }
      Files.writeString(projectSettings, String.join(System.lineSeparator(), newLines));
    }
  }
}
