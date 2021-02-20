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
    jcenter()
    maven {
        setUrl("https://dl.bintray.com/curioswitch/curiostack")
    }
    maven {
        setUrl("https://dl.bintray.com/mockito/maven")
    }
    gradlePluginPortal()
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val GRPC_VERSION = "1.35.0"

val DEPENDENCY_BOMS = listOf(
        "com.fasterxml.jackson:jackson-bom:2.12.1",
        "com.google.cloud:google-cloud-bom:0.147.0",
        "com.google.api-client:google-api-client-bom:1.31.2",
        "com.google.guava:guava-bom:30.1-jre",
        "com.google.http-client:google-http-client-bom:1.38.1",
        "com.google.protobuf:protobuf-bom:3.15.1",
        "com.linecorp.armeria:armeria-bom:1.5.0",
        "io.dropwizard.metrics:metrics-bom:4.1.17",
        "io.grpc:grpc-bom:${GRPC_VERSION}",
        "io.micrometer:micrometer-bom:1.6.4",
        "io.zipkin.brave:brave-bom:5.12.7",
        "io.netty:netty-bom:4.1.58.Final",
        "org.apache.beam:beam-sdks-java-bom:2.27.0",
        "org.apache.logging.log4j:log4j-bom:2.14.0",
        "org.junit:junit-bom:5.7.1",
        "software.amazon.awssdk:bom:2.16.3"
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
                "3.13.0",
                listOf("java-jwt")
        ),
        DependencySet(
                "com.google.auth",
                "0.23.0",
                listOf("google-auth-library-oauth2-http")
        ),
        DependencySet(
                "com.google.auto.factory",
                "1.0-beta8",
                listOf("auto-factory")
        ),
        DependencySet(
                "com.google.auto.service",
                "1.0-rc7",
                listOf("auto-service")
        ),
        DependencySet(
                "com.google.auto.value",
                "1.7.4",
                listOf("auto-value", "auto-value-annotations")
        ),
        DependencySet(
                "com.github.ben-manes.caffeine",
                "2.9.0",
                listOf("caffeine")
        ),
        DependencySet(
                "com.google.auto",
                "0.11",
                listOf("auto-common")
        ),
        DependencySet(
                "com.google.code.findbugs",
                "3.0.2",
                listOf("jsr305")
        ),
        DependencySet(
                "com.google.cloud.sql",
                "1.2.1",
                listOf(
                        "mysql-socket-factory",
                        "mysql-socket-factory-connector-j-6",
                        "mysql-socket-factory-connector-j-8")
        ),
        DependencySet(
                "com.google.dagger",
                "2.32",
                listOf("dagger", "dagger-compiler", "dagger-producers")
        ),
        DependencySet(
                "com.google.errorprone",
                "2.4.0",
                listOf("error_prone_annotations", "error_prone_core")
        ),
        DependencySet(
                "com.google.firebase",
                "7.1.0",
                listOf("firebase-admin")
        ),
        DependencySet(
                "com.google.protobuf",
                "3.15.1",
                listOf("protoc")
        ),
        DependencySet(
                "com.spotify",
                "4.2.2",
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
                        "retrofit")
        ),
        DependencySet(
                "com.typesafe",
                "1.4.1",
                listOf("config")
        ),
        DependencySet(
                "com.zaxxer",
                "4.0.2",
                listOf("HikariCP")
        ),
        DependencySet(
                "info.solidsoft.mockito",
                "2.5.0",
                listOf("mockito-java8")
        ),
        DependencySet(
                "io.fabric8",
                "5.1.0",
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
                "6.0.2.RELEASE",
                listOf("lettuce-core")
        ),
        DependencySet(
                "io.netty",
                "2.0.36.Final",
                listOf("netty-tcnative-boringssl-static")
        ),
        DependencySet(
                "io.prometheus",
                "0.10.0",
                listOf("simpleclient", "simpleclient_common", "simpleclient_hotspot", "simpleclient_log4j2")
        ),
        DependencySet(
                "io.zipkin.gcp",
                "0.17.0",
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
                "1.10.20",
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
                "4.0.3",
                listOf("awaitility")
        ),
        DependencySet(
                "org.bouncycastle",
                "1.67",
                listOf("bcpkix-jdk15on", "bcprov-jdk15on")
        ),
        DependencySet(
                "org.checkerframework",
                "3.10.0",
                listOf("checker-qual")
        ),
        DependencySet(
                "org.checkerframework",
                "2.5.5",
                listOf("checker-compat-qual")
        ),
        DependencySet(
                "org.codehaus.groovy",
                "2.5.8",
                listOf("groovy")
        ),
        DependencySet(
                "org.eclipse.jgit",
                "5.10.0.202012080955-r",
                listOf("org.eclipse.jgit")
        ),
        DependencySet(
                "org.immutables",
                "2.8.8",
                listOf("builder", "value", "value-annotations")
        ),
        DependencySet(
                "org.javassist",
                "3.27.0-GA",
                listOf("javassist")
        ),
        DependencySet(
                "org.jctools",
                "3.2.0",
                listOf("jctools-core")
        ),
        DependencySet(
                "org.jooq",
                "3.14.7",
                listOf("jooq", "jooq-codegen", "jooq-meta")
        ),
        DependencySet(
                "org.mockito",
                "3.7.18",
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
                        "sfm-converter", "sfm-jdbc", "sfm-jooq", "sfm-map", "sfm-reflect", "sfm-util")
        ),
        DependencySet(
                "org.yaml",
                "1.27",
                listOf("snakeyaml")
        )
)

