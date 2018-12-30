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

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static java.lang.System.lineSeparator;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class GenerateApiServerTask extends DefaultTask {

  @TaskAction
  public void exec() {
    var ant = getAnt();
    ant.invokeMethod(
        "echo", ImmutableMap.of("message", "Generating an API server (see ...) for details."));
    ant.invokeMethod(
        "input",
        ImmutableMap.of(
            "message",
                "What is the path that the files should be written to (e.g., path/to/project)?",
            "addproperty", "outputPath"));
    ant.invokeMethod(
        "input",
        ImmutableMap.of(
            "message",
            "What is name of the project? Two artifacts, {name}-api and {name}-server will "
                + "be generated. {name}-server will be used in deployments and should be relatively "
                + "concise. The name should be hyphen-case",
            "addproperty",
            "name"));
    ant.invokeMethod(
        "input",
        ImmutableMap.of(
            "message",
            "What is the java package to place code under? "
                + "API will live under {package}.api and server code will live under "
                + "{package}.server",
            "addproperty",
            "javaPackage"));
    ant.invokeMethod(
        "input",
        ImmutableMap.of(
            "message",
            "What is the proto package to place the API under? "
                + "Defaults to same value as the java package + .api.",
            "addproperty",
            "protoPackage",
            "defaultvalue",
            ant.getProperty("javaPackage") + ".api"));
    ant.invokeMethod(
        "input",
        ImmutableMap.of(
            "message",
            "What is the name of the API service? This should generally be UpperCamelCase. "
                + "Defaults to UpperCamelCase version of {name}Service.",
            "addproperty",
            "serviceName",
            "defaultvalue",
            LOWER_HYPHEN.to(UPPER_CAMEL, (String) ant.getProperty("name")) + "Service"));

    var outputPathStr = (String) ant.getProperty("outputPath");
    if (outputPathStr.startsWith("/")) {
      throw new IllegalArgumentException(
          "outputPath must be a relative path below the repository root, it cannot begin with /");
    }
    if (outputPathStr.endsWith("/")) {
      outputPathStr = outputPathStr.substring(0, outputPathStr.length() - 1);
    }

    Path outputPath = Paths.get(getProject().getRootDir().getAbsolutePath(), outputPathStr);
    if (Files.exists(outputPath)) {
      throw new IllegalArgumentException(
          "Output path already exists. If this is correct, remove the directory first.");
    }

    Path apiOutputPath = outputPath.resolve("api");
    Path serverOutputPath = outputPath.resolve("server");

    Path copyrightFile = null;
    try (var s = Files.list(getProject().file(".baseline/copyright").toPath())) {
      copyrightFile = s.sorted().findFirst().orElse(null);
    } catch (IOException e) {
      // Ignore exceptions trying to read copyright.
    }

    String copyright = "";
    try (var s = Files.lines(copyrightFile)) {
      copyright =
          s.map(line -> line.replace("${today.year}", String.valueOf(Year.now().getValue())))
              .map(line -> " * " + line)
              .collect(
                  Collectors.joining(
                      lineSeparator(),
                      "/*" + lineSeparator(),
                      lineSeparator() + " */" + lineSeparator()));
    } catch (IOException e) {
      // Ignore exceptions trying to read copyright.
    }

    String apiDependency = ':' + (outputPathStr + "/api").replace("/", ":");

    String protoPackage = (String) ant.getProperty("protoPackage");
    String javaPackage = (String) ant.getProperty("javaPackage");
    String serviceName = (String) ant.getProperty("serviceName");

    Map<String, Object> context =
        ImmutableMap.<String, Object>builder()
            .put("copyright", copyright)
            .put("name", ant.getProperty("name"))
            .put("proto_package", protoPackage)
            .put("java_package", javaPackage)
            .put("service_name", serviceName)
            .put("api_dependency", apiDependency)
            .build();

    render(
        "templates/apiserver/api/build.gradle.kts.tmpl",
        context,
        apiOutputPath.resolve("build.gradle.kts"));
    render(
        "templates/apiserver/api/service.proto.tmpl",
        context,
        apiOutputPath.resolve(
            "src/main/proto/" + protoPackage.replace(".", "/") + "/service.proto"));

    render(
        "templates/apiserver/server/build.gradle.kts.tmpl",
        context,
        serverOutputPath.resolve("build.gradle.kts"));
    render(
        "templates/apiserver/server/Main.java.tmpl",
        context,
        serverOutputPath.resolve(
            "src/main/java/"
                + javaPackage.replace(".", "/")
                + "/server/"
                + serviceName
                + "Main.java"));
    render(
        "templates/apiserver/server/ServiceImpl.java.tmpl",
        context,
        serverOutputPath.resolve(
            "src/main/java/" + javaPackage.replace(".", "/") + "/server/" + serviceName + ".java"));
  }

  private static void render(String templatePath, Map<String, Object> context, Path outputPath) {
    if (!Files.exists(outputPath.getParent())) {
      try {
        Files.createDirectories(outputPath.getParent());
      } catch (IOException e) {
        throw new UncheckedIOException("Could not create directory.", e);
      }
    }

    Jinjava jinjava = new Jinjava();

    final String template;
    try {
      template = Resources.toString(Resources.getResource(templatePath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read template.", e);
    }

    String rendered = jinjava.render(template, context);
    try {
      Files.writeString(outputPath, rendered);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write file.", e);
    }
  }
}
