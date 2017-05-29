/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.plugins.curiostack;

import com.diffplug.gradle.spotless.JavaExtension;
import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.protobuf.gradle.ProtobufPlugin;
import com.google.protobuf.gradle.ProtobufSourceDirectorySet;
import com.gorylenko.GitPropertiesPlugin;
import com.palantir.baseline.plugins.BaselineIdea;
import groovy.util.Node;
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import me.champeau.gradle.JMHPlugin;
import me.champeau.gradle.JMHPluginExtension;
import nl.javadude.gradle.plugins.license.LicenseExtension;
import nl.javadude.gradle.plugins.license.LicensePlugin;
import org.curioswitch.gradle.plugins.curiostack.StandardDependencies.DependencySet;
import org.curioswitch.gradle.plugins.gcloud.GcloudPlugin;
import org.curioswitch.gradle.plugins.monorepo.MonorepoCircleCiPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.jvm.tasks.Jar;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;

public class CuriostackPlugin implements Plugin<Project> {

  @Override
  public void apply(Project rootProject) {
    PluginContainer plugins = rootProject.getPlugins();
    // Provides useful tasks like 'clean', 'assemble' to the root project.
    plugins.apply(BasePlugin.class);

    plugins.apply(BaselineIdea.class);

    plugins.apply(GcloudPlugin.class);
    plugins.apply(MonorepoCircleCiPlugin.class);

    String baselineFiles;
    try {
      baselineFiles =
          Resources.toString(
              Resources.getResource("META-INF/org.curioswitch.curiostack.baseline_manifest.txt"),
              StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    Task baselineUpdateConfig =
        rootProject
            .getTasks()
            .create("baselineUpdateConfig")
            .doLast(
                task -> {
                  File baselineDir = rootProject.file(".baseline");
                  baselineDir.mkdirs();
                  for (String filePath : baselineFiles.split("\n")) {
                    Path path = Paths.get(filePath);
                    Path outputDirectory =
                        Paths.get(baselineDir.getAbsolutePath()).resolve(path.getParent());
                    rootProject.file(outputDirectory.toAbsolutePath()).mkdirs();
                    try (FileOutputStream os =
                            new FileOutputStream(
                                outputDirectory.resolve(path.getFileName()).toFile());
                        InputStream is = Resources.getResource(filePath).openStream()) {
                      ByteStreams.copy(is, os);
                    } catch (IOException e) {
                      throw new UncheckedIOException(e);
                    }
                  }
                });

    if (!rootProject.file(".baseline").exists()) {
      rootProject.getTasks().getByName("ideaProject").dependsOn(baselineUpdateConfig);
    }

    rootProject.allprojects(
        project -> {
          setupRepositories(project);

          project.getPlugins().withType(JavaPlugin.class, plugin -> setupJavaProject(project));

          project
              .getPlugins()
              .withType(
                  LicensePlugin.class,
                  unused -> {
                    LicenseExtension license =
                        project.getExtensions().getByType(LicenseExtension.class);
                    license.setHeader(rootProject.file("LICENSE"));
                    license.mapping(
                        ImmutableMap.of(
                            "conf", "DOUBLESLASH_STYLE",
                            "proto", "JAVADOC_STYLE",
                            "yml", "SCRIPT_STYLE"));
                  });
        });

    rootProject
        .getPlugins()
        .withType(
            IdeaPlugin.class,
            idea -> {
              idea.getModel()
                  .getProject()
                  .getIpr()
                  .withXml(CuriostackPlugin::addAnnotationProcessingXml);
            });
  }

  private static void setupRepositories(Project project) {
    project.getRepositories().jcenter();
    project
        .getRepositories()
        .maven(
            maven -> {
              maven.setUrl("https://plugins.gradle.org/m2/");
            });
    project
        .getRepositories()
        .maven(
            maven -> {
              maven.setUrl("http://dl.bintray.com/curioswitch/curiostack");
            });
    project.getRepositories().mavenCentral();
    project.getRepositories().mavenLocal();
  }

  private static void setupJavaProject(Project project) {
    PluginContainer plugins = project.getPlugins();
    plugins.apply(BaselineIdea.class);
    plugins.apply(DependencyManagementPlugin.class);
    plugins.apply(GitPropertiesPlugin.class);
    plugins.apply(LicensePlugin.class);
    plugins.apply(SpotlessPlugin.class);

    project.getTasks().withType(JavaCompile.class, task -> task.getOptions().setIncremental(true));

    SourceSetContainer sourceSets =
        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
    setupAptSourceSet(project, sourceSets.getByName("main"));
    setupAptSourceSet(project, sourceSets.getByName("test"));

    // While Gradle attempts to generate a unique module name automatically,
    // it doesn't seem to always work properly, so we just always use unique
    // module names.
    project
        .getPlugins()
        .withType(
            IdeaPlugin.class,
            plugin -> {
              IdeaModule module = plugin.getModel().getModule();
              String moduleName = project.getName();
              Project ancestor = project.getParent();
              while (ancestor != null && ancestor != project.getRootProject()) {
                moduleName = ancestor.getName() + "-" + moduleName;
                ancestor = ancestor.getParent();
              }
              module.setName(moduleName);
            });

    DependencyManagementExtension dependencyManagement =
        project.getExtensions().getByType(DependencyManagementExtension.class);
    dependencyManagement.dependencies(
        dependencies -> {
          for (DependencySet set : StandardDependencies.DEPENDENCY_SETS) {
            dependencies.dependencySet(
                ImmutableMap.of(
                    "group", set.group(),
                    "version", set.version()),
                dependencySet -> set.modules().forEach(dependencySet::entry));
          }
          StandardDependencies.DEPENDENCIES.forEach(dependencies::dependency);
        });

    project.afterEvaluate(CuriostackPlugin::addStandardJavaTestDependencies);

    Javadoc javadoc = (Javadoc) project.getTasks().getByName("javadoc");
    CoreJavadocOptions options = (CoreJavadocOptions) javadoc.getOptions();
    options.quiet();
    options.addBooleanOption("Xdoclint:all", true);
    options.addBooleanOption("Xdoclint:-missing", true);

    project
        .getTasks()
        .create(
            "javadocJar",
            Jar.class,
            javadocJar -> {
              javadocJar.dependsOn(javadoc);
              javadocJar.setClassifier("javadoc");
              javadocJar.from(javadoc.getDestinationDir());
            });

    project
        .getTasks()
        .create(
            "sourceJar",
            Jar.class,
            sourceJar -> {
              sourceJar.setClassifier("sources");
              sourceJar.from(sourceSets.getByName("main").getAllSource());
            });

    SpotlessExtension spotless = project.getExtensions().getByType(SpotlessExtension.class);
    spotless.java(JavaExtension::googleJavaFormat);

    project
        .getTasks()
        .create(
            "resolveDependencies",
            resolveDependencies ->
                resolveDependencies.doLast(
                    unused -> {
                      project
                          .getConfigurations()
                          .all(
                              configuration -> {
                                if (configuration.isCanBeResolved()) {
                                  configuration.resolve();
                                }
                              });
                    }));

    // Protobuf plugin doesn't add proto sourceset to allSource, which seems like an omission.
    // We add it to make sure license plugin picks up the files.
    project
        .getPlugins()
        .withType(
            ProtobufPlugin.class,
            unused -> {
              for (SourceSet sourceSet : sourceSets) {
                sourceSet
                    .getAllSource()
                    .source(
                        ((ExtensionAware) sourceSet)
                            .getExtensions()
                            .getByType(ProtobufSourceDirectorySet.class));
              }
            });

    project
        .getPlugins()
        .withType(
            JMHPlugin.class,
            unused -> {
              JMHPluginExtension jmh = project.getExtensions().getByType(JMHPluginExtension.class);
              // Benchmarks are usually very small and converge quickly. If this stops being the case
              // these numbers can be adjusted.
              jmh.setFork(2);
              jmh.setIterations(5);

              jmh.setProfilers(ImmutableList.of("hs_comp"));
              jmh.setJmhVersion("1.19");

              Object jmhRegex = project.getRootProject().findProperty("jmhRegex");
              if (jmhRegex != null) {
                jmh.setInclude((String) jmhRegex);
              }

              // We will use the jmhManaged for any dependencies that should only be applied to JMH
              // but should be resolved by our managed dependencies. We need a separate configuration
              // to be able to provide the resolution workaround described below.
              Configuration jmhManaged = project.getConfigurations().create("jmhManaged");
              Configuration jmhConfiguration =
                  project.getConfigurations().getByName(JMHPlugin.getJMH_NAME());
              jmhConfiguration.extendsFrom(jmhManaged);

              // JMH plugin uses a detached configuration to build an uber-jar, which dependencyManagement
              // doesn't know about. Work around this by forcing parent configurations to be resolved and
              // added directly to the jmh configuration, which overwrites the otherwise unresolvable
              // dependency.
              project.afterEvaluate(
                  p -> {
                    jmhConfiguration
                        .getExtendsFrom()
                        .forEach(
                            parent -> {
                              parent
                                  .getResolvedConfiguration()
                                  .getFirstLevelModuleDependencies()
                                  .forEach(
                                      dep -> {
                                        project
                                            .getDependencies()
                                            .add(
                                                JMHPlugin.getJMH_NAME(),
                                                dep.getModule().toString());
                                      });
                            });
                  });
            });
  }

  private static void setupAptSourceSet(Project project, SourceSet sourceSet) {
    // HACK: Configurations usually use the same naming logic/scheme as for tasks
    Configuration aptConfiguration =
        project.getConfigurations().create(sourceSet.getTaskName("", "apt"));
    project
        .getConfigurations()
        .getByName(sourceSet.getTaskName("", "compileOnly"))
        .extendsFrom(aptConfiguration);

    File outputDir = project.file("build/generated/source/apt/" + sourceSet.getName());
    String outputDirPath = outputDir.getAbsolutePath();
    project
        .getTasks()
        .getByName(
            sourceSet.getCompileJavaTaskName(),
            t -> {
              JavaCompile task = (JavaCompile) t;
              task.getOptions().setAnnotationProcessorPath(aptConfiguration);
              task.getOptions().getCompilerArgs().addAll(ImmutableList.of("-s", outputDirPath));
              task.doFirst(
                  unused -> {
                    project.mkdir(outputDirPath);
                  });
            });

    project
        .getPlugins()
        .withType(
            IdeaPlugin.class,
            plugin -> {
              IdeaModule module = plugin.getModel().getModule();
              if (sourceSet.getName().equals("test")) {
                Set<File> testSrcDirs = module.getTestSourceDirs();
                testSrcDirs.add(outputDir);
                module.setTestSourceDirs(testSrcDirs);
              } else {
                Set<File> srcDirs = module.getSourceDirs();
                srcDirs.add(outputDir);
                module.setSourceDirs(srcDirs);
              }
              module.getGeneratedSourceDirs().add(outputDir);
            });
  }

  private static void addStandardJavaTestDependencies(Project project) {
    Configuration testConfiguration =
        project.getPlugins().hasPlugin(JavaLibraryPlugin.class)
            ? project
                .getConfigurations()
                .getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
            : project.getConfigurations().getByName(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME);
    DependencyHandler dependencies = project.getDependencies();

    dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, "com.google.code.findbugs:jsr305");
    dependencies.add(testConfiguration.getName(), "org.assertj:assertj-core");
    dependencies.add(testConfiguration.getName(), "junit:junit");
    dependencies.add(testConfiguration.getName(), "org.mockito:mockito-core");
  }

  private static Optional<Node> findChild(Node node, Predicate<Node> predicate) {
    // Should work.
    @SuppressWarnings("unchecked")
    List<Node> children = (List<Node>) node.children();
    return children.stream().filter(predicate).findFirst();
  }

  // Using groovy's XmlProvider from Java is very verbose, but this is the only time we need to, so
  // live with it.
  private static void addAnnotationProcessingXml(XmlProvider xml) {
    Node compilerConfiguration =
        findChild(
                xml.asNode(),
                node ->
                    node.name().equals("component")
                        && node.attribute("name").equals("CompilerConfiguration"))
            .orElseGet(
                () ->
                    xml.asNode()
                        .appendNode("component", ImmutableMap.of("name", "CompilerConfiguration")));
    findChild(compilerConfiguration, node -> node.name().equals("annotationProcessing"))
        .ifPresent(compilerConfiguration::remove);
    Node profile =
        compilerConfiguration
            .appendNode("annotationProcessing")
            .appendNode(
                "profile",
                ImmutableMap.of("name", "Default", "enabled", "true", "default", "true"));
    profile.appendNode("outputRelativeToContentRoot", ImmutableMap.of("value", "true"));
    profile.appendNode("processorPath", ImmutableMap.of("useClasspath", "true"));
    profile.appendNode("sourceOutputDir", ImmutableMap.of("name", "."));
    profile.appendNode("sourceTestOutputDir", ImmutableMap.of("name", "."));
  }
}
