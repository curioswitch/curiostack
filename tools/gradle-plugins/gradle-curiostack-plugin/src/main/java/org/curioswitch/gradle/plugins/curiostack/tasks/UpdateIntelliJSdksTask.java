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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link org.gradle.api.Task} to update IntelliJ's jdk.table.xml configuration to include the JDK
 * downloaded by CurioStack as well as other SDKs.
 */
public class UpdateIntelliJSdksTask extends DefaultTask {

  public static final String NAME = "curioUpdateIntelliJSdks";

  @VisibleForTesting static final String LATEST_INTELLIJ_CONFIG_FOLDER = ".IntelliJIdea2019.1";

  // TODO(choko): Use the same variable when generating get-jdk.sh and here.
  @VisibleForTesting static final String JDK_FOLDER_NAME = "jdk-11.0.4+11";

  @VisibleForTesting static final String JAVA_VERSION = "11.0.4";

  @VisibleForTesting static final String JAVA_8_VERSION = "1.8.0_222";

  @VisibleForTesting static final String JDK_8_FOLDER_NAME = "jdk8u222-b10/jdk8u222-b10";

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

  private static final List<String> JAVA_8_JARS =
      ImmutableList.of(
          "charsets.jar",
          "deploy.jar",
          "ext/access-bridge-64.jar",
          "ext/cldrdata.jar",
          "ext/dnsns.jar",
          "ext/jaccess.jar",
          "ext/jfxrt.jar",
          "ext/localedata.jar",
          "ext/nashorn.jar",
          "ext/sunec.jar",
          "ext/sunjce_provider.jar",
          "ext/sunmscapi.jar",
          "ext/sunpkcs11.jar",
          "ext/zipfs.jar",
          "javaws.jar",
          "jce.jar",
          "jfr.jar",
          "jfxswt.jar",
          "jsse.jar",
          "management-agent.jar",
          "plugin.jar",
          "resources.jar",
          "rt.jar");

  private static final String EMPTY_JDK_TABLE =
      "<application>\n"
          + "  <component name=\"ProjectJdkTable\">\n"
          + "  </component>\n"
          + "</application>";

  private static final String EMPTY_GO_SDK_TABLE =
      "<application>\n"
          + "  <component name=\"GoSdkList\">\n"
          + "    <sdk-path>\n"
          + "      <set>\n"
          + "      </set>\n"
          + "    </sdk-path>\n"
          + "  </component>\n"
          + "</application>";

