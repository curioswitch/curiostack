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

package org.curioswitch.gradle.plugins.curiostack;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static net.ltgt.gradle.errorprone.CheckSeverity.ERROR;
import static net.ltgt.gradle.errorprone.CheckSeverity.OFF;
import static net.ltgt.gradle.errorprone.CheckSeverity.WARN;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;
import com.github.benmanes.gradle.versions.VersionsPlugin;
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import groovy.util.Node;
import groovy.util.XmlParser;
import groovy.xml.QName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import me.champeau.gradle.JMHPlugin;
import me.champeau.gradle.JMHPluginExtension;
import net.ltgt.gradle.errorprone.CheckSeverity;
import net.ltgt.gradle.errorprone.ErrorProneOptions;
import net.ltgt.gradle.errorprone.ErrorPronePlugin;
import net.ltgt.gradle.nullaway.NullAwayOptions;
import net.ltgt.gradle.nullaway.NullAwayPlugin;
import nu.studer.gradle.jooq.JooqPlugin;
import nu.studer.gradle.jooq.JooqTask;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.curioswitch.gradle.conda.CondaBuildEnvPlugin;
import org.curioswitch.gradle.conda.CondaExtension;
import org.curioswitch.gradle.conda.CondaPlugin;
import org.curioswitch.gradle.golang.GolangExtension;
import org.curioswitch.gradle.golang.GolangPlugin;
import org.curioswitch.gradle.golang.GolangSetupPlugin;
import org.curioswitch.gradle.golang.tasks.JibTask;
import org.curioswitch.gradle.plugins.aws.AwsSetupPlugin;
import org.curioswitch.gradle.plugins.ci.CurioGenericCiPlugin;
import org.curioswitch.gradle.plugins.curiostack.tasks.CreateShellConfigTask;
import org.curioswitch.gradle.plugins.curiostack.tasks.GenerateApiServerTask;
import org.curioswitch.gradle.plugins.curiostack.tasks.SetupGitHooks;
import org.curioswitch.gradle.plugins.curiostack.tasks.UpdateGradleWrapperTask;
import org.curioswitch.gradle.plugins.curiostack.tasks.UpdateIntelliJSdksTask;
import org.curioswitch.gradle.plugins.curiostack.tasks.UpdateProjectSettingsTask;
import org.curioswitch.gradle.plugins.gcloud.GcloudPlugin;
import org.curioswitch.gradle.plugins.nodejs.NodePlugin;
import org.curioswitch.gradle.plugins.nodejs.util.NodeUtil;
import org.curioswitch.gradle.plugins.terraform.TerraformSetupPlugin;
import org.curioswitch.gradle.release.ReleasePlugin;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.VariantVersionMappingStrategy;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;
import org.gradle.api.tasks.wrapper.Wrapper;
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.internal.deprecation.DeprecatableConfiguration;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;
import org.xml.sax.SAXException;

public class CuriostackRootPlugin implements Plugin<Project> {

  private static final Map<String, CheckSeverity> ERROR_PRONE_CHECKS =
      ImmutableMap.<String, CheckSeverity>builder()
          .put("BadComparable", ERROR)
          .put("BoxedPrimitiveConstructor", ERROR)
          .put("CannotMockFinalClass", OFF)
          .put("CatchFail", ERROR)
          .put("ClassCanBeStatic", ERROR)
          .put("ClassNewInstance", ERROR)
          .put("CloseableProvides", OFF)
          .put("CollectionToArraySafeParameter", ERROR)
          .put("ComparableAndComparator", ERROR)
          .put("DateFormatConstant", ERROR)
          .put("DefaultCharset", ERROR)
          .put("DoubleCheckedLocking", ERROR)
          .put("EqualsIncompatibleType", ERROR)
          .put("FallThrough", ERROR)
          .put("FieldCanBeStatic", ERROR)
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
          .put("LockOnBoxedPrimitive", ERROR)
          .put("LogicalAssignment", ERROR)
          .put("MissingCasesInEnumSwitch", ERROR)
          .put("MissingOverride", ERROR)
          .put("ModifyCollectionInEnhancedForLoop", ERROR)
          .put("MultipleParallelOrSequentialCalls", ERROR)
          .put("MutableConstantField", OFF)
          .put("MutablePublicArray", ERROR)
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
          .put("ProtectedMembersInFinalClass", ERROR)
          .put("PublicConstructorForAbstractClass", ERROR)
          .put("ShortCircuitBoolean", ERROR)
          .put("StaticAssignmentInConstructor", ERROR)
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
          .put("ConstantPatternCompile", ERROR)
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
          .put("PrivateConstructorForUtilityClass", ERROR)
          // Handled by spotless which also allows fixing it.
          .put("RemoveUnusedImports", OFF)
          .put("SwitchDefault", ERROR)
          .put("ThrowsUncheckedException", ERROR)
          .put("UngroupedOverloads", ERROR)
          .put("UnnecessaryStaticImport", ERROR)
          .put("UseBinds", ERROR)
          .put("WildcardImport", ERROR)
          // TODO(choko): Remove when works on Java 13
          .put("TypeParameterUnusedInFormals", OFF)
          .put("BadImport", OFF)
          .put("JavaTimeDefaultTimeZone", OFF)
          .build();

