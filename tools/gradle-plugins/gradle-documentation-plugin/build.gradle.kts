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
    `java-gradle-plugin`
    groovy
}

repositories {
    jcenter()
}

sourceSets {
    main {
        withConvention(GroovySourceSet::class) {
            java.setSrcDirs(emptyList<String>())
            groovy.setSrcDirs(listOf("src/main/java", "src/main/groovy"))
        }
    }
}

dependencies {
    implementation(project(":tools:gradle-plugins:gradle-helpers"))

    implementation("com.google.gradle:osdetector-gradle-plugin")
    implementation("com.google.guava:guava")

    compileOnly(project(":common:curio-helpers"))

    annotationProcessor("org.immutables:value")
    compileOnly("org.immutables:value-annotations")

    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
}

gradlePlugin {
    plugins {
        register("documentation") {
            id = "org.curioswitch.gradle-documentation-plugin"
            displayName = "Gradle Documentation Plugin"
            description = "A plugin for building and publishing documentation."
            implementationClass = "org.curioswitch.gradle.documentation.DocumentationPlugin"
        }
    }
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations.getByName("functionalTestImplementation")
        .extendsFrom(configurations.getByName("testImplementation"))

val functionalTest by tasks.registering(Test::class) {
    group = "verification"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

val check by tasks.getting(Task::class) {
    dependsOn(functionalTest)
}
