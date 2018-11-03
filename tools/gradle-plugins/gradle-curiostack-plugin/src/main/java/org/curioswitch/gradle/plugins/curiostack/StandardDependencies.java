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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;

public class StandardDependencies {

  @Immutable
  @Style(
      visibility = ImplementationVisibility.PACKAGE,
      builderVisibility = BuilderVisibility.PACKAGE,
      defaultAsDefault = true)
  interface DependencySet {
    String group();

    List<String> modules();

    String version();
  }

  public static final String GCLOUD_VERSION = "223.0.0";
  public static final String HELM_VERSION = "2.10.0";
  public static final String MINICONDA_VERSION = "4.5.11";
  public static final String TERRAFORM_VERSION = "0.11.10";
  public static final String NODE_VERSION = "10.13.0";
  public static final String YARN_VERSION = "1.12.1";

  static final String GOOGLE_JAVA_FORMAT_VERSION = "1.6";
  static final String GRADLE_VERSION = "5.0-rc-1";

  private static final String JACKSON_VERSION = "2.9.7";

  static final ImmutableList<DependencySet> DEPENDENCY_SETS =
      ImmutableList.of(
          ImmutableDependencySet.builder()
              .group("commons-codec")
              .version("1.10")
              .addModules("commons-codec")
              .build(),
          ImmutableDependencySet.builder()
              .group("commons-logging")
              .version("1.2")
              .addModules("commons-logging")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.auth0")
              .version("3.4.1")
              .addModules("java-jwt")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.auth")
              .version("0.11.0")
              .addModules("google-auth-library-oauth2-http")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.auto.factory")
              .version("1.0-beta6")
              .addModules("auto-factory")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.auto.service")
              .version("1.0-rc4")
              .addModules("auto-service")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.auto.value")
              .version("1.6.3rc1")
              .addModules("auto-value", "auto-value-annotations")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.fasterxml.jackson.core")
              .version(JACKSON_VERSION)
              .addModules("jackson-annotations", "jackson-core", "jackson-databind")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.fasterxml.jackson.dataformat")
              .version(JACKSON_VERSION)
              .addModules("jackson-dataformat-yaml")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.fasterxml.jackson.datatype")
              .version(JACKSON_VERSION)
              .addModules("jackson-datatype-guava", "jackson-datatype-jsr310")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.github.ben-manes.caffeine")
              .version("2.6.2")
              .addModules("caffeine")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.api-client")
              .version("1.26.0")
              .addModules("google-api-client")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.api.grpc")
              .version("0.34.0")
              .addModules("grpc-google-cloud-trace-v1")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.api.grpc")
              .version("1.33.0")
              .addModules("grpc-google-cloud-pubsub-v1")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.auto")
              .version("0.10")
              .addModules("auto-common")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.auto.factory")
              .version("1.0-beta5")
              .addModules("auto-factory")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.code.findbugs")
              .version("3.0.2")
              .addModules("jsr305")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud")
              .version("1.51.0")
              .addModules(
                  "google-cloud-bigquery",
                  "google-cloud-core",
                  "google-cloud-core-grpc",
                  "google-cloud-pubsub",
                  "google-cloud-storage")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud")
              .version("0.53.0-alpha")
              .addModules("google-cloud-resourcemanager")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud")
              .version("0.53.0-beta")
              .addModules("google-cloud-iot", "google-cloud-trace")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud.sql")
              .version("1.0.11")
              .addModules(
                  "mysql-socket-factory",
                  "mysql-socket-factory-connector-j-6",
                  "mysql-socket-factory-connector-j-8")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud.trace.adapters.zipkin")
              .version("0.3.0")
              .addModules("translation")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.dagger")
              .version("2.19")
              .addModules("dagger", "dagger-compiler", "dagger-producers")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.errorprone")
              .version("2.3.2")
              .addModules("error_prone_core")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.firebase")
              .version("6.5.0")
              .addModules("firebase-admin")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.guava")
              .version("27.0-jre")
              .addModules("guava", "guava-testlib")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.protobuf")
              .version("3.6.1")
              .addModules("protobuf-java", "protobuf-java-util")
              .build(),
          // protoc often diverges from protobuf-java
          ImmutableDependencySet.builder()
              .group("com.google.protobuf")
              .version("3.6.1")
              .addModules("protoc")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.linecorp.armeria")
              .version("0.74.1")
              .addModules("armeria", "armeria-grpc", "armeria-retrofit2", "armeria-zipkin")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.spotify")
              .version("4.0.0")
              .addModules("futures-extra")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.squareup.retrofit2")
              .version("2.4.0")
              .addModules(
                  "adapter-guava",
                  "adapter-java8",
                  "converter-guava",
                  "converter-jackson",
                  "converter-java8",
                  "retrofit")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.sun.xml.bind")
              .version("2.3.0.1")
              .addModules("jaxb-core", "jaxb-impl")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.typesafe")
              .version("1.3.3")
              .addModules("config")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.zaxxer")
              .version("3.2.0")
              .addModules("HikariCP")
              .build(),
          ImmutableDependencySet.builder()
              .group("info.solidsoft.mockito")
              .version("2.5.0")
              .addModules("mockito-java8")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.dropwizard.metrics")
              .version("4.0.3")
              .addModules("metrics-core", "metrics-jvm", "metrics-json", "metrics-log4j2")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.fabric8")
              .version("4.1.0")
              .addModules("kubernetes-client")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.lettuce")
              .version("5.1.2.RELEASE")
              .addModules("lettuce-core")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.grpc")
              .version("1.16.1")
              .addModules(
                  "grpc-all",
                  "grpc-auth",
                  "grpc-core",
                  "grpc-netty",
                  "grpc-netty-shaded",
                  "grpc-okhttp",
                  "grpc-protobuf",
                  "grpc-services",
                  "grpc-stub")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.micrometer")
              .version("1.1.0")
              .addModules("micrometer-core", "micrometer-registry-prometheus")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.netty")
              .version("4.1.30.Final")
              .addModules(
                  "netty-buffer",
                  "netty-codec",
                  "netty-codec-dns",
                  "netty-codec-haproxy",
                  "netty-codec-http2",
                  "netty-codec-http",
                  "netty-codec-socks",
                  "netty-common",
                  "netty-handler",
                  "netty-handler-proxy",
                  "netty-resolver",
                  "netty-resolver-dns",
                  "netty-transport",
                  "netty-transport-native-epoll",
                  "netty-transport-native-unix-common")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.netty")
              .version("2.0.17.Final")
              .addModules("netty-tcnative-boringssl-static")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.prometheus")
              .version("0.5.0")
              .addModules("simpleclient", "simpleclient_hotspot", "simpleclient_log4j2")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.zipkin.brave")
              .version("5.5.0")
              .addModules("brave", "brave-instrumentation-mysql", "brave-instrumentation-mysql8")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.zipkin.gcp")
              .version("0.8.3")
              .addModules("brave-propagation-stackdriver", "zipkin-translation-stackdriver")
              .build(),
          ImmutableDependencySet.builder()
              .group("javax.inject")
              .version("1")
              .addModules("javax.inject")
              .build(),
          ImmutableDependencySet.builder()
              .group("junit")
              .version("4.12")
              .addModules("junit")
              .build(),
          ImmutableDependencySet.builder()
              .group("net.bytebuddy")
              .version("1.9.3")
              .addModules("byte-buddy", "byte-buddy-agent")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.apache.beam")
              .version("2.4.0")
              .addModules(
                  "beam-runners-direct-java",
                  "beam-runners-google-cloud-dataflow-java",
                  "beam-sdks-java-io-google-cloud-platform")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.apache.httpcomponents")
              .version("4.5.3")
              .addModules("httpclient")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.apache.httpcomponents")
              .version("4.4.6")
              .addModules("httpcore")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.apache.logging.log4j")
              .version("2.11.1")
              .addModules("log4j-api", "log4j-core", "log4j-jcl", "log4j-jul", "log4j-slf4j-impl")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.assertj")
              .version("3.11.1")
              .addModules("assertj-core")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.assertj")
              .version("3.2.0")
              .addModules("assertj-guava")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.awaitility")
              .version("3.1.2")
              .addModules("awaitility")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.bouncycastle")
              .version("1.60")
              .addModules("bcpkix-jdk15on", "bcprov-jdk15on")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.checkerframework")
              .version("2.5.5")
              .addModules("checker-qual", "checker-compat-qual")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.codehaus.groovy")
              .version("2.4.12")
              .addModules("groovy")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.curioswitch.curiostack")
              .version("0.0.29")
              .addModules(
                  "armeria-google-cloud-core",
                  "armeria-google-cloud-iam",
                  "armeria-google-cloud-pubsub",
                  "armeria-google-cloud-storage",
                  "armeria-google-cloud-trace")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.curioswitch.curiostack")
              .version("0.0.95")
              .addModules("curio-server-framework")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.curioswitch.curiostack")
              .version("0.0.13")
              .addModules("curio-testing-framework")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.eclipse.jgit")
              .version("5.0.1.201806211838-r")
              .addModules("org.eclipse.jgit")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.glassfish.jaxb")
              .version("2.3.0.1")
              .addModules("jaxb-runtime")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.immutables")
              .version("2.7.1")
              .addModules("builder", "value", "value-annotations")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.javassist")
              .version("3.22.0-GA")
              .addModules("javassist")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.jctools")
              .version("2.1.2")
              .addModules("jctools-core")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.jooq")
              .version("3.11.6")
              .addModules("jooq", "jooq-codegen", "jooq-meta")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.junit.jupiter")
              .version("5.3.1")
              .addModules("junit-jupiter-api", "junit-jupiter-engine", "junit-jupiter-params")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.junit.vintage")
              .version("5.3.1")
              .addModules("junit-vintage-engine")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.mockito")
              .version("2.23.2")
              .addModules("mockito-core", "mockito-junit-jupiter")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.slf4j")
              .version("1.7.25")
              .addModules("slf4j-api")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.simpleflatmapper")
              .version("6.0.4")
              .addModules(
                  "sfm-converter", "sfm-jdbc", "sfm-jooq", "sfm-map", "sfm-reflect", "sfm-util")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.snakeyaml")
              .version("1.19")
              .addModules("snakeyaml")
              .build());

