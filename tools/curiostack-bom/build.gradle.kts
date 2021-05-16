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

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    `java-platform`
    `maven-publish`
    id("com.github.ben-manes.versions")
}

javaPlatform {
    allowDependencies()
}

repositories {
    gradlePluginPortal()
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val GRPC_VERSION = "1.37.1"

val DEPENDENCY_BOMS = listOf(
        "com.fasterxml.jackson:jackson-bom:2.12.3",
        "com.google.cloud:google-cloud-bom:0.153.0",
        "com.google.api-client:google-api-client-bom:1.31.5",
        "com.google.guava:guava-bom:30.1.1-jre",
        "com.google.http-client:google-http-client-bom:1.39.2",
        "com.google.protobuf:protobuf-bom:3.17.0",
        "com.linecorp.armeria:armeria-bom:1.7.2",
        "io.dropwizard.metrics:metrics-bom:4.2.0",
        "io.grpc:grpc-bom:${GRPC_VERSION}",
        "io.micrometer:micrometer-bom:1.7.0",
        "io.zipkin.brave:brave-bom:5.13.3",
        "io.netty:netty-bom:4.1.63.Final",
        "org.apache.beam:beam-sdks-java-bom:2.29.0",
        "org.apache.logging.log4j:log4j-bom:2.14.1",
        "org.junit:junit-bom:5.7.2",
        "software.amazon.awssdk:bom:2.16.63"
)

val DEPENDENCY_SETS = listOf(
        DependencySet(
                "commons-codec",
                "1.15",
                listOf("commons-codec")
        ),
        DependencySet(
                "commons-logging",
                "1.2",
                listOf("commons-logging")
        ),
        DependencySet(
                "com.auth0",
                "3.16.0",
                listOf("java-jwt")
        ),
        DependencySet(
                "com.google.auth",
                "0.25.5",
                listOf("google-auth-library-oauth2-http")
        ),
        DependencySet(
                "com.google.auto.factory",
                "1.0.1",
                listOf("auto-factory")
        ),
        DependencySet(
                "com.google.auto.service",
                "1.0",
                listOf("auto-service")
        ),
        DependencySet(
                "com.google.auto.value",
                "1.8.1",
                listOf("auto-value", "auto-value-annotations")
        ),
        DependencySet(
                "com.github.ben-manes.caffeine",
                "3.0.2",
                listOf("caffeine")
        ),
        DependencySet(
                "com.google.auto",
                "1.0",
                listOf("auto-common")
        ),
        DependencySet(
                "com.google.code.findbugs",
                "3.0.2",
                listOf("jsr305")
        ),
        DependencySet(
                "com.google.cloud.sql",
                "1.2.3",
                listOf(
                        "mysql-socket-factory",
                        "mysql-socket-factory-connector-j-8"
                )
        ),
        DependencySet(
                "com.google.dagger",
                "2.35.1",
                listOf("dagger", "dagger-compiler", "dagger-producers")
        ),
        DependencySet(
                "com.google.errorprone",
                "2.7.0",
                listOf("error_prone_annotations", "error_prone_core")
        ),
        DependencySet(
                "com.google.firebase",
                "7.3.0",
                listOf("firebase-admin")
        ),
        DependencySet(
                "com.google.protobuf",
                "3.17.0",
                listOf("protoc")
        ),
        DependencySet(
                "com.spotify",
                "4.3.0",
                listOf("futures-extra")
        ),
        DependencySet(
                "com.squareup.retrofit2",
                "2.9.0",
                listOf(
                        "adapter-guava",
                        "adapter-java8",
                        "converter-guava",
                        "converter-jackson",
                        "converter-java8",
                        "retrofit"
                )
        ),
        DependencySet(
                "com.typesafe",
                "1.4.1",
                listOf("config")
        ),
        DependencySet(
                "com.zaxxer",
                "4.0.3",
                listOf("HikariCP")
        ),
        DependencySet(
                "info.solidsoft.mockito",
                "2.5.0",
                listOf("mockito-java8")
        ),
        DependencySet(
                "io.fabric8",
                "5.2.1",
                listOf("kubernetes-client")
        ),
        // grpc-bom can only be applied to Java projects because it does not export Gradle metadata. For
        // non-Java projects compiling gRPC stubs, they will only use these artifacts so we go ahead and manage
        // then in curiostack-bom as well.
        DependencySet(
                "io.grpc",
                GRPC_VERSION,
                listOf("grpc-core", "grpc-protobuf", "grpc-stub")
        ),
        DependencySet(
                "io.lettuce",
                "6.1.2.RELEASE",
                listOf("lettuce-core")
        ),
        DependencySet(
                "io.netty",
                "2.0.39.Final",
                listOf("netty-tcnative-boringssl-static")
        ),
        DependencySet(
                "io.prometheus",
                "0.10.0",
                listOf("simpleclient", "simpleclient_common", "simpleclient_hotspot", "simpleclient_log4j2")
        ),
        DependencySet(
                "io.zipkin.gcp",
                "1.0.2",
                listOf("brave-propagation-stackdriver", "zipkin-translation-stackdriver")
        ),
        DependencySet(
                "jakarta.annotation",
                "1.3.5",
                listOf("jakarta.annotation-api")
        ),
        DependencySet(
                "jakarta.inject",
                "2.0.0",
                listOf("jakarta.inject-api")
        ),
        DependencySet(
                "junit",
                "4.13.2",
                listOf("junit")
        ),
        DependencySet(
                "net.adoptopenjdk",
                "0.4.0",
                listOf("net.adoptopenjdk.v3.api", "net.adoptopenjdk.v3.vanilla")
        ),
        DependencySet(
                "net.bytebuddy",
                "1.11.0",
                listOf("byte-buddy", "byte-buddy-agent")
        ),
        DependencySet(
                "org.apache.httpcomponents",
                "4.5.13",
                listOf("httpclient")
        ),
        DependencySet(
                "org.apache.httpcomponents",
                "4.4.14",
                listOf("httpcore")
        ),
        DependencySet(
                "org.assertj",
                "3.19.0",
                listOf("assertj-core")
        ),
        DependencySet(
                "org.assertj",
                "3.4.0",
                listOf("assertj-guava")
        ),
        DependencySet(
                "org.awaitility",
                "4.1.0",
                listOf("awaitility")
        ),
        DependencySet(
                "org.bouncycastle",
                "1.67",
                listOf("bcpkix-jdk15on", "bcprov-jdk15on")
        ),
        DependencySet(
                "org.checkerframework",
                "3.13.0",
                listOf("checker-qual")
        ),
        DependencySet(
                "org.checkerframework",
                "2.5.5",
                listOf("checker-compat-qual")
        ),
        DependencySet(
                "org.codehaus.groovy",
                "3.0.8",
                listOf("groovy")
        ),
        DependencySet(
                "org.eclipse.jgit",
                "5.11.1.202105131744-r",
                listOf("org.eclipse.jgit", "org.eclipse.jgit.ssh.apache", "org.eclipse.jgit.ssh.jsch")
        ),
        DependencySet(
                "org.immutables",
                "2.8.8",
                listOf("builder", "value", "value-annotations")
        ),
        DependencySet(
                "org.javassist",
                "3.28.0-GA",
                listOf("javassist")
        ),
        DependencySet(
                "org.jctools",
                "3.3.0",
                listOf("jctools-core")
        ),
        DependencySet(
                "org.jooq",
                "3.14.9",
                listOf("jooq", "jooq-codegen", "jooq-meta")
        ),
        DependencySet(
                "org.mockito",
                "3.10.0",
                listOf("mockito-core", "mockito-junit-jupiter")
        ),
        DependencySet(
                "org.slf4j",
                "1.7.30",
                listOf("jul-to-slf4j", "slf4j-api")
        ),
        DependencySet(
                "org.simpleflatmapper",
                "8.2.3",
                listOf(
                        "sfm-converter", "sfm-jdbc", "sfm-jooq", "sfm-map", "sfm-reflect", "sfm-util"
                )
        ),
        DependencySet(
                "org.yaml",
                "1.28",
                listOf("snakeyaml")
        )
)

val DEPENDENCIES = listOf(
        "com.bmuschko:gradle-docker-plugin:7.0.0",
        "com.diffplug.spotless:spotless-plugin-gradle:5.12.5",
        "com.github.ben-manes:gradle-versions-plugin:0.38.0",
        "com.google.code.gson:gson:2.8.6",
        "com.google.gradle:osdetector-gradle-plugin:1.7.0",
        "com.google.maps:google-maps-services:0.18.0",
        "com.gorylenko.gradle-git-properties:gradle-git-properties:2.3.1",
        "com.gradle:gradle-enterprise-gradle-plugin:3.6.1",
        "com.hubspot.jinjava:jinjava:2.5.7",
        "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5",
        "com.uber.nullaway:nullaway:0.9.1",
        "de.undercouch:gradle-download-task:4.1.1",
        "gradle.plugin.com.boxfuse.client:gradle-plugin-publishing:6.0.6",
        "gradle.plugin.com.google.cloud.tools:jib-gradle-plugin:2.8.0",
        "gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.14.0",
        "io.sgr:s2-geometry-library-java:1.0.1",
        "it.unimi.dsi:fastutil:8.5.3",
        "javax.activation:activation:1.1.1",
        "javax.annotation:javax.annotation-api:1.3.2",
        "me.champeau.gradle:jmh-gradle-plugin:0.5.3",
        "mysql:mysql-connector-java:8.0.24",
        "net.ltgt.gradle:gradle-errorprone-plugin:2.0.1",
        "net.ltgt.gradle:gradle-nullaway-plugin:1.1.0",
        "nu.studer:gradle-jooq-plugin:4.2",
        "org.jsoup:jsoup:1.13.1"
)

val bomProject = project
rootProject.allprojects {
    if (path.startsWith(":common:")) {
        bomProject.evaluationDependsOn(path)
    }
}

dependencies {
    for (bom in DEPENDENCY_BOMS) {
        api(enforcedPlatform(bom))
    }
    constraints {
        rootProject.allprojects {
            if (path.startsWith(":common:")) {
                plugins.withId("maven-publish") {
                    api("${group}:${base.archivesBaseName}:${version}")
                }
            }
        }

        for (set in DEPENDENCY_SETS) {
            for (module in set.modules) {
                api("${set.group}:${module}:${set.version}")
            }
        }
        for (dependency in DEPENDENCIES) {
            api(dependency)
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components.getByName("javaPlatform"))

            pom {
                name.set("Curiostack Bill-of-Materials")
                description.set("BOM specifying versions for all standard Curiostack dependencies.")
                url.set(
                        "https://github.com/curioswitch/curiostack/tree/master/tools/" +
                                "curiostack-bom"
                )
                setPackaging("pom")
            }
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version) || version.endsWith("-jre")
    return isStable.not()
}

tasks {
    named<DependencyUpdatesTask>("dependencyUpdates") {
        revision = "release"
        checkConstraints = true

        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }

    withType<GenerateModuleMetadata> {
        suppressedValidationErrors.add("enforced-platform")
    }
}
