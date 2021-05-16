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
    id("org.curioswitch.gradle-curio-server-plugin")
}

base {
    archivesBaseName = "eggworld-server"
}

application {
    mainClass.set("org.curioswitch.eggworld.server.EggworldMain")
}

dependencies {
    implementation(project(":common:server:framework"))
    implementation(project(":eggworld:api"))
    implementation(project(":eggworld:client:web"))

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava")
    implementation("com.linecorp.armeria:armeria-retrofit2")
    implementation("com.squareup.retrofit2:adapter-guava")
    implementation("com.squareup.retrofit2:converter-jackson")

    annotationProcessor("com.google.dagger:dagger-compiler")

    annotationProcessor("org.immutables:value")
    compileOnly("org.immutables:value-annotations")
}

server {
    deployments {
        register("alpha") {
            namespace.set("eggworld-dev")
            autoDeploy.set(true)
        }
    }
}
