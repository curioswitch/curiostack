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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static net.ltgt.gradle.errorprone.javacplugin.CheckSeverity.ERROR;
import static net.ltgt.gradle.errorprone.javacplugin.CheckSeverity.OFF;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;
import com.diffplug.gradle.spotless.SpotlessTask;
import com.github.benmanes.gradle.versions.VersionsPlugin;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.protobuf.gradle.ProtobufPlugin;
import com.google.protobuf.gradle.ProtobufSourceDirectorySet;
import com.jetbrains.python.envs.PythonEnvsExtension;
import com.jetbrains.python.envs.PythonEnvsPlugin;
import com.moowork.gradle.node.NodeExtension;
import com.moowork.gradle.node.NodePlugin;
import com.moowork.gradle.node.npm.NpmTask;
import com.moowork.gradle.node.task.NodeTask;
import com.moowork.gradle.node.yarn.YarnInstallTask;
import com.moowork.gradle.node.yarn.YarnTask;
import com.palantir.baseline.plugins.BaselineIdea;
import groovy.lang.Closure;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import me.champeau.gradle.JMHPlugin;
import me.champeau.gradle.JMHPluginExtension;
import nebula.plugin.resolutionrules.ResolutionRulesPlugin;
import net.ltgt.gradle.apt.AptIdeaPlugin;
import net.ltgt.gradle.apt.AptIdeaPlugin.ModuleAptConvention;
import net.ltgt.gradle.apt.AptPlugin;
import net.ltgt.gradle.errorprone.javacplugin.CheckSeverity;
import net.ltgt.gradle.errorprone.javacplugin.ErrorProneJavacPluginPlugin;
import net.ltgt.gradle.errorprone.javacplugin.ErrorProneOptions;
import nl.javadude.gradle.plugins.license.LicenseExtension;
import nl.javadude.gradle.plugins.license.LicensePlugin;
import nu.studer.gradle.jooq.JooqPlugin;
import nu.studer.gradle.jooq.JooqTask;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.curioswitch.gradle.common.LambdaClosure;
import org.curioswitch.gradle.plugins.ci.CurioGenericCiPlugin;
import org.curioswitch.gradle.plugins.curiostack.StandardDependencies.DependencySet;
import org.curioswitch.gradle.plugins.curiostack.tasks.CreateShellConfigTask;
import org.curioswitch.gradle.plugins.curiostack.tasks.SetupGitHooks;
import org.curioswitch.gradle.plugins.gcloud.GcloudPlugin;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.jvm.tasks.Jar;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.process.ExecSpec;

public class CuriostackPlugin implements Plugin<Project> {

  private static final String GOOGLE_JAVA_FORMAT_VERSION = "1.5";
  private static final String NODE_VERSION = "9.8.0";
  private static final String YARN_VERSION = "1.6.0";

  private static final Splitter NEW_LINE_SPLITTER = Splitter.on('\n');

