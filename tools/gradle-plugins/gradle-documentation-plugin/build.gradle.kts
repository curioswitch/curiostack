/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
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
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation("com.google.guava:guava")

    testImplementation(gradleTestKit())
    testImplementation("org.javatuples:javatuples:1.2")
    testImplementation(project(":tools:gradle-plugins:gradle-test-helpers"))
}

val descriptionContent = "Gradle plugin for building and publishing documentation."
gradlePlugin {
    plugins {
        register("documentation") {
            id = "org.curioswitch.gradle-documentation-plugin"
            displayName = "Gradle Documentation Plugin"
            description = descriptionContent
            implementationClass = "org.curioswitch.gradle.documentation.DocumentationPlugin"
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("pluginMaven") {
            pom {
                name.set("Gradle Documentation Plugin")
                description.set(descriptionContent)
                url.set("https://github.com/curioswitch/curiostack/tree/master/tools/" +
                        "gradle-plugins/gradle-documentation-plugin")
            }
        }
    }
}