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

package org.curioswitch.gradle.plugins.terraform.tasks;

import static com.google.common.io.Files.getNameWithoutExtension;
import static java.nio.file.Files.createDirectories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;

public class ConvertConfigsToJsonTask extends DefaultTask {

  public static final String NAME = "terraformConvertConfigs";

  private static final ObjectMapper YAML_READER = new ObjectMapper(new YAMLFactory());
  private static final ObjectMapper JSON_WRITER =
      new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

  private final WorkerExecutor workerExecutor;
  private final Path outputDir;

  @Inject
  public ConvertConfigsToJsonTask(WorkerExecutor workerExecutor) {
    this.workerExecutor = workerExecutor;

    outputDir = getProject().getBuildDir().toPath().resolve("terraform");
  }

  @InputFiles
  public Iterable<File> getInputFiles() {
    return getProject()
        .fileTree(
            getProject().getProjectDir(),
            files ->
                files.include("*.tf").include("*.tf.yaml").include("*.tf.yml").include("*/**/*"));
  }

  @OutputDirectory
  public Path getOutputDir() {
    return outputDir;
  }

  @TaskAction
  void exec() {
    var project = getProject();
    for (var file : getInputFiles()) {
      Path path = file.toPath();

      if (!path.toString().endsWith(".tf.yml") && !path.toString().endsWith(".tf.yaml")) {
        // Copy non-config files (usually templates) as is.
        project.copy(
            copy -> {
              copy.from(path);
              copy.into(
                  outputDir.resolve(
                      project.getProjectDir().toPath().relativize(file.toPath()).getParent()));
            });
        continue;
      }

      String jsonFilename = getNameWithoutExtension(path.getFileName().toString()) + ".json";

      Path relativePath = getProject().getProjectDir().toPath().relativize(path);
      final Path outPath;
      if (relativePath.getNameCount() > 1 && !relativePath.startsWith("modules")) {
        String filenamePrefix = "";
        for (int i = 0; i < relativePath.getNameCount() - 1; i++) {
          filenamePrefix += relativePath.getName(i).toFile() + "-";
        }
        outPath = outputDir.resolve(filenamePrefix + jsonFilename);
      } else {
        outPath =
            outputDir
                .resolve(project.getProjectDir().toPath().relativize(file.toPath()))
                .resolveSibling(jsonFilename);
      }

      try {
        createDirectories(outPath.getParent());
      } catch (IOException e) {
        throw new UncheckedIOException("Could not create output directory.", e);
      }
      workerExecutor.submit(
          ConvertToJson.class,
          config -> {
            config.setIsolationMode(IsolationMode.NONE);
            config.params(file, outPath.toFile());
          });
    }
  }

  public static class ConvertToJson implements Runnable {

    private final File sourceFile;
    private final File outFile;

    @Inject
    public ConvertToJson(File sourceFile, File outFile) {
      this.sourceFile = sourceFile;
      this.outFile = outFile;
    }

    @Override
    public void run() {
      JsonNode contents;
      try {
        contents = YAML_READER.readTree(sourceFile);
      } catch (IOException e) {
        throw new UncheckedIOException("Could not read config yaml.", e);
      }
      try {
        JSON_WRITER.writeValue(outFile, contents);
      } catch (IOException e) {
        throw new UncheckedIOException("Could not write config json.", e);
      }
    }
  }
}
