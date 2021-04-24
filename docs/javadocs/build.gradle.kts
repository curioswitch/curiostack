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

plugins {
  java
}

dependencies {
  implementation(project(":common:grpc:protobuf-jackson"))
  implementation(project(":common:server:framework"))
}

tasks {
  val javadoc = named<Javadoc>("javadoc") {
    listOf(
        ":common:grpc:protobuf-jackson",
        ":common:server:framework",
        ":common:testing:framework"
    ).forEach {
      val proj = project(it)
      proj.plugins.withType(JavaPlugin::class) {
        val task = proj.tasks.named<Javadoc>("javadoc").get()

        // A bit repetitive, but we go ahead and depend on subproject javadoc to ensure any
        // of its dependencies (e.g., code generation) run.
        dependsOn(task)
        source(task.source)
        classpath = classpath.plus(task.classpath)
      }
    }
  }

  named("assemble") {
    dependsOn(javadoc)
  }
}
