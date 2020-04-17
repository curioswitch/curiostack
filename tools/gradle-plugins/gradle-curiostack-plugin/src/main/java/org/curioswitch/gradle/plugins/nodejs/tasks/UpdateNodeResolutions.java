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
package org.curioswitch.gradle.plugins.nodejs.tasks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.inject.Inject;
import org.curioswitch.gradle.plugins.nodejs.NodeSetupExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public class UpdateNodeResolutions extends DefaultTask {

  public static final String NAME = "updateNodeResolutions";
  public static final String CHECK_NAME = "checkNodeResolutions";

  private static final List<String> CURIOSTACK_PACKAGES =
      ImmutableList.of(
          "@curiostack/eslint-config-base",
          "@curiostack/eslint-config-web",
          "@curiostack/base-node-dev",
          "@curiostack/base-web");

  private static final Splitter PACKAGE_SPLITTER = Splitter.on('/');

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
    var config = getProject().getExtensions().getByType(NodeSetupExtension.class);

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

    Map<String, String> managedDependencies = new HashMap<>();
    List<String> excludes = config.getExcludes().get();

    for (var pkg : CURIOSTACK_PACKAGES) {
      String packageFolder = Iterables.getLast(PACKAGE_SPLITTER.splitToList(pkg));
      File localPackageJson = getProject().file("common/web/" + packageFolder + "/package.json");
      if (!localPackageJson.exists()) {
        localPackageJson = getProject().file("node_modules/" + pkg + "/package.json");
      }
      if (!localPackageJson.exists()) {
        throw new GradleException(
            "Could not find "
                + pkg
                + "'s package.json to check resolutions. Did yarn run correctly?");
      }

      Map<String, String> dependencies =
          OBJECT_MAPPER.convertValue(
              OBJECT_MAPPER.readTree(localPackageJson).get("dependencies"), NODE_DEPENDENCIES);
      dependencies.forEach(
          (name, version) -> {
            if (!excludes.contains(name)) {
              managedDependencies.put(name, version);
            }
          });
    }

    if (checkOnly) {
      managedDependencies.forEach(
          (name, version) -> {
            if (!version.equals(oldResolutions.get(name))) {
              throw new GradleException(
                  "Resolution ["
                      + name
                      + "@"
                      + version
                      + "] out of date versus base-web version. "
                      + "Run updateNodeResolutions to update them.");
            }
          });
      return;
    }

    Map<String, String> newResolutions = new TreeMap<>();
    newResolutions.putAll(oldResolutions);
    newResolutions.putAll(managedDependencies);

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
