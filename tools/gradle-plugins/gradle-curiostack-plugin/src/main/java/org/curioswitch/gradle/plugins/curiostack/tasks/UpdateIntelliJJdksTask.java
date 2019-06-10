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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link org.gradle.api.Task} to update IntelliJ's jdk.table.xml configuration to include the JDK
 * downloaded by CurioStack.
 */
public class UpdateIntelliJJdksTask extends DefaultTask {

  // TODO(choko): Use the same variable when generating get-jdk.sh and here.
  @VisibleForTesting static final String JDK_FOLDER_NAME = "jdk-11.0.3+7";

  @VisibleForTesting static final String JAVA_VERSION = "11.0.3";

  private static final List<String> JAVA_MODULES =
      ImmutableList.of(
          "java.base",
          "java.compiler",
          "java.datatransfer",
          "java.desktop",
          "java.instrument",
          "java.logging",
          "java.management",
          "java.management.rmi",
          "java.naming",
          "java.net.http",
          "java.prefs",
          "java.rmi",
          "java.scripting",
          "java.se",
          "java.security.jgss",
          "java.security.sasl",
          "java.smartcardio",
          "java.sql",
          "java.sql.rowset",
          "java.transaction.xa",
          "java.xml",
          "java.xml.crypto",
          "jdk.accessibility",
          "jdk.aot",
          "jdk.attach",
          "jdk.charsets",
          "jdk.compiler",
          "jdk.crypto.cryptoki",
          "jdk.crypto.ec",
          "jdk.crypto.mscapi",
          "jdk.dynalink",
          "jdk.editpad",
          "jdk.hotspot.agent",
          "jdk.httpserver",
          "jdk.internal.ed",
          "jdk.internal.jvmstat",
          "jdk.internal.le",
          "jdk.internal.opt",
          "jdk.internal.vm.ci",
          "jdk.internal.vm.compiler",
          "jdk.internal.vm.compiler.management",
          "jdk.jartool",
          "jdk.javadoc",
          "jdk.jcmd",
          "jdk.jconsole",
          "jdk.jdeps",
          "jdk.jdi",
          "jdk.jdwp.agent",
          "jdk.jfr",
          "jdk.jlink",
          "jdk.jshell",
          "jdk.jsobject",
          "jdk.jstatd",
          "jdk.localedata",
          "jdk.management",
          "jdk.management.agent",
          "jdk.management.jfr",
          "jdk.naming.dns",
          "jdk.naming.rmi",
          "jdk.net",
          "jdk.pack",
          "jdk.rmic",
          "jdk.scripting.nashorn",
          "jdk.scripting.nashorn.shell",
          "jdk.sctp",
          "jdk.security.auth",
          "jdk.security.jgss",
          "jdk.unsupported",
          "jdk.unsupported.desktop",
          "jdk.xml.dom",
          "jdk.zipfs");

  private static final String EMPTY_JDK_TABLE =
      "<application>\n"
          + "  <component name=\"ProjectJdkTable\">\n"
          + "  </component>\n"
          + "</application>";

  @TaskAction
  public void exec() throws IOException {
    Path userHome = Paths.get(System.getProperty("user.home"));
    final List<Path> intelliJFolders;
    try (var files = Files.list(userHome)) {
      intelliJFolders =
          files
              .filter(
                  path ->
                      Files.isDirectory(path)
                          && path.getFileName().toString().startsWith(".IntelliJIdea"))
              .sorted()
              .collect(toImmutableList());
    }

    if (intelliJFolders.isEmpty()) {
      getProject().getLogger().info("No IntelliJ config folder found, not setting up JDKs.");
      return;
    }

    Path intelliJFolder = Iterables.getLast(intelliJFolders);
    getProject()
        .getLogger()
        .info("Updating IntelliJ folder {}, found folders {}", intelliJFolder, intelliJFolders);

    String jdkFolder =
        getProject()
            .getGradle()
            .getGradleUserHomeDir()
            .toPath()
            .resolve("curiostack/openjdk")
            .resolve(JDK_FOLDER_NAME)
            .toAbsolutePath()
            .toString();
    if (new PlatformHelper().getOs() == OperatingSystem.WINDOWS) {
      // IntelliJ config users a normal Windows path with backslashes turned to slashes, e.g.
      // C:/Users/Choko/.gradle/openjdk/jdk-12.0.2
      jdkFolder = jdkFolder.replace('\\', '/');
    }

    var jdkTable =
        Files.createDirectories(intelliJFolder.resolve("config/options")).resolve("jdk.table.xml");
    final String existingJdks;
    if (Files.exists(jdkTable)) {
      existingJdks = Files.readString(jdkTable);
      // Do a quick simple check for our openjdk path, if it exists as a string at all we should
      // already be good.
      if (existingJdks.contains(jdkFolder)) {
        getProject()
            .getLogger()
            .info("OpenJDK already configured in IntelliJ config, not doing anything.");
        return;
      }
    } else {
      existingJdks = EMPTY_JDK_TABLE;
    }

    List<String> existingJdksLines = existingJdks.lines().collect(toImmutableList());

    // To minimize dependence on the IntelliJ JDK XML format, we print out existing content as is.
    var updatedTables =
        ImmutableList.<String>builderWithExpectedSize(existingJdksLines.size() + 100);
    int lineIndex = 0;
    boolean updatedExistingJdk = false;
    for (; lineIndex < existingJdksLines.size(); lineIndex++) {
      String line = existingJdksLines.get(lineIndex);
      if (line.contains("</component>")) {
        break;
      }
      updatedTables.add(line);
      if (line.contains("<name value=\"11\"")) {
        addJdkSnippet(jdkFolder, updatedTables, true);
        updatedExistingJdk = true;
        for (; lineIndex < existingJdksLines.size(); lineIndex++) {
          if (line.contains("</jdk>")) {
            break;
          }
        }
      }
    }

    if (!updatedExistingJdk) {
      addJdkSnippet(jdkFolder, updatedTables, false);
    }

    updatedTables.add("  </component>").add("</application>");

    Files.writeString(jdkTable, String.join("\n", updatedTables.build()));
  }

  private static void addJdkSnippet(
      String jdkFolder, ImmutableList.Builder<String> lines, boolean skipStart) throws IOException {
    Jinjava jinjava =
        new Jinjava(JinjavaConfig.newBuilder().withLstripBlocks(true).withTrimBlocks(true).build());

    String template =
        Resources.toString(
            Resources.getResource("curiostack/openjdk-intellij-table-snippet.template.xml"),
            StandardCharsets.UTF_8);

    String rendered =
        jinjava.render(
            template,
            ImmutableMap.of(
                "jdkFolder", jdkFolder, "javaVersion", JAVA_VERSION, "javaModules", JAVA_MODULES));

    rendered.lines().skip(skipStart ? 2 : 0).forEach(lines::add);
  }
}
