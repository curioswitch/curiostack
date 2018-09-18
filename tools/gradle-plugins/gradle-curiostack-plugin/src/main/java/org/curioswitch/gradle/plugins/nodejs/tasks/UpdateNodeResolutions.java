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

package org.curioswitch.gradle.plugins.nodejs.tasks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.linecorp.armeria.client.ClientDecoration;
import com.linecorp.armeria.client.ClientOption;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.retry.RetryStrategy;
import com.linecorp.armeria.client.retry.RetryingHttpClient;
import com.linecorp.armeria.common.AggregatedHttpMessage;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public class UpdateNodeResolutions extends DefaultTask {

  public static final String NAME = "updateNodeResolutions";
  public static final String CHECK_NAME = "checkNodeResolutions";

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

  private static final TypeReference<Map<String, String>> NODE_DEPENDENCIES =
      new TypeReference<>() {};

  private final boolean checkOnly;

  @Inject
  public UpdateNodeResolutions(boolean checkOnly) {
    this.checkOnly = checkOnly;
  }

  @InputFile
  public File getOldPackageJson() {
    return packageJson();
  }

  @OutputFile
  public File getNewPackageJson() {
    return packageJson();
  }

  private File packageJson() {
    return getProject().file("package.json");
  }

  @TaskAction
  public void exec() throws IOException {
    JsonNode root = OBJECT_MAPPER.readTree(packageJson());
    if (!root.has("resolutions")) {
      logPackageJsonError();
      return;
    }

    Map<String, String> oldResolutions =
        OBJECT_MAPPER.convertValue(root.get("resolutions"), NODE_DEPENDENCIES);
    String baseWebVersion = oldResolutions.get("@curiostack/base-web");

    if (baseWebVersion == null) {
      logPackageJsonError();
      return;
    }

    var localBaseWebPackageJson =
        getProject()
            .file(System.getProperty("packageJsonLocation", "common/web/base-web/package.json"));
    if (!localBaseWebPackageJson.exists()) {
      localBaseWebPackageJson = getProject().file("node_modules/@curiostack/base-web/package.json");
    }
    final byte[] baseWebPackageJson;
    if (localBaseWebPackageJson.exists()) {
      baseWebPackageJson = Files.readAllBytes(localBaseWebPackageJson.toPath());
    } else {
      String urlPath =
          "/curioswitch/curiostack/%40curiostack/base-web-"
              + baseWebVersion
              + "/common/web/base-web/package.json";
      var client =
          HttpClient.of(
              "https://raw.githubusercontent.com/",
              ClientOption.DECORATION.newValue(
                  ClientDecoration.of(
                      HttpRequest.class,
                      HttpResponse.class,
                      RetryingHttpClient.newDecorator(RetryStrategy.onServerErrorStatus()))));
      AggregatedHttpMessage msg = client.get(urlPath).aggregate().join();
      if (!msg.status().equals(HttpStatus.OK)) {
        throw new IllegalStateException("Could not fetch base-web package.json.");
      }
      baseWebPackageJson = msg.content().array();
    }

    Map<String, String> baseWebDependencies =
        OBJECT_MAPPER.convertValue(
            OBJECT_MAPPER.readTree(baseWebPackageJson).get("dependencies"), NODE_DEPENDENCIES);

    if (checkOnly) {
      baseWebDependencies.forEach(
          (name, version) -> {
            if (!version.equals(oldResolutions.get(name))) {
              throw new GradleException(
                  "Resolutions out of date versus base-web version. "
                      + "Run updateNodeResolutions to update them.");
            }
          });
      return;
    }

    Map<String, String> newResolutions = new TreeMap<>();
    newResolutions.putAll(oldResolutions);
    newResolutions.putAll(baseWebDependencies);

    // Recreate new root instead of replacing field to preserve order.
    ObjectNode newRoot = OBJECT_MAPPER.getNodeFactory().objectNode();

    for (Entry<String, JsonNode> field : ImmutableList.copyOf(root.fields())) {
      if (!field.getKey().equals("resolutions")) {
        newRoot.set(field.getKey(), field.getValue());
      } else {
        newRoot.putPOJO("resolutions", newResolutions);
      }
    }

    OBJECT_MAPPER.writeValue(packageJson(), newRoot);
  }

  private void logPackageJsonError() {
    getLogger()
        .warn(
            "No resolutions field in package.json. To use this task, there must be a"
                + " resolutions field with a version specified for @curiostack/base-web");
  }
}
