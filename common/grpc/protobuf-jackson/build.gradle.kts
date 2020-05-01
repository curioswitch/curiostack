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

plugins {
    `java-library`
    `maven-publish`

    id("me.champeau.gradle.jmh")
    id("org.curioswitch.gradle-protobuf-plugin")
}

base {
    archivesBaseName = "protobuf-jackson"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

jmh {
    exclude = listOf(".*FieldDispatchBenchmark.*")
    warmupIterations = 30
    iterations = 10
    fork = 5
    profilers = listOf()
    isIncludeTests = true
    isZip64 = true
}

protobuf {
    protoc {
        artifact.set("com.google.protobuf:protoc:3.8.0")
    }

    descriptorSetOptions.enabled.set(false)
    descriptorSetOptions.path.set(file("build/unused-descriptor-set"))
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.google.protobuf:protobuf-java")

    implementation("com.google.protobuf:protobuf-java-util")
    implementation("net.bytebuddy:byte-buddy")

    testImplementation("org.curioswitch.curiostack:curio-testing-framework:0.0.6")
}

publishing {
    publications {
        register("maven", MavenPublication::class) {
            pom {
                name.set("protobuf-jackson")
                description.set("A library for efficient marshalling of Protocol Buffer messages " +
                        "to and from JSON.")
                url.set("https://github.com/curioswitch/curiostack/tree/master/" +
                        "common/grpc/protobuf-jackson")
            }
        }
    }
}

tasks {
    named<Javadoc>("javadoc") {
        exclude("**/org/curioswitch/common/protobuf/json/ParseSupport.*")
        exclude("**/org/curioswitch/common/protobuf/json/SerializeSupport.*")
        exclude("**/org/curioswitch/common/protobuf/json/TypeSpecificMarshaller.*")
        exclude("**/org/curioswitch/common/protobuf/json/bytebuddy/**")
    }
}
