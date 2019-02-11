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
    id("org.curioswitch.gradle-curio-server-plugin")
}

base {
    archivesBaseName = "cafe-map-server"
}

application {
    mainClassName = "org.curioswitch.cafemap.server.CafeMapServiceMain"
}

dependencies {
    compile(project(":cafe-map:api"))
    compile(project(":cafe-map:client:web"))

    compile("org.curioswitch.curiostack:curio-server-framework")

    annotationProcessor("com.google.dagger:dagger-compiler")
    annotationProcessor("org.immutables:value-annotations")
    compileOnly("org.immutables:value")

    testAnnotationProcessor("com.google.dagger:dagger-compiler")
    testAnnotationProcessor("org.immutables:value-annotations")
    testCompileOnly("org.immutables:value")
}

deployment {
    types {
        alpha {
            namespace = 'cafemap-dev'
        }
    }
}