val DEPENDENCIES = listOf(
        "com.bmuschko:gradle-docker-plugin:6.7.0",
        "com.diffplug.spotless:spotless-plugin-gradle:5.10.2",
        "com.github.ben-manes:gradle-versions-plugin:0.36.0",
        "com.google.code.gson:gson:2.8.6",
        "com.google.gradle:osdetector-gradle-plugin:1.7.0",
        "com.google.maps:google-maps-services:0.17.0",
        "com.gradle:gradle-enterprise-gradle-plugin:3.5.2",
        "com.hubspot.jinjava:jinjava:2.5.6",
        "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5",
        "com.uber.nullaway:nullaway:0.8.0",
        "de.undercouch:gradle-download-task:4.1.1",
        "gradle.plugin.com.boxfuse.client:gradle-plugin-publishing:6.0.6",
        "gradle.plugin.com.google.cloud.tools:jib-gradle-plugin:2.1.0",
        "gradle.plugin.com.gorylenko.gradle-git-properties:gradle-git-properties:2.2.4",
        "gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.14.0",
        "io.sgr:s2-geometry-library-java:1.0.1",
        "it.unimi.dsi:fastutil:8.5.2",
        "javax.activation:activation:1.1.1",
        "javax.annotation:javax.annotation-api:1.3.2",
        "me.champeau.gradle:jmh-gradle-plugin:0.5.3",
        "mysql:mysql-connector-java:8.0.23",
        "net.ltgt.gradle:gradle-apt-plugin:0.21",
        "net.ltgt.gradle:gradle-errorprone-plugin:1.3.0",
        "net.ltgt.gradle:gradle-nullaway-plugin:1.0.2",
        "nu.studer:gradle-jooq-plugin:4.2",
        "org.jsoup:jsoup:1.13.1"
)

dependencies {
    for (bom in DEPENDENCY_BOMS) {
        api(enforcedPlatform(bom))
    }
    constraints {
        rootProject.allprojects {
            if (path.startsWith(":common:")) {
                plugins.withId("maven-publish") {
                    afterEvaluate {
                        api("${group}:${base.archivesBaseName}:${version}")
                    }
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
                url.set("https://github.com/curioswitch/curiostack/tree/master/tools/" +
                        "curiostack-bom")
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
}
