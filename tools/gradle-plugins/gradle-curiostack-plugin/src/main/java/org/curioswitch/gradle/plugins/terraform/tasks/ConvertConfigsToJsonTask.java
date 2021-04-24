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

package org.curioswitch.gradle.plugins.terraform.tasks;

import static com.google.common.io.Files.getNameWithoutExtension;
import static java.nio.file.Files.createDirectories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.loader.FileLocator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
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
    var jinJava = new Jinjava(JinjavaConfig.newBuilder().withTrimBlocks(true).build());
    try {
      jinJava.setResourceLocator(new FileLocator(getProject().getProjectDir()));
    } catch (FileNotFoundException e) {
      throw new IllegalStateException("Could not initialize Jinjava");
    }

    var project = getProject();
    for (var file : getInputFiles()) {
      Path path = file.toPath();

      if (!path.toString().endsWith(".tf.yml") && !path.toString().endsWith(".tf.yaml")) {
        var outDir =
            outputDir.resolve(project.getProjectDir().toPath().relativize(path.getParent()));

        if (path.toString().contains(".jinja.")) {
          var outPath = outDir.resolve(path.getFileName().toString().replace(".jinja.", "."));
          try {
            Files.createDirectories(outDir);
            Files.writeString(outPath, jinJava.render(Files.readString(path), ImmutableMap.of()));
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        } else {
          // Copy non-config files (usually templates) as is.
          project.copy(
              copy -> {
                copy.from(path);
                copy.into(outDir);
              });
        }
        continue;
      }

      String jsonFilename = getNameWithoutExtension(path.getFileName().toString()) + ".json";

      Path relativePath = getProject().getProjectDir().toPath().relativize(path);
      final Path outPath;
      if (relativePath.getNameCount() > 1) {
        if (!relativePath.startsWith("modules")) {
          outPath = outputDir.resolve(flattenFilename(relativePath, jsonFilename));
        } else {
          // Modules will always have a top-level directory where this should go.
          outPath =
              outputDir
                  .resolve(relativePath.subpath(0, 3))
                  .resolveSibling(flattenFilename(relativePath, jsonFilename));
        }
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
      workerExecutor
          .noIsolation()
          .submit(
              ConvertToJson.class,
              parameters -> {
                parameters.getProjectDirectory().set(project.getProjectDir());
                parameters.getSourceFile().set(file);
                parameters.getOutFile().set(outPath.toFile());
              });
    }
  }

  private static String flattenFilename(Path relativePath, String filename) {
    String filenamePrefix = "";
    for (int i = 0; i < relativePath.getNameCount() - 1; i++) {
      filenamePrefix += relativePath.getName(i).toFile() + "-";
    }
    return filenamePrefix + filename;
  }

  public abstract static class ConvertToJson implements WorkAction<ActionParameters> {

    @Override
    public void execute() {
      File sourceFile = getParameters().getSourceFile().get();
      File outFile = getParameters().getOutFile().get();

      final String fileContents;
      if (sourceFile.getName().contains(".jinja.")) {
        var jinJava = new Jinjava();
        try {
          jinJava.setResourceLocator(new FileLocator(getParameters().getProjectDirectory().get()));
        } catch (FileNotFoundException e) {
          throw new IllegalStateException("Could not initialize Jinjava");
        }
        try {
          fileContents = jinJava.render(Files.readString(sourceFile.toPath()), ImmutableMap.of());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      } else {
        try {
          fileContents = Files.readString(sourceFile.toPath());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }

      final JsonNode contents;
      try {
        contents = YAML_READER.readTree(fileContents);
      } catch (IOException e) {
        throw new UncheckedIOException("Could not read config yaml.", e);
      }
      try {
        File renamedOutFile = outFile;
        if (outFile.getName().contains(".jinja.")) {
          renamedOutFile = new File(outFile.getParent(), outFile.getName().replace(".jinja.", "."));
        }
        JSON_WRITER.writeValue(renamedOutFile, contents);
      } catch (IOException e) {
        throw new UncheckedIOException("Could not write config json.", e);
      }
    }
  }

  public interface ActionParameters extends WorkParameters {
    Property<File> getProjectDirectory();

    Property<File> getSourceFile();

    Property<File> getOutFile();
  }
}