  @Override
  public void apply(Project rootProject) {
    PluginContainer plugins = rootProject.getPlugins();
    // Provides useful tasks like 'clean', 'assemble' to the root project.
    plugins.apply(BasePlugin.class);

    plugins.apply(BaselineIdea.class);

    plugins.apply(CurioGenericCiPlugin.class);
    plugins.apply(GcloudPlugin.class);
    plugins.apply(NodePlugin.class);
    plugins.apply(PythonEnvsPlugin.class);

    setupPyenvs(rootProject);

    YarnInstallTask yarnTask =
        rootProject.getTasks().withType(YarnInstallTask.class).getByName("yarn");
    yarnTask.setArgs(ImmutableList.of("--frozen-lockfile"));
    YarnInstallTask yarnUpdateTask =
        rootProject.getTasks().create("yarnUpdate", YarnInstallTask.class);
    Task yarnWarning =
        rootProject
            .getTasks()
            .create(
                "yarnWarning",
                task -> {
                  task.onlyIf(unused -> yarnTask.getState().getFailure() != null);
                  task.doFirst(
                      unused ->
                          rootProject
                              .getLogger()
                              .warn(
                                  "yarn task failed. If you have updated a dependency and the "
                                      + "error says 'Your lockfile needs to be updated.', run \n\n"
                                      + "./gradlew "
                                      + yarnUpdateTask.getPath()));
                });
    yarnTask.finalizedBy(yarnWarning);

    rootProject.getTasks().create("setupGitHooks", SetupGitHooks.class);
    CreateShellConfigTask rehash =
        rootProject
            .getTasks()
            .create(
                "rehash",
                CreateShellConfigTask.class,
                t ->
                    t.path(CommandUtil.getPythonBinDir(rootProject, "dev"))
                        .path(CommandUtil.getGcloudSdkBinDir(rootProject)));

    setupNode(rootProject, rehash);

    rootProject
        .getTasks()
        .create(
            "setup",
            t -> {
              t.dependsOn("gcloudSetup");
              t.dependsOn("pythonSetup");
              t.dependsOn("nodeSetup");
              t.dependsOn("yarnSetup");
              t.dependsOn("rehash");
            });

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
                  for (String filePath : NEW_LINE_SPLITTER.split(baselineFiles)) {
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

    rootProject.afterEvaluate(
        (p) ->
            rootProject
                .getPlugins()
                .withType(
                    IdeaPlugin.class,
                    plugin -> {
                      plugin
                          .getModel()
                          .getProject()
                          .getIpr()
                          .withXml(provider -> setupProjectXml(rootProject, provider));
                      plugin
                          .getModel()
                          .getWorkspace()
                          .getIws()
                          .withXml(provider -> setupWorkspaceXml(rootProject, provider));
                    }));

    rootProject.allprojects(
        project -> {
          setupRepositories(project);

          project.getPlugins().apply(ResolutionRulesPlugin.class);

          project.getPlugins().withType(JavaPlugin.class, plugin -> setupJavaProject(project));

          project
              .getPlugins()
              .withType(
                  LicensePlugin.class,
                  unused -> {
                    LicenseExtension license =
                        project.getExtensions().getByType(LicenseExtension.class);
                    license.exclude("**/*.json");
                    license.setHeader(rootProject.file("LICENSE"));
                    license.mapping(
                        ImmutableMap.of(
                            "conf", "DOUBLESLASH_STYLE",
                            "java", "SLASHSTAR_STYLE",
                            "proto", "SLASHSTAR_STYLE",
                            "yml", "SCRIPT_STYLE"));
                  });
        });

    setupDataSources(rootProject);
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
    plugins.apply(AptPlugin.class);
    plugins.apply(AptIdeaPlugin.class);
    plugins.apply(BaselineIdea.class);
    plugins.apply(DependencyManagementPlugin.class);
    plugins.apply(ErrorProneJavacPluginPlugin.class);
    plugins.apply(LicensePlugin.class);
    plugins.apply(SpotlessPlugin.class);
    plugins.apply(VersionsPlugin.class);

    Map<String, CheckSeverity> checks =
        ImmutableMap.<String, CheckSeverity>builder()
            .put("BadComparable", ERROR)
            .put("BoxedPrimitiveConstructor", ERROR)
            .put("CannotMockFinalClass", OFF)
            .put("CatchFail", ERROR)
            .put("ClassCanBeStatic", ERROR)
            .put("ClassNewInstance", ERROR)
            .put("CollectionToArraySafeParameter", ERROR)
            .put("ComparableAndComparator", ERROR)
            .put("DateFormatConstant", ERROR)
            .put("DefaultCharset", ERROR)
            .put("DoubleCheckedLocking", ERROR)
            .put("EqualsIncompatibleType", ERROR)
            .put("FallThrough", ERROR)
            .put("Finally", ERROR)
            .put("FloatCast", ERROR)
            .put("FloatingPointLiteralPrecision", ERROR)
            .put("GetClassOnEnum", ERROR)
            .put("HidingField", ERROR)
            .put("ImmutableAnnotationChecker", ERROR)
            .put("ImmutableEnumChecker", ERROR)
            .put("IncrementInForLoopAndHeader", ERROR)
            .put("InjectOnConstructorOfAbstractClass", ERROR)
            .put("InputStreamSlowMultibyteRead", ERROR)
            .put("IntLongMath", ERROR)
            .put("IterableAndIterator", ERROR)
            .put("JavaLangClash", ERROR)
            .put("LogicalAssignment", ERROR)
            .put("MissingCasesInEnumSwitch", ERROR)
            .put("MissingOverride", ERROR)
            .put("ModifyCollectionInEnhancedForLoop", ERROR)
            .put("MultipleParallelOrSequentialCalls", ERROR)
            .put("MutableConstantField", OFF)
            .put("NarrowingCompoundAssignment", ERROR)
            .put("NestedInstanceOfConditions", ERROR)
            .put("NonAtomicVolatileUpdate", ERROR)
            .put("NonOverridingEquals", ERROR)
            .put("NullableConstructor", ERROR)
            .put("NullablePrimitive", ERROR)
            .put("NullableVoid", ERROR)
            .put("OptionalNotPresent", ERROR)
            .put("OverrideThrowableToString", ERROR)
            .put("PreconditionsInvalidPlaceholder", ERROR)
            .put("ProtoFieldPreconditionsCheckNotNull", ERROR)
            .put("ShortCircuitBoolean", ERROR)
            .put("StaticGuardedByInstance", ERROR)
            .put("StreamResourceLeak", ERROR)
            .put("StringSplitter", ERROR)
            .put("SynchronizeOnNonFinalField", ERROR)
            .put("ThreadJoinLoop", ERROR)
            .put("ThreadLocalUsage", ERROR)
            .put("ThreeLetterTimeZoneID", ERROR)
            .put("URLEqualsHashCode", ERROR)
            .put("UnsynchronizedOverridesSynchronized", ERROR)
            .put("WaitNotInLoop", ERROR)
            .put("AutoFactoryAtInject", ERROR)
            .put("ClassName", ERROR)
            .put("ComparisonContractViolated", ERROR)
            .put("DepAnn", ERROR)
            .put("DivZero", ERROR)
            .put("EmptyIf", ERROR)
            .put("FuzzyEqualsShouldNotBeUsedInEqualsMethod", ERROR)
            .put("InjectInvalidTargetingOnScopingAnnotation", ERROR)
            .put("InjectScopeAnnotationOnInterfaceOrAbstractClass", ERROR)
            .put("InsecureCryptoUsage", ERROR)
            .put("IterablePathParameter", ERROR)
            .put("LongLiteralLowerCaseSuffix", ERROR)
            .put("NumericEquality", ERROR)
            .put("ParameterPackage", ERROR)
            .put("ProtoStringFieldReferenceEquality", ERROR)
            .put("AssistedInjectAndInjectOnConstructors", ERROR)
            .put("BigDecimalLiteralDouble", ERROR)
            .put("ConstructorLeaksThis", ERROR)
            .put("InconsistentOverloads", ERROR)
            .put("MissingDefault", ERROR)
            .put("PrimitiveArrayPassedToVarargsMethod", ERROR)
            .put("RedundantThrows", ERROR)
            .put("StaticQualifiedUsingExpression", ERROR)
            .put("StringEquality", ERROR)
            .put("TestExceptionChecker", ERROR)
            .put("FieldMissingNullable", ERROR)
            .put("LambdaFunctionalInterface", ERROR)
            .put("MethodCanBeStatic", ERROR)
            .put("MixedArrayDimensions", ERROR)
            .put("MultiVariableDeclaration", ERROR)
            .put("MultipleTopLevelClasses", ERROR)
            .put("MultipleUnaryOperatorsInMethodCall", ERROR)
            .put("PackageLocation", ERROR)
            .put("ParameterComment", ERROR)
            .put("ParameterNotNullable", ERROR)
            .put("PrivateConstructorForUtilityClass", ERROR)
            .put("RemoveUnusedImports", ERROR)
            .put("ReturnMissingNullable", ERROR)
            .put("SwitchDefault", ERROR)
            .put("ThrowsUncheckedException", ERROR)
            .put("UngroupedOverloads", ERROR)
            .put("UnnecessaryStaticImport", ERROR)
            .put("UseBinds", ERROR)
            .put("WildcardImport", ERROR)
            .build();

    project
        .getTasks()
        .withType(
            JavaCompile.class,
            task -> {
              task.getOptions().setIncremental(true);
              task.getOptions().setCompilerArgs(ImmutableList.of("-XDcompilePolicy=byfile"));
              project
                  .getTasks()
                  .withType(SpotlessTask.class, spotlessTask -> spotlessTask.dependsOn(task));

              ErrorProneOptions errorProne =
                  ((ExtensionAware) task.getOptions())
                      .getExtensions()
                      .findByType(ErrorProneOptions.class);
              if (errorProne != null) {
                errorProne.setDisableWarningsInGeneratedCode(true);
                errorProne.setExcludedPaths("(.*/build/.*|.*/gen-src/.*)");
                errorProne.setChecks(checks);
              }
            });

    JavaPluginConvention javaPlugin = project.getConvention().getPlugin(JavaPluginConvention.class);
    javaPlugin.setSourceCompatibility(JavaVersion.VERSION_1_10);
    javaPlugin.setTargetCompatibility(JavaVersion.VERSION_1_10);

    Test test = project.getTasks().withType(Test.class).getByName("test");
    if (project.getRootProject().hasProperty("updateSnapshots")) {
      test.jvmArgs(ImmutableList.of("-Dorg.curioswitch.testing.updateSnapshots=true"));
    }
    test.useJUnitPlatform(platform -> platform.includeEngines("junit-jupiter", "junit-vintage"));

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

              project
                  .getTasks()
                  .getByName("clean")
                  .doLast(unused -> project.file(project.getName() + ".iml").delete());

              new DslObject(module)
                  .getConvention()
                  .getPlugin(ModuleAptConvention.class)
                  .getApt()
                  .setAddAptDependencies(false);
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

    // Pretty much all java code needs at least the Generated annotation.
    project
        .getDependencies()
        .add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "javax.annotation:javax.annotation-api");
    project
        .getDependencies()
        .add(
            ErrorProneJavacPluginPlugin.CONFIGURATION_NAME,
            "com.google.errorprone:error_prone_core");
    project.afterEvaluate(CuriostackPlugin::addStandardJavaTestDependencies);

    project
        .getConfigurations()
        .all(
            configuration -> {
              configuration.resolutionStrategy(ResolutionStrategy::preferProjectModules);
              configuration.exclude(
                  ImmutableMap.of("group", "com.google.guava", "module", "guava-jdk5"));
            });

    Javadoc javadoc = (Javadoc) project.getTasks().getByName("javadoc");
    CoreJavadocOptions options = (CoreJavadocOptions) javadoc.getOptions();
    options.quiet();
    options.addBooleanOption("Xdoclint:all,-missing", true);

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

    SourceSetContainer sourceSets = javaPlugin.getSourceSets();
    project
        .getTasks()
        .create(
            "sourceJar",
            Jar.class,
            sourceJar -> {
              sourceJar.setClassifier("sources");
              sourceJar.from(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getAllSource());
            });

    SpotlessExtension spotless = project.getExtensions().getByType(SpotlessExtension.class);
    spotless.java((extension) -> extension.googleJavaFormat(GOOGLE_JAVA_FORMAT_VERSION));

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
              // Benchmarks are usually very small and converge quickly. If this stops being the
              // case
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
              // but should be resolved by our managed dependencies. We need a separate
              // configuration
              // to be able to provide the resolution workaround described below.
              Configuration jmhManaged = project.getConfigurations().create("jmhManaged");
              Configuration jmhConfiguration = project.getConfigurations().getByName("jmh");
              jmhConfiguration.extendsFrom(jmhManaged);

              // JMH plugin uses a detached configuration to build an uber-jar, which
              // dependencyManagement
              // doesn't know about. Work around this by forcing parent configurations to be
              // resolved and
              // added directly to the jmh configuration, which overwrites the otherwise
              // unresolvable
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
                                            .add("jmh", dep.getModule().toString());
                                      });
                            });
                  });
            });

    project
        .getPlugins()
        .withType(
            JooqPlugin.class,
            unused -> {
              project
                  .getTasks()
                  .withType(
                      JooqTask.class,
                      t -> {
                        for (String dependency :
                            ImmutableList.of(
                                "javax.activation:activation",
                                "javax.xml.bind:jaxb-api",
                                "com.sun.xml.bind:jaxb-core",
                                "com.sun.xml.bind:jaxb-impl",
                                "mysql:mysql-connector-java",
                                // Not sure why this isn't automatically added.
                                "com.google.guava:guava",
                                "com.google.cloud.sql:mysql-socket-factory")) {
                          project.getDependencies().add("jooqRuntime", dependency);
                        }
                      });
            });

    // It is very common to want to pass in command line system properties to the binary, so just
    // always forward properties. It won't affect production since no one runs binaries via Gradle
    // in production.
    project
        .getTasks()
        .withType(
            JavaExec.class,
            task ->
                System.getProperties()
                    .entrySet()
                    .stream()
                    // IntelliJ property which doesn't work with Java9.
                    .filter(entry -> !entry.getKey().equals("java.endorsed.dirs"))
                    .forEach(
                        entry -> task.systemProperty((String) entry.getKey(), entry.getValue())));
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
    dependencies.add(
        testConfiguration.getName(), "org.curioswitch.curiostack:curio-testing-framework");
    dependencies.add(testConfiguration.getName(), "org.assertj:assertj-core");
    dependencies.add(testConfiguration.getName(), "org.awaitility:awaitility");
    dependencies.add(testConfiguration.getName(), "junit:junit");
    dependencies.add(testConfiguration.getName(), "org.mockito:mockito-core");
    dependencies.add(testConfiguration.getName(), "info.solidsoft.mockito:mockito-java8");

    dependencies.add(testConfiguration.getName(), "org.junit.jupiter:junit-jupiter-api");
    dependencies.add(
        JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.jupiter:junit-jupiter-engine");
    dependencies.add(
        JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.vintage:junit-vintage-engine");
  }

  private static void setupDataSources(Project project) {
    // TODO(choko): Preconfigure XML as well
    Configuration configuration = project.getConfigurations().create("jdbcDrivers");
    project
        .getDependencies()
        .add(configuration.getName(), "com.google.cloud.sql:mysql-socket-factory:1.0.5");
    project
        .getTasks()
        .create(
            "setupDataSources",
            Copy.class,
            t -> {
              t.from(configuration);
              t.into(".ideaDataSources/drivers");
            });
  }

  private static void setupNode(Project project, CreateShellConfigTask rehash) {
    NodeExtension node = project.getExtensions().getByType(NodeExtension.class);
    node.setVersion(NODE_VERSION);
    node.setYarnVersion(YARN_VERSION);
    node.setDownload(true);

    Closure<?> pathOverrider =
        LambdaClosure.of(
            (ExecSpec exec) -> {
              if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
                String actualCommand =
                    exec.getExecutable() + " " + String.join(" ", exec.getArgs());
                exec.setCommandLine(
                    "bash",
                    "-c",
                    ". "
                        + CommandUtil.getCondaBaseDir(project)
                            .resolve("etc/profile.d/conda.sh")
                            .toString()
                        + " && conda activate > /dev/null && cd "
                        + exec.getWorkingDir().toString()
                        + " && "
                        + actualCommand);
              }
              exec.getEnvironment()
                  .put(
                      "PATH",
                      CommandUtil.getPythonBinDir(project, "build")
                          + File.pathSeparator
                          + exec.getEnvironment().get("PATH"));
            });

    project.getTasks().withType(NodeTask.class, t -> t.setExecOverrides(pathOverrider));
    project.getTasks().withType(NpmTask.class, t -> t.setExecOverrides(pathOverrider));
    project.getTasks().withType(YarnTask.class, t -> t.setExecOverrides(pathOverrider));

    node.setWorkDir(CommandUtil.getNodeDir(project).toFile());
    node.setNpmWorkDir(CommandUtil.getNpmDir(project).toFile());
    node.setYarnWorkDir(CommandUtil.getYarnDir(project).toFile());

    project.afterEvaluate(
        n -> {
          rehash.path(node.getVariant().getNodeBinDir().toPath());
          rehash.path(node.getVariant().getYarnBinDir().toPath());
          rehash.path(node.getVariant().getNpmBinDir().toPath());
        });

    project
        .getTasks()
        .getByName(
            "nodeSetup",
            t -> {
              t.dependsOn("pythonSetup");
              t.onlyIf(unused -> !node.getVariant().getNodeDir().exists());
            });
    project
        .getTasks()
        .findByName("yarnSetup")
        .onlyIf(t -> !node.getVariant().getYarnDir().exists());

    // Since yarn is very fast, go ahead and clean node_modules too to prevent
    // inconsistency.
    project.getPluginManager().apply(BasePlugin.class);
    project
        .getTasks()
        .getByName(
            BasePlugin.CLEAN_TASK_NAME,
            task -> ((Delete) task).delete(project.file("node_modules")));
  }

  private static void setupPyenvs(Project rootProject) {
    PythonEnvsExtension envs = rootProject.getExtensions().getByType(PythonEnvsExtension.class);

    Path pythonDir = CommandUtil.getPythonDir(rootProject);

    envs.setBootstrapDirectory(pythonDir.resolve("bootstrap").toFile());
    envs.setEnvsDirectory(pythonDir.resolve("envs").toFile());

    ImmutableList.Builder<String> condaPackages =
        ImmutableList.<String>builder().add("git").add("automake").add("autoconf").add("make");
    if (Os.isFamily(Os.FAMILY_MAC)) {
      condaPackages.add("clang_osx-64", "clangxx_osx-64", "gfortran_osx-64");
    } else if (Os.isFamily(Os.FAMILY_UNIX)) {
      condaPackages.add("gcc_linux-64", "gxx_linux-64", "gfortran_linux-64");
    }

    envs.conda(
        "miniconda2",
        "Miniconda2-4.4.10",
        condaPackages.build().stream().map(envs::condaPackage).collect(toImmutableList()));
    envs.condaenv("build", "2.7", "miniconda2");
    envs.condaenv("dev", "2.7", "miniconda2");

    rootProject.getTasks().create("pythonSetup", t -> t.dependsOn("build_envs"));
  }

  private static Optional<Node> findChild(Node node, Predicate<Node> predicate) {
    // Should work.
    @SuppressWarnings("unchecked")
    List<Node> children = (List<Node>) node.children();
    return children.stream().filter(predicate).findFirst();
  }

  private static Node findOrCreateChild(Node parent, String type, String name) {
    Map<String, String> attributes = new HashMap<>();
    attributes.put("name", name);
    return findChild(
            parent, node -> node.name().equals(type) && node.attribute("name").equals(name))
        .orElseGet(() -> parent.appendNode(type, attributes));
  }

  private static void setOption(Node component, String name, String value) {
    findOrCreateChild(component, "option", name).attributes().put("value", value);
  }

  private static void setProperty(Node component, String name, String value) {
    findOrCreateChild(component, "property", name).attributes().put("value", value);
  }

  private static void setupProjectXml(Project project, XmlProvider xml) {
    NodeExtension nodeConfig = project.getExtensions().getByType(NodeExtension.class);
    Node typescriptCompiler = findOrCreateChild(xml.asNode(), "component", "TypeScriptCompiler");
    setOption(
        typescriptCompiler,
        "typeScriptServiceDirectory",
        project.file("node_modules/typescript").getAbsolutePath());
    setOption(
        typescriptCompiler, "nodeInterpreterTextField", nodeConfig.getVariant().getNodeExec());
    setOption(typescriptCompiler, "versionType", "SERVICE_DIRECTORY");

    Node angularComponent = findOrCreateChild(xml.asNode(), "component", "AngularJSSettings");
    setOption(angularComponent, "useService", "false");

    Node inspectionManager =
        findOrCreateChild(xml.asNode(), "component", "InspectionProjectProfileManager");
    Node profile =
        findChild(inspectionManager, n -> n.name().equals("profile"))
            .orElseGet(
                () -> inspectionManager.appendNode("profile", ImmutableMap.of("version", "1.0")));
    setOption(profile, "myName", "Project Default");
    findChild(
            profile,
            n -> n.name().equals("inspection_tool") && "TsLint".equals(n.attribute("class")))
        .orElseGet(
            () ->
                profile.appendNode(
                    "inspection_tool",
                    ImmutableMap.of(
                        "class",
                        "TsLint",
                        "enabled",
                        "true",
                        "level",
                        "ERROR",
                        "enabled_by_default",
                        "true")));
  }

  private static void setupWorkspaceXml(Project project, XmlProvider xml) {
    NodeExtension nodeConfig = project.getExtensions().getByType(NodeExtension.class);
    Node properties = findOrCreateChild(xml.asNode(), "component", "PropertiesComponent");
    setProperty(
        properties, "node.js.path.for.package.tslint", nodeConfig.getVariant().getNodeExec());
    setProperty(properties, "node.js.detected.package.tslint", "true");
    setProperty(
        properties,
        "node.js.selected.package.tslint",
        project.file("node_modules/tslint").getAbsolutePath());
    setProperty(properties, "typescript-compiler-editor-notification", "false");
  }
}
