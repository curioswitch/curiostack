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

package org.curioswitch.gradle.plugins.grpcapi.tasks;

import static org.curioswitch.gradle.plugins.grpcapi.GrpcApiPlugin.PACKAGE_JSON_TEMPLATE;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.curioswitch.gradle.plugins.grpcapi.GrpcExtension;
import org.curioswitch.gradle.plugins.grpcapi.ImmutableGrpcExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public class PackageWebTask extends DefaultTask {

  private static final String CURIOSTACK_BASE_NODE_DEV_VERSION = "0.0.14";
  private static final String GOOGLE_PROTOBUF_VERSION = "3.7.1";
  private static final String GRPC_WEB_VERSION = "1.0.4";
  private static final String TYPES_GOOGLE_PROTOBUF_VERSION = "3.2.7";

  @Input
  public String getPackageJsonTemplate() {
    return PACKAGE_JSON_TEMPLATE;
  }

  @Input
  public List<String> getVersions() {
    return ImmutableList.of(
        CURIOSTACK_BASE_NODE_DEV_VERSION,
        GOOGLE_PROTOBUF_VERSION,
        GRPC_WEB_VERSION,
        TYPES_GOOGLE_PROTOBUF_VERSION);
  }

  @InputDirectory
  public String getWebProtosDir() {
    return "build/webprotos";
  }

  @OutputDirectory
  public String getWebDir() {
    return "build/web";
  }

  @TaskAction
  public void exec() throws IOException {
    ImmutableGrpcExtension config = getProject().getExtensions().getByType(GrpcExtension.class);

    Path packageJsonPath = getProject().file("build/web/package.json").toPath();
    String packageName =
        config.webPackageName().isEmpty()
            ? getProject()
                .getConvention()
                .getPlugin(BasePluginConvention.class)
                .getArchivesBaseName()
            : config.webPackageName();

    Files.writeString(
        packageJsonPath,
        PACKAGE_JSON_TEMPLATE
            .replaceFirst("\\|PACKAGE_NAME\\|", packageName)
            .replaceFirst("\\|TYPES_GOOGLE_PROTOBUF_VERSION\\|", TYPES_GOOGLE_PROTOBUF_VERSION)
            .replaceFirst("\\|GOOGLE_PROTOBUF_VERSION\\|", GOOGLE_PROTOBUF_VERSION)
            .replaceFirst("\\|GRPC_WEB_VERSION\\|", GRPC_WEB_VERSION)
            .replaceFirst(
                "\\|CURIOSTACK_BASE_NODE_DEV_VERSION\\|", CURIOSTACK_BASE_NODE_DEV_VERSION));

    getProject()
        .copy(
            copy -> {
              copy.from("build/webprotos");
              copy.into("build/web");
            });
  }
}