  private static final Set<String> UNMANAGED_CONFIGURATIONS =
      ImmutableSet.of(JacocoPlugin.AGENT_CONFIGURATION_NAME, JacocoPlugin.ANT_CONFIGURATION_NAME);

  private static final Pattern IS_STABLE_VERSION = Pattern.compile("^[0-9,.v-]+(-r)?$");

  @Override
  public void apply(Project rootProject) {
    rootProject
        .getGradle()
        .getTaskGraph()
        .whenReady(
            tasks -> {
              if (!rootProject
                      .getGradle()
                      .getGradleVersion()
                      .equals(ToolDependencies.getGradleVersion(rootProject))
                  && !tasks.hasTask(":wrapper")) {
                throw new IllegalStateException(
                    "Gradle wrapper out-of-date, run ./gradlew :wrapper");
              }
            });

    PluginContainer plugins = rootProject.getPlugins();
    // Provides useful tasks like 'clean', 'assemble' to the root project.
    plugins.apply(BasePlugin.class);

    plugins.apply(AwsSetupPlugin.class);
    plugins.apply(CondaBuildEnvPlugin.class);
    plugins.apply(CurioGenericCiPlugin.class);
    plugins.apply(GcloudPlugin.class);
    plugins.apply(IdeaPlugin.class);
    plugins.apply(NodePlugin.class);
    plugins.apply(ReleasePlugin.class);
    plugins.apply(TerraformSetupPlugin.class);
    plugins.apply(ToolDownloaderPlugin.class);

    var updateGradleWrapper =
        rootProject.getTasks().register("curioUpdateWrapper", UpdateGradleWrapperTask.class);

    rootProject
        .getTasks()
        .withType(Wrapper.class)
        .configureEach(
            wrapper -> {
              wrapper.setGradleVersion(ToolDependencies.getGradleVersion(rootProject));
              wrapper.setDistributionType(DistributionType.ALL);

              wrapper.finalizedBy(updateGradleWrapper);
            });

    rootProject.getTasks().register("setupGitHooks", SetupGitHooks.class);
    var updateShellConfig =
        rootProject.getTasks().register("updateShellConfig", CreateShellConfigTask.class);
    DownloadToolUtil.getDownloadTask(rootProject, "miniconda-build")
        .configure(t -> t.finalizedBy(updateShellConfig));
    rootProject
        .getTasks()
        .named("condaInstallPackagesMinicondaBuild", t -> t.mustRunAfter(updateShellConfig));

    var updateIntelliJJdks =
        rootProject
            .getTasks()
            .register(
                UpdateIntelliJSdksTask.NAME,
                UpdateIntelliJSdksTask.class,
                t -> t.dependsOn(updateGradleWrapper));

    var idea = rootProject.getTasks().named("idea");
    idea.configure(task -> task.dependsOn(updateIntelliJJdks));

    rootProject
        .getTasks()
        .register(
            "setup",
            t -> {
              t.dependsOn(idea);
              t.dependsOn(rootProject.getTasks().named("toolsSetupAll"));
              t.dependsOn(updateShellConfig);
            });

    var updateProjectSettings =
        rootProject.getTasks().register("updateProjectSettings", UpdateProjectSettingsTask.class);
    rootProject
        .getTasks()
        .register(
            "generateApiServer",
            GenerateApiServerTask.class,
            t -> t.finalizedBy(updateProjectSettings));

    rootProject.afterEvaluate(
        (p) ->
            rootProject
                .getPlugins()
                .withType(
                    IdeaPlugin.class,
                    plugin -> {
                      var ipr = plugin.getModel().getProject().getIpr();
                      ipr.whenMerged(
                          unused ->
                              ipr.withXml(provider -> setupProjectXml(rootProject, provider)));
                      var iws = plugin.getModel().getWorkspace().getIws();
                      iws.whenMerged(
                          unused ->
                              iws.withXml(provider -> setupWorkspaceXml(rootProject, provider)));
                    }));

    rootProject.allprojects(
        project -> {
          project
              .getPlugins()
              .withType(JavaPlugin.class, plugin -> setupJavaProject(project, ERROR_PRONE_CHECKS));

          project
              .getPlugins()
              .withType(
                  VersionsPlugin.class,
                  unused -> {
                    project
                        .getTasks()
                        .named(
                            "dependencyUpdates",
                            DependencyUpdatesTask.class,
                            task -> {
                              task.setRevision("release");
                              task.setCheckConstraints(true);

                              task.rejectVersionIf(
                                  filter -> {
                                    String version = filter.getCandidate().getVersion();
                                    String uppercaseVersion = version.toUpperCase(Locale.ROOT);
                                    if (uppercaseVersion.contains("RELEASE")
                                        || uppercaseVersion.contains("FINAL")
                                        || uppercaseVersion.contains("GA")) {
                                      return false;
                                    }
                                    if (IS_STABLE_VERSION.matcher(version).matches()) {
                                      return false;
                                    }
                                    if (version.endsWith("-jre")) {
                                      // Guava
                                      return false;
                                    }
                                    return true;
                                  });
                            });
                  });

          project
              .getPlugins()
              .withType(
                  GolangPlugin.class,
                  unused -> {
                    project
                        .getExtensions()
                        .getByType(GolangExtension.class)
                        .jib(
                            jib ->
                                jib.getCredentialHelper()
                                    .set(
                                        DownloadedToolManager.get(project)
                                            .getBinDir("gcloud")
                                            .resolve("docker-credential-gcr")));

                    project
                        .getTasks()
                        .withType(JibTask.class)
                        .configureEach(
                            t ->
                                t.dependsOn(
                                    project.getRootProject().getTasks().getByName("gcloudSetup")));
                  });
        });

    setupDataSources(rootProject);

    rootProject
        .getPlugins()
        .withType(
            GolangSetupPlugin.class,
            unused ->
                rootProject
                    .getPlugins()
                    .withType(
                        ToolDownloaderPlugin.class,
                        plugin ->
                            plugin
                                .tools()
                                .named("go")
                                .configure(
                                    tool ->
                                        tool.getVersion()
                                            .set(ToolDependencies.getGolangVersion(rootProject)))));

    rootProject
        .getPlugins()
        .withType(
            CondaPlugin.class,
            plugin ->
                plugin
                    .getCondas()
                    .withType(CondaExtension.class)
                    .named("miniconda-build")
                    .configure(
                        conda ->
                            conda
                                .getVersion()
                                .set(ToolDependencies.getMinicondaVersion(rootProject))));

    setupSpotless(rootProject);
  }