  private static final String EMPTY_GO_LIBRARIES =
      "<application>\n"
          + "  <component name=\"GoLibraries\">\n"
          + "    <option name=\"urls\">\n"
          + "      <list>\n"
          + "      </list>\n"
          + "    </option>\n"
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
                          && (path.getFileName().toString().startsWith(".IntelliJIdea")
                              || path.getFileName().toString().startsWith(".IdeaIC")))
              .sorted()
              .collect(toImmutableList());
    }

    final Path intelliJFolder;
    if (!intelliJFolders.isEmpty()) {
      intelliJFolder = Iterables.getLast(intelliJFolders);
    } else {
      getProject()
          .getLogger()
          .info("No IntelliJ config folder found, writing to default location.");
      intelliJFolder = userHome.resolve(LATEST_INTELLIJ_CONFIG_FOLDER);
    }

    getProject()
        .getLogger()
        .info("Updating IntelliJ folder {}, found folders {}", intelliJFolder, intelliJFolders);

    var jdkTable =
        Files.createDirectories(intelliJFolder.resolve("config/options")).resolve("jdk.table.xml");
    updateConfig(
        jdkTable,
        JDK_FOLDER_NAME,
        "11",
        "curiostack/openjdk-intellij-table-snippet.template.xml",
        ImmutableMap.of("javaVersion", JAVA_VERSION, "javaModules", JAVA_MODULES),
        getProject());
    updateConfig(
        jdkTable,
        JDK_8_FOLDER_NAME,
        "1.8",
        "curiostack/openjdk-8-intellij-table-snippet.template.xml",
        ImmutableMap.of("javaVersion", JAVA_8_VERSION, "javaModules", JAVA_8_JARS),
        getProject());

    updateGoSdk(intelliJFolder, getProject());
    updateGoPath(intelliJFolder, getProject());
  }

  private static void updateGoSdk(Path intelliJFolder, Project project) throws IOException {
    var goSdkXmlFile = intelliJFolder.resolve("config/options/go.sdk.xml");
    final List<String> goSdkXmlLines;
    if (Files.exists(goSdkXmlFile)) {
      goSdkXmlLines = Files.readAllLines(goSdkXmlFile);
    } else {
      goSdkXmlLines = EMPTY_GO_SDK_TABLE.lines().collect(toImmutableList());
    }

    String goSdk =
        intellijPath(
            DownloadedToolManager.get(project)
                .getToolDir("go")
                .resolve("go")
                .toAbsolutePath()
                .toString());

    if (goSdkXmlLines.stream().anyMatch(line -> line.contains("<option value=\"" + goSdk + "\""))) {
      return;
    }

    var updatedGoSdkXmlLines = ImmutableList.<String>builder();
    for (int lineIndex = 0; lineIndex < goSdkXmlLines.size(); lineIndex++) {
      String line = goSdkXmlLines.get(lineIndex);
      updatedGoSdkXmlLines.add(line);
      if (line.contains("<set>")) {
        updatedGoSdkXmlLines.add("        <option value=\"" + goSdk + "\" />");
      }
    }

    Files.writeString(goSdkXmlFile, String.join("\n", updatedGoSdkXmlLines.build()));
  }

  private static void updateGoPath(Path intelliJFolder, Project project) throws IOException {
    var goLibrariesFile = intelliJFolder.resolve("config/options/goLibraries.xml");
    final List<String> goLibrariesLines;
    if (Files.exists(goLibrariesFile)) {
      goLibrariesLines = Files.readAllLines(goLibrariesFile);
    } else {
      goLibrariesLines = EMPTY_GO_LIBRARIES.lines().collect(toImmutableList());
    }

    String gopath =
        intellijPath(
            CommandUtil.getCuriostackDir(project).resolve("gopath").toAbsolutePath().toString());

    if (goLibrariesLines.stream()
        .anyMatch(line -> line.contains("<option value=\"file://" + gopath + "\""))) {
      return;
    }

    var updatedGoLibrariesLines = ImmutableList.<String>builder();
    for (int lineIndex = 0; lineIndex < goLibrariesLines.size(); lineIndex++) {
      String line = goLibrariesLines.get(lineIndex);
      updatedGoLibrariesLines.add(line);
      if (line.contains("<list>")) {
        updatedGoLibrariesLines.add("        <option value=\"file://" + gopath + "\" />");
      }
    }

    Files.writeString(goLibrariesFile, String.join("\n", updatedGoLibrariesLines.build()));
  }

  private static void updateConfig(
      Path jdkTable,
      String jdkSubFolder,
      String jdkName,
      String templatePath,
      Map<String, Object> templateVars,
      Project project)
      throws IOException {
    String jdkFolder =
        CommandUtil.getCuriostackDir(project)
            .resolve("openjdk")
            .resolve(jdkSubFolder)
            .toAbsolutePath()
            .toString();
    if (new PlatformHelper().getOs() == OperatingSystem.WINDOWS) {
      // IntelliJ config users a normal Windows path with backslashes turned to slashes, e.g.
      // C:/Users/Choko/.gradle/openjdk/jdk-12.0.2
      jdkFolder = jdkFolder.replace('\\', '/');
    }

    templateVars =
        ImmutableMap.<String, Object>builder()
            .putAll(templateVars)
            .put("jdkFolder", jdkFolder)
            .build();

    final String existingJdks;
    if (Files.exists(jdkTable)) {
      existingJdks = Files.readString(jdkTable);
      // Do a quick simple check for our openjdk path, if it exists as a string at all we should
      // already be good.
      if (existingJdks.contains(jdkFolder)) {
        project
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
      if (line.contains("<name value=\"" + jdkName + "\"")) {
        addJdkSnippet(templatePath, templateVars, updatedTables, true);
        updatedExistingJdk = true;
        for (; lineIndex < existingJdksLines.size(); lineIndex++) {
          if (line.contains("</jdk>")) {
            break;
          }
        }
      }
    }

    if (!updatedExistingJdk) {
      addJdkSnippet(templatePath, templateVars, updatedTables, false);
    }

    updatedTables.add("  </component>").add("</application>");

    Files.writeString(jdkTable, String.join("\n", updatedTables.build()));

    // We optimistically update Go SDKs as well even if the build doesn't use Go. The worst that
    // could happen is a couple of red SDKs in the IntelliJ lists.

  }

  private static void addJdkSnippet(
      String templatePath,
      Map<String, Object> templateVars,
      ImmutableList.Builder<String> lines,
      boolean skipStart)
      throws IOException {
    Jinjava jinjava = new Jinjava();

    String template =
        Resources.toString(Resources.getResource(templatePath), StandardCharsets.UTF_8);

    String rendered = jinjava.render(template, templateVars);

    rendered.lines().skip(skipStart ? 2 : 0).forEach(lines::add);
  }

  private static String intellijPath(String rawPath) {
    if (new PlatformHelper().getOs() == OperatingSystem.WINDOWS) {
      // IntelliJ config users a normal Windows path with backslashes turned to slashes, e.g.
      // C:/Users/Choko/.gradle/openjdk/jdk-12.0.2
      return rawPath.replace('\\', '/');
    }
    return rawPath;
  }
}
