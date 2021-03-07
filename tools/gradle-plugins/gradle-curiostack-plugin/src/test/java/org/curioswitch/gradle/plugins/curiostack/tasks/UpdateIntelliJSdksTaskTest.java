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

package org.curioswitch.gradle.plugins.curiostack.tasks;

import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.curioswitch.gradle.golang.GolangSetupPlugin;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.plugins.curiostack.CuriostackRootPlugin;
import org.curioswitch.gradle.plugins.curiostack.ToolDependencies;
import org.curioswitch.gradle.testing.GradleTempDirectories;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// TODO(choko): Update
@Disabled
class UpdateIntelliJSdksTaskTest {

  private Path testUserHome;
  private String oldUserHome;
  private Path testGradleHome;

  private UpdateIntelliJSdksTask task;

  @BeforeEach
  void setUserHome() throws Exception {
    oldUserHome = System.getProperty("user.home");
    testUserHome = GradleTempDirectories.create("home");
    testGradleHome = GradleTempDirectories.create("gradlehome");
    System.setProperty("user.home", testUserHome.toAbsolutePath().toString());

    // Add an unrelated folder to make it look just a little more like a user home.
    Files.writeString(
        Files.createDirectories(testUserHome.resolve("curiotest")).resolve("foo.txt"), "bar");

    var project = ProjectBuilder.builder().withGradleUserHomeDir(testGradleHome.toFile()).build();
    var properties = project.getExtensions().getByType(ExtraPropertiesExtension.class);
    properties.set("org.curioswitch.curiostack.tools.openjdk", "zulu13.28.11-ca-jdk13.0.1");

    project.getPlugins().apply(CuriostackRootPlugin.class);
    project.getPlugins().apply(GolangSetupPlugin.class);

    task =
        project
            .getTasks()
            .withType(UpdateIntelliJSdksTask.class)
            .getByName(UpdateIntelliJSdksTask.NAME);
  }

  @AfterEach
  void resetUserHome() {
    assertThat(oldUserHome).isNotNull();
    System.setProperty("user.home", oldUserHome);
  }

  @Test
  void noIntelliJFolders() throws Exception {
    task.exec();

    assertNoExistingConfig(
        testUserHome.resolve(UpdateIntelliJSdksTask.LATEST_INTELLIJ_CONFIG_FOLDER));
  }

  @Test
  void noExistingFile() throws Exception {
    Path intelliJFolder = Files.createDirectories(testUserHome.resolve(".IntelliJIdea2019.1"));

    task.exec();

    assertNoExistingConfig(intelliJFolder);
  }

  @Test
  void noExistingFile_community() throws Exception {
    Path intelliJFolder = Files.createDirectories(testUserHome.resolve(".IdeaIC2019.1"));

    task.exec();

    assertNoExistingConfig(intelliJFolder);
    testTemplate("update-intellij-jdks-task-test-tables/only-curio-openjdk.template.xml");
  }

  private void assertNoExistingConfig(Path intellijFolder) {
    assertThat(intellijFolder.resolve("config/options/jdk.table.xml"))
        .hasContent(
            testTemplate("update-intellij-jdks-task-test-tables/only-curio-openjdk.template.xml"));

    assertThat(intellijFolder.resolve("config/options/go.sdk.xml"))
        .hasContent(
            testTemplate("update-intellij-jdks-task-test-tables/only-curio-gosdk.template.xml"));

    assertThat(intellijFolder.resolve("config/options/goLibraries.xml"))
        .hasContent(
            testTemplate("update-intellij-jdks-task-test-tables/only-curio-gopath.template.xml"));
  }

  @Test
  void existingUnrelated() throws Exception {
    Path optionsFolder =
        Files.createDirectories(testUserHome.resolve(".IntelliJIdea2019.1/config/options"));
    Files.writeString(
        optionsFolder.resolve("jdk.table.xml"),
        resource("update-intellij-jdks-task-test-tables/existing-unrelated.xml"));
    Files.writeString(
        optionsFolder.resolve("go.sdk.xml"),
        resource("update-intellij-jdks-task-test-tables/existing-gosdk.xml"));
    Files.writeString(
        optionsFolder.resolve("goLibraries.xml"),
        resource("update-intellij-jdks-task-test-tables/existing-gopath.xml"));

    task.exec();

    assertThat(optionsFolder.resolve("jdk.table.xml"))
        .hasContent(
            testTemplate(
                "update-intellij-jdks-task-test-tables/existing-and-curio-openjdk.template.xml"));

    assertThat(optionsFolder.resolve("go.sdk.xml"))
        .hasContent(
            testTemplate(
                "update-intellij-jdks-task-test-tables/existing-and-curio-gosdk.template.xml"));

    assertThat(optionsFolder.resolve("goLibraries.xml"))
        .hasContent(
            testTemplate(
                "update-intellij-jdks-task-test-tables/existing-and-curio-gopath.template.xml"));
  }

  @Test
  void existingRelated() throws Exception {
    Path optionsFolder =
        Files.createDirectories(testUserHome.resolve(".IntelliJIdea2019.1/config/options"));
    Files.writeString(
        optionsFolder.resolve("jdk.table.xml"),
        resource("update-intellij-jdks-task-test-tables/existing-related.xml"));
    Files.writeString(
        optionsFolder.resolve("go.sdk.xml"),
        testTemplate("update-intellij-jdks-task-test-tables/only-curio-gosdk.template.xml"));
    Files.writeString(
        optionsFolder.resolve("goLibraries.xml"),
        testTemplate("update-intellij-jdks-task-test-tables/only-curio-gopath.template.xml"));

    task.exec();

    assertThat(optionsFolder.resolve("jdk.table.xml"))
        .hasContent(
            testTemplate(
                "update-intellij-jdks-task-test-tables/existing-and-curio-openjdk.template.xml"));

    assertThat(optionsFolder.resolve("go.sdk.xml"))
        .hasContent(
            testTemplate("update-intellij-jdks-task-test-tables/only-curio-gosdk.template.xml"));

    assertThat(optionsFolder.resolve("goLibraries.xml"))
        .hasContent(
            testTemplate("update-intellij-jdks-task-test-tables/only-curio-gopath.template.xml"));
  }

  private static String resource(String path) {
    try {
      return Resources.toString(Resources.getResource(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not get resource " + path, e);
    }
  }

  private String testTemplate(String path) {
    String template = resource(path);

    Jinjava jinjava = new Jinjava();
    return jinjava.render(
        template,
        ImmutableMap.<String, Object>builder()
            .put("gradleHome", testGradleHome.toAbsolutePath().toString().replace('\\', '/'))
            .put("jdkFolder", "jdk-zulu13.28.11-ca-jdk13.0.1")
            .put("javaVersion", "zulu13.28.11-ca-jdk13.0.1")
            .put("goVersion", ToolDependencies.getDefaultVersion("golang"))
            .build());
  }

  private static String suffix() {
    switch (new PlatformHelper().getOs()) {
      case WINDOWS:
        return "win_x64";
      case MAC_OSX:
        return "macosx_x64";
      case LINUX:
        return "linux_x64";
      default:
        throw new IllegalStateException("Unknown OS");
    }
  }
}