  static final ImmutableList<String> DEPENDENCIES =
      ImmutableList.of(
          "com.bmuschko:gradle-docker-plugin:4.0.3",
          "com.diffplug.spotless:spotless-plugin-gradle:3.16.0",
          "com.github.ben-manes:gradle-versions-plugin:0.20.0",
          "com.google.gradle:osdetector-gradle-plugin:1.6.0",
          "com.google.protobuf:protobuf-gradle-plugin:0.8.6",
          "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4",
          "com.netflix.nebula:gradle-dependency-lock-plugin:6.0.0",
          "com.netflix.nebula:gradle-resolution-rules-plugin:6.0.5",
          "com.palantir:gradle-baseline-java:0.10.0",
          "de.undercouch:gradle-download-task:3.4.3",
          "gradle.plugin.com.boxfuse.client:gradle-plugin-publishing:5.2.1",
          "gradle.plugin.com.google.cloud.tools:jib-gradle-plugin:0.9.13",
          "gradle.plugin.com.gorylenko.gradle-git-properties:gradle-git-properties:2.0.0-beta1",
          "gradle.plugin.com.jetbrains.python:gradle-python-envs:0.0.25",
          "gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.14.0",
          "io.spring.gradle:dependency-management-plugin:1.0.6.RELEASE",
          "javax.activation:activation:1.1.1",
          "javax.annotation:javax.annotation-api:1.3.2",
          "javax.xml.bind:jaxb-api:2.3.0",
          "me.champeau.gradle:jmh-gradle-plugin:0.4.7",
          "mysql:mysql-connector-java:8.0.13",
          "net.ltgt.gradle:gradle-apt-plugin:0.19",
          "net.ltgt.gradle:gradle-errorprone-plugin:0.6",
          "nu.studer:gradle-jooq-plugin:3.0.2");

  private StandardDependencies() {}
}
