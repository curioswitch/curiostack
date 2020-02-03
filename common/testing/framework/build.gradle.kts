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

plugins {
    `java-library`
    `maven-publish`
    id("org.curioswitch.gradle-grpc-api-plugin")
}

base {
    archivesBaseName = "curio-testing-framework"
}

configurations {
    testImplementation {
        // Curiostack plugin adds this but this is the one project we don't want it since we are the
        // framework.
        exclude("org.curioswitch.curiostack", "curio-testing-framework")
    }
}

dependencies {
    api(project(":common:server:framework"))
    api(project(":common:testing:assertj-protobuf"))
    implementation(project(":common:grpc:protobuf-jackson"))

    api("org.junit.jupiter:junit-jupiter-api")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.assertj:assertj-core")
    implementation("org.mockito:mockito-core")
    implementation("org.mockito:mockito-junit-jupiter")

    annotationProcessor("com.google.dagger:dagger-compiler")

    annotationProcessor("org.immutables:value")
    compileOnly("org.immutables:value-annotations")
    testAnnotationProcessor("org.immutables:value")
    testCompileOnly("org.immutables:value-annotations")
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            pom {
                name.set("Curio Testing Framework")
                description.set("A testing framework to help when using curiostack.")
                url.set("https://github.com/curioswitch/curiostack/tree/master/" +
                        "common/testing/framework")
            }
        }
    }
}
