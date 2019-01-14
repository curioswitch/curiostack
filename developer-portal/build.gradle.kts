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

import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask

plugins {
    id("org.curioswitch.gradle-node-plugin")
}

tasks {
    val mergeSite by registering(Copy::class) {
        dependsOn(project(":docs:codelabs").tasks.named("assemble"))

        into("build/site")

        from("index.html")

        from(project(":docs:codelabs").file("build/site")) {
            into("codelabs")
        }
    }

    val assemble = named("assemble") {
        dependsOn(mergeSite);
    }

    val deploy by registering(NodeTask::class) {
        dependsOn(rootProject.tasks.named("yarn"), assemble)

        args("run", "firebase", "deploy")
    }

    val preview by registering(NodeTask::class) {
        dependsOn(rootProject.tasks.named("yarn"), assemble)

        args("run", "superstatic", "--port=8080")
    }
}
