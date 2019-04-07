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
    `java-library`
    `maven-publish`
}

base {
    archivesBaseName = "armeria-google-map-services"
}

dependencies {
    compileOnly(project(":common:curio-helpers"))

    api("com.google.maps:google-maps-services:0.9.3")
    api("com.typesafe:config")

    implementation("com.google.guava:guava")
    implementation("com.linecorp.armeria:armeria")

    annotationProcessor("com.google.dagger:dagger-compiler")
    compileOnly("com.google.dagger:dagger")

    annotationProcessor("org.immutables:value")
    compileOnly("org.immutables:value-annotations")
}

publishing {
    publications {
        register("maven", MavenPublication::class) {
            pom {
                name.set("armeria-google-map-services")
                description.set("A Google Maps API client, based on Armeria.")
                url.set("https://github.com/curioswitch/curiostack/tree/master/tools/" +
                    "common/google-cloud/maps-services")
            }
        }
    }
}
