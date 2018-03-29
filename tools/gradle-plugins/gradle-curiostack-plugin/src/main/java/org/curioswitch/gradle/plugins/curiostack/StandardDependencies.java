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

class StandardDependencies {

  @Immutable
  @Style(
    visibility = ImplementationVisibility.PACKAGE,
    builderVisibility = BuilderVisibility.PACKAGE,
    defaultAsDefault = true
  )
  interface DependencySet {
    String group();

    List<String> modules();

    String version();
  }

  private static final String JACKSON_VERSION = "2.9.5";

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
              .group("com.google.auto.factory")
              .version("1.0-beta5")
              .addModules("auto-factory")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.auto.service")
              .version("1.0-rc4")
              .addModules("auto-service")
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
              .group("com.google.auto")
              .version("0.10")
              .addModules("auto-common")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.code.findbugs")
              .version("3.0.2")
              .addModules("jsr305")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud")
              .version("1.24.0")
              .addModules("google-cloud-storage")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud")
              .version("0.42.0-alpha")
              .addModules("google-cloud-resourcemanager")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud")
              .version("0.42.0-beta")
              .addModules("google-cloud-bigquery", "google-cloud-trace")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud.sql")
              .version("1.0.5")
              .addModules("mysql-socket-factory")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.cloud.trace.adapters.zipkin")
              .version("0.3.0")
              .addModules("translation")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.dagger")
              .version("2.15")
              .addModules("dagger", "dagger-compiler", "dagger-producers")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.firebase")
              .version("5.9.0")
              .addModules("firebase-admin")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.guava")
              .version("24.1-jre")
              .addModules("guava", "guava-testlib")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.google.protobuf")
              .version("3.5.1")
              .addModules("protobuf-java", "protobuf-java-util")
              .build(),
          // protoc often diverges from protobuf-java
          ImmutableDependencySet.builder()
              .group("com.google.protobuf")
              .version("3.5.1-1")
              .addModules("protoc")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.linecorp.armeria")
              .version("0.59.0")
              .addModules("armeria", "armeria-grpc", "armeria-retrofit2", "armeria-zipkin")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.spotify")
              .version("3.1.1")
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
              .version("2.3.0")
              .addModules("jaxb-core", "jaxb-impl")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.typesafe")
              .version("1.3.3")
              .addModules("config")
              .build(),
          ImmutableDependencySet.builder()
              .group("com.zaxxer")
              .version("2.7.8")
              .addModules("HikariCP")
              .build(),
          ImmutableDependencySet.builder()
              .group("info.solidsoft.mockito")
              .version("2.3.0")
              .addModules("mockito-java8")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.dropwizard.metrics")
              .version("4.0.2")
              .addModules("metrics-core", "metrics-jvm", "metrics-json", "metrics-log4j2")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.fabric8")
              .version("3.1.10")
              .addModules("kubernetes-client")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.lettuce")
              .version("5.0.3.RELEASE")
              .addModules("lettuce-core")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.grpc")
              .version("1.11.0")
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
              .version("1.0.2")
              .addModules("micrometer-core", "micrometer-registry-prometheus")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.netty")
              .version("4.1.22.Final")
              .addModules(
                  "netty-buffer",
                  "netty-codec",
                  "netty-codec-dns",
                  "netty-codec-http2",
                  "netty-codec-http",
                  "netty-codec-socks",
                  "netty-common",
                  "netty-handler",
                  "netty-handler-proxy",
                  "netty-resolver",
                  "netty-resolver-dns",
                  "netty-transport",
                  "netty-transport-native-epoll")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.netty")
              .version("2.0.7.Final")
              .addModules("netty-tcnative-boringssl-static")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.prometheus")
              .version("0.3.0")
              .addModules("simpleclient_hotspot", "simpleclient_log4j2")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.zipkin.brave")
              .version("4.18.2")
              .addModules("brave", "brave-instrumentation-mysql")
              .build(),
          ImmutableDependencySet.builder()
              .group("io.zipkin.gcp")
              .version("0.1.0")
              .addModules("zipkin-storage-stackdriver")
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
              .version("1.8.1")
              .addModules("byte-buddy", "byte-buddy-agent")
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
              .version("2.11.0")
              .addModules("log4j-api", "log4j-core", "log4j-jcl", "log4j-jul", "log4j-slf4j-impl")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.assertj")
              .version("3.9.1")
              .addModules("assertj-core")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.awaitility")
              .version("3.1.0")
              .addModules("awaitility")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.bouncycastle")
              .version("1.59")
              .addModules("bcpkix-jdk15on")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.codehaus.groovy")
              .version("2.4.12")
              .addModules("groovy")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.curioswitch.curiostack")
              .version("0.0.64")
              .addModules("curio-server-framework")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.curioswitch.curiostack")
              .version("0.0.5")
              .addModules("curio-testing-framework")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.eclipse.jgit")
              .version("4.9.0.201710071750-r")
              .addModules("org.eclipse.jgit")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.immutables")
              .version("2.5.6")
              .addModules("builder", "value")
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
              .version("3.10.6")
              .addModules("jooq")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.junit.jupiter")
              .version("5.1.0")
              .addModules("junit-jupiter-api", "junit-jupiter-engine")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.junit.vintage")
              .version("5.1.0")
              .addModules("junit-vintage-engine")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.mockito")
              .version("2.17.0")
              .addModules("mockito-core", "mockito-junit-jupiter")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.slf4j")
              .version("1.7.25")
              .addModules("slf4j-api")
              .build(),
          ImmutableDependencySet.builder()
              .group("org.snakeyaml")
              .version("1.19")
              .addModules("snakeyaml")
              .build());

  static final ImmutableList<String> DEPENDENCIES =
      ImmutableList.of(
          "com.bmuschko:gradle-docker-plugin:3.2.5",
          "com.diffplug.spotless:spotless-plugin-gradle:3.10.0",
          "com.github.ben-manes:gradle-versions-plugin:0.17.0",
          "com.google.protobuf:protobuf-gradle-plugin:0.8.5",
          "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.0",
          "com.moowork.gradle:gradle-node-plugin:1.2.0",
          "com.netflix.nebula:gradle-dependency-lock-plugin:5.0.4",
          "com.netflix.nebula:gradle-resolution-rules-plugin:5.1.1",
          "com.palantir:gradle-baseline-java:0.10.0",
          "gradle.plugin.com.boxfuse.client:gradle-plugin-publishing:5.0.7",
          "gradle.plugin.com.gorylenko.gradle-git-properties:gradle-git-properties:1.4.21",
          "gradle.plugin.com.jetbrains.python:gradle-python-envs:0.0.25",
          "gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.14.0",
          "io.spring.gradle:dependency-management-plugin:1.0.4.RELEASE",
          "javax.activation:activation:1.1.1",
          "javax.annotation:javax.annotation-api:1.3.2",
          "javax.xml.bind:jaxb-api:2.3.0",
          "me.champeau.gradle:jmh-gradle-plugin:0.4.5",
          "mysql:mysql-connector-java:5.1.45",
          "net.ltgt.gradle:gradle-apt-plugin:0.15",
          "net.ltgt.gradle:gradle-errorprone-plugin:0.0.13",
          "nu.studer:gradle-jooq-plugin:2.0.11");

  private StandardDependencies() {}
}