  private static void setupRepositories(Project project) {
    project.getRepositories().gradlePluginPortal();
    project.getRepositories().mavenCentral();
  }

  private static void setupJavaProject(
      Project project, Map<String, CheckSeverity> errorProneChecks) {
    setupRepositories(project);

    PluginContainer plugins = project.getPlugins();
    plugins.apply(ErrorPronePlugin.class);
    plugins.apply(IdeaPlugin.class);
    plugins.apply(NullAwayPlugin.class);
    plugins.apply(VersionsPlugin.class);

    var java = project.getExtensions().getByType(JavaPluginExtension.class);
    java.withJavadocJar();
    java.withSourcesJar();

    // Manage all dependencies by adding the bom as a platform.
    Object bomDependency =
        isCuriostack(project)
            ? project.getDependencies().project(ImmutableMap.of("path", ":tools:curiostack-bom"))
            : "org.curioswitch.curiostack:curiostack-bom:"
                + ToolDependencies.getBomVersion(project);

    var platformDependency = project.getDependencies().platform(bomDependency);

    // Needs to be in afterEvaluate since there is no way to guarantee isCanBeResolved, etc
    // are set otherwise.
    project
        .getConfigurations()
        .configureEach(
            configuration -> {
              if (configuration instanceof DeprecatableConfiguration) {
                if (((DeprecatableConfiguration) configuration).getDeclarationAlternatives()
                    != null) {
                  return;
                }
              }
              if (!configuration.getName().endsWith("Classpath")
                  && !UNMANAGED_CONFIGURATIONS.contains(configuration.getName())) {
                project.getDependencies().add(configuration.getName(), platformDependency);
              }
            });

    project.afterEvaluate(
        unused -> {
          plugins.withType(
              MavenPublishPlugin.class,
              unused2 -> {
                var publishing = project.getExtensions().getByType(PublishingExtension.class);
                publishing
                    .getPublications()
                    .configureEach(
                        publication -> {
                          if (!(publication instanceof MavenPublication)) {
                            return;
                          }
                          var mavenPublication = (MavenPublication) publication;

                          mavenPublication.versionMapping(
                              mapping ->
                                  mapping.allVariants(
                                      VariantVersionMappingStrategy::fromResolutionResult));

                          mavenPublication
                              .getPom()
                              .withXml(
                                  xml -> {
                                    var root = xml.asNode();
                                    findChild(root, "dependencyManagement").ifPresent(root::remove);
                                  });
                        });
              });
        });

    project
        .getTasks()
        .withType(JavaCompile.class)
        .configureEach(
            task -> {
              task.getOptions().setIncremental(true);

              ErrorProneOptions errorProne =
                  ((ExtensionAware) task.getOptions())
                      .getExtensions()
                      .findByType(ErrorProneOptions.class);
              if (errorProne != null) {
                errorProne.getDisableWarningsInGeneratedCode().set(true);
                errorProne.getIgnoreUnknownCheckNames().set(true);
                errorProne.getExcludedPaths().set("(.*/build/.*|.*/gen-src/.*)");
                errorProne.getChecks().set(errorProneChecks);

                var nullaway =
                    ((ExtensionAware) errorProne).getExtensions().getByType(NullAwayOptions.class);
                nullaway.getSeverity().set(WARN);
                nullaway
                    .getExcludedFieldAnnotations()
                    .addAll(MonotonicNonNull.class.getCanonicalName(), "org.mockito.Mock");
              }
            });

    JavaPluginConvention javaPlugin = project.getConvention().getPlugin(JavaPluginConvention.class);
    javaPlugin.setSourceCompatibility(JavaVersion.VERSION_16);
    javaPlugin.setTargetCompatibility(JavaVersion.VERSION_16);

    // Even for libraries that set source version to 8/11 for compatibility with older runtimes,
    // we always use 15 for tests.
    var testSourceSet = javaPlugin.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
    project
        .getConfigurations()
        .getByName(testSourceSet.getCompileClasspathConfigurationName())
        .getAttributes()
        .attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 16);
    project
        .getConfigurations()
        .getByName(testSourceSet.getRuntimeClasspathConfigurationName())
        .getAttributes()
        .attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 16);

    project
        .getTasks()
        .withType(Test.class)
        .named("test")
        .configure(
            test -> {
              if (project.getRootProject().hasProperty("updateSnapshots")) {
                test.jvmArgs(ImmutableList.of("-Dorg.curioswitch.testing.updateSnapshots=true"));
              }
              test.useJUnitPlatform(
                  platform -> platform.includeEngines("junit-jupiter", "junit-vintage"));
              test.testLogging(
                  logging -> {
                    logging.setShowStandardStreams(true);
                    logging.setExceptionFormat(TestExceptionFormat.FULL);
                  });
            });

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
                  .named("cleanIdea")
                  .configure(
                      t -> t.doLast(unused -> project.file(project.getName() + ".iml").delete()));
            });

    // Pretty much all java code needs at least the Generated annotation.
    project
        .getDependencies()
        .add(
            JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
            "jakarta.annotation:jakarta.annotation-api");
    project
        .getDependencies()
        .add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, "org.checkerframework:checker-qual");
    project
        .getDependencies()
        .add(ErrorPronePlugin.CONFIGURATION_NAME, "com.google.errorprone:error_prone_core");
    project
        .getDependencies()
        .add(ErrorPronePlugin.CONFIGURATION_NAME, "com.uber.nullaway:nullaway");
    project
        .getDependencies()
        .add(ErrorPronePlugin.CONFIGURATION_NAME, "com.google.auto.value:auto-value-annotations");

    project.afterEvaluate(CuriostackRootPlugin::addStandardJavaTestDependencies);

    project
        .getConfigurations()
        .all(
            configuration -> {
              configuration.resolutionStrategy(ResolutionStrategy::preferProjectModules);
              configuration.exclude(
                  ImmutableMap.of("group", "com.google.guava", "module", "guava-jdk5"));
            });

    var javadoc = project.getTasks().withType(Javadoc.class).named("javadoc");
    javadoc.configure(
        t -> {
          CoreJavadocOptions options = (CoreJavadocOptions) t.getOptions();
          options.quiet();
          options.addBooleanOption("Xdoclint:all,-missing", true);
        });

    project
        .getTasks()
        .register(
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
            });

    project
        .getPlugins()
        .withType(
            JooqPlugin.class,
            unused ->
                project
                    .getTasks()
                    .withType(JooqTask.class)
                    .configureEach(
                        t -> {
                          for (String dependency :
                              ImmutableList.of(
                                  "javax.activation:activation",
                                  "mysql:mysql-connector-java",
                                  // Not sure why this isn't automatically added.
                                  "com.google.guava:guava",
                                  "com.google.cloud.sql:mysql-socket-factory")) {
                            project.getDependencies().add("jooqRuntime", dependency);
                          }
                        }));

    // It is very common to want to pass in command line system properties to the binary, so just
    // always forward properties. It won't affect production since no one runs binaries via Gradle
    // in production.
    project
        .getTasks()
        .withType(JavaExec.class)
        .configureEach(
            task ->
                System.getProperties().entrySet().stream()
                    // Don't pass JRE properties.
                    .filter(entry -> !((String) entry.getKey()).startsWith("java."))
                    .forEach(
                        entry -> task.systemProperty((String) entry.getKey(), entry.getValue())));
  }

  private static void setupSpotless(Project rootProject) {
    List<String> copyrightLines;
    try (var files = Files.list(rootProject.file(".baseline/copyright").toPath())) {
      copyrightLines = Files.readAllLines(files.findFirst().get());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    copyrightLines =
        copyrightLines.stream()
            .map(line -> line.replace("${today.year}", "$today.year"))
            .collect(toImmutableList());

    String copyrightSlashStar =
        copyrightLines.stream()
            .map(line -> line.isEmpty() ? " *" : " * " + line)
            .collect(Collectors.joining("\n", "/*\n", "\n */\n\n"));

    String copyrightDoubleSlash =
        copyrightLines.stream()
            .map(line -> line.isEmpty() ? "//" : "// " + line)
            .collect(Collectors.joining("\n", "", "\n\n"));

    String copyrightSharp =
        copyrightLines.stream()
            .map(line -> line.isEmpty() ? "#" : "# " + line)
            .collect(Collectors.joining("\n", "", "\n\n"));

    rootProject.subprojects(
        project -> {
          // Don't apply spotless to intermediate projects, CurioStack currently doesn't have a use
          // case for it and it allows globs like "**/*.ext"
          if (!project.getChildProjects().isEmpty()) {
            return;
          }

          project.getPlugins().apply(SpotlessPlugin.class);

          SpotlessExtension spotless = project.getExtensions().getByType(SpotlessExtension.class);

          project
              .getPlugins()
              .withId(
                  "java",
                  unused -> {
                    spotless.java(
                        (java) -> {
                          java.targetExclude("build/**");
                          java.googleJavaFormat(
                              ToolDependencies.getGoogleJavaFormatVersion(project));
                          java.licenseHeader(
                              copyrightSlashStar, "package |// End License|// Includes work from:");
                        });
                  });

          spotless.typescript(
              typescript -> {
                typescript.targetExclude("node_modules/**");
                typescript.target("**/*.ts", "**/*.js", "**/*.tsx", "**/*.jsx");

                typescript.licenseHeader(
                    copyrightSlashStar,
                    "import|const|declare|export|var|module|/\\* eslint|// eslint|// End License|// Includes work from:|it\\(|/\\* global|#!|type ");
              });

          spotless.format(
              "go",
              go -> {
                go.target("**/*.go");

                go.licenseHeader(
                    copyrightDoubleSlash,
                    "package|// \\+|// -|//go|// End License|// Includes work from:");
              });

          spotless.format(
              "proto",
              go -> {
                go.target("**/*.proto");

                go.licenseHeader(
                    copyrightDoubleSlash, "syntax|// End License|// Includes work from:");
              });

          spotless.format(
              "conf",
              conf -> {
                conf.target("**/*.conf");

                conf.licenseHeader(
                    copyrightDoubleSlash, "[a-zA-Z0-9]|// End License|// Includes work from:");
              });

          spotless.format(
              "yml",
              conf -> {
                conf.target("**/*.yml", "**/*.yaml");

                conf.licenseHeader(
                    copyrightSharp, "[a-zA-Z0-9]|# End License|# Includes work from:");
              });
        });
  }

  private static void addStandardJavaTestDependencies(Project project) {
    DependencyHandler dependencies = project.getDependencies();

    dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, "com.google.code.findbugs:jsr305");
    dependencies.add(
        JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
        "org.curioswitch.curiostack:curio-testing-framework");
    dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.assertj:assertj-core");
    dependencies.add(
        JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.awaitility:awaitility");
    dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "junit:junit");
    dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.mockito:mockito-core");
    dependencies.add(
        JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.mockito:mockito-junit-jupiter");
    dependencies.add(
        JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "info.solidsoft.mockito:mockito-java8");

    dependencies.add(
        JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, "org.junit.jupiter:junit-jupiter-api");
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
        .add(configuration.getName(), "com.google.cloud.sql:mysql-socket-factory");
    project
        .getTasks()
        .register(
            "setupDataSources",
            Copy.class,
            t -> {
              t.from(configuration);
              t.into(".ideaDataSources/drivers");
            });
  }

  private static Optional<Node> findChild(Node node, String name) {
    // Should work.
    @SuppressWarnings("unchecked")
    List<Node> children = (List<Node>) node.children();
    return children.stream()
        .filter(
            n ->
                n.name().equals(name)
                    || (n.name() instanceof QName
                        && ((QName) n.name()).getLocalPart().equals(name)))
        .findFirst();
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

  private static Node findOrCreateChild(Node parent, String type) {
    return findChild(parent, node -> node.name().equals(type))
        .orElseGet(() -> parent.appendNode(type));
  }

  private static void setOption(Node component, String name, String value) {
    findOrCreateChild(component, "option", name).attributes().put("value", value);
  }

  private static void setProperty(Node component, String name, String value) {
    findOrCreateChild(component, "property", name).attributes().put("value", value);
  }

  private static void setupProjectXml(Project project, XmlProvider xml) {
    var gradleSettings = findOrCreateChild(xml.asNode(), "component", "GradleSettings");
    if (gradleSettings.children().isEmpty()) {
      var linkedExternalProjectsSettings =
          findOrCreateChild(gradleSettings, "option", "linkedExternalProjectsSettings");
      var gradleProjectSettings =
          linkedExternalProjectsSettings.appendNode("GradleProjectSettings");
      setOption(gradleProjectSettings, "distributionType", "DEFAULT_WRAPPED");
      setOption(gradleProjectSettings, "externalProjectPath", "$PROJECT_DIR$");
      setOption(gradleProjectSettings, "useAutoImport", "true");
      setOption(gradleProjectSettings, "useQualifiedModuleNames", "true");
    }

    var googleJavaFormat = findOrCreateChild(xml.asNode(), "component", "GoogleJavaFormatSettings");
    if (googleJavaFormat.children().isEmpty()) {
      setOption(googleJavaFormat, "enabled", "true");
    }

    var copyrightManager = findOrCreateChild(xml.asNode(), "component", "CopyrightManager");
    var copyrightDir = project.file(".baseline").toPath().resolve("copyright");
    try (var files = Files.list(copyrightDir)) {
      files.forEach(
          file -> {
            String filename = copyrightDir.relativize(file).toString();
            if (findChild(
                    copyrightManager,
                    copyright ->
                        copyright.name().equals("copyright")
                            && findChild(
                                    copyright,
                                    option ->
                                        option.name().equals("myName")
                                            && option.attribute("value").equals(filename))
                                .isPresent())
                .isPresent()) {
              return;
            }
            final String copyrightText;
            try {
              copyrightText = Files.readString(file);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
            var copyrightNode = copyrightManager.appendNode("copyright");
            setOption(copyrightNode, "notice", copyrightText);
            setOption(copyrightNode, "keyword", "Copyright");
            setOption(copyrightNode, "allowReplaceKeyword", "");
            setOption(copyrightNode, "myName", filename);
            setOption(copyrightNode, "myLocal", "true");

            copyrightManager.attributes().put("default", filename);
          });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    var vcsDirectoryMappings = findOrCreateChild(xml.asNode(), "component", "VcsDirectoryMappings");
    if (findChild(
            vcsDirectoryMappings,
            node -> node.name().equals("mapping") && node.attribute("vcs").equals("Git"))
        .isEmpty()) {
      vcsDirectoryMappings.appendNode(
          "mapping", ImmutableMap.of("directory", "$PROJECT_DIR$", "vcs", "Git"));
    }

    Node typescriptCompiler = findOrCreateChild(xml.asNode(), "component", "TypeScriptCompiler");
    setOption(
        typescriptCompiler, "nodeInterpreterTextField", NodeUtil.getNodeExe(project).toString());

    Node eslintConfiguration = findOrCreateChild(xml.asNode(), "component", "EslintConfiguration");
    findOrCreateChild(eslintConfiguration, "extra-options")
        .attributes()
        .put("value", "--ext .js,.ts,.jsx,.tsx");

    Node inspectionManager =
        findOrCreateChild(xml.asNode(), "component", "InspectionProjectProfileManager");
    Node profile =
        findChild(inspectionManager, n -> n.name().equals("profile"))
            .orElseGet(
                () -> inspectionManager.appendNode("profile", ImmutableMap.of("version", "1.0")));
    setOption(profile, "myName", "Project Default");
    Node updated =
        findChild(
                profile,
                n -> n.name().equals("inspection_tool") && "Eslint".equals(n.attribute("class")))
            .orElseGet(
                () ->
                    profile.appendNode(
                        "inspection_tool",
                        ImmutableMap.of(
                            "class",
                            "Eslint",
                            "enabled",
                            "true",
                            "level",
                            "WARNING",
                            "enabled_by_default",
                            "true")));

    var projectCodeStyleConfiguration =
        findOrCreateChild(xml.asNode(), "component", "ProjectCodeStyleConfiguration");
    setOption(projectCodeStyleConfiguration, "USE_PER_PROJECT_SETTINGS", "true");
    projectCodeStyleConfiguration.remove(
        findOrCreateChild(projectCodeStyleConfiguration, "code_scheme", "Project"));

    final String googleStyle;
    try {
      googleStyle =
          Resources.toString(
              Resources.getResource(
                  CuriostackRootPlugin.class, "/curiostack/intellij-java-google-style.xml"),
              StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    final Node googleStyleNode;
    try {
      googleStyleNode = new XmlParser().parseText(googleStyle);
    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new IllegalStateException(e);
    }

    googleStyleNode.attributes().put("name", "Project");
    projectCodeStyleConfiguration.append(googleStyleNode);
  }

  private static void setupWorkspaceXml(Project project, XmlProvider xml) {
    Node properties = findOrCreateChild(xml.asNode(), "component", "PropertiesComponent");
    setProperty(properties, "js.linters.configure.manually.selectedeslint", "true");
    setProperty(
        properties, "node.js.path.for.package.eslint", NodeUtil.getNodeExe(project).toString());
    setProperty(properties, "node.js.detected.package.eslint", "true");
    setProperty(properties, "node.js.selected.package.eslint", "$PROJECT_DIR$/node_modules/eslint");
    setProperty(
        properties, "settings.editor.selected.configurable", "settings.javascript.linters.eslint");
  }

  private static boolean isCuriostack(Project project) {
    return "true"
        .equals(project.getRootProject().findProperty("org.curioswitch.curiostack.is_curiostack"));
  }
}
