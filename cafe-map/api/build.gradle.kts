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

import org.curioswitch.gradle.helpers.platform.OperatingSystem
import org.curioswitch.gradle.helpers.platform.PlatformHelper
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil

plugins {
    id("org.curioswitch.gradle-grpc-api-plugin")
    java
}

base {
    archivesBaseName = "cafe-map-api"
}

protobuf {
    languages {
        register("csharp") {
            outputDir.set(file("build/generated/csharp"))
        }

        register("grpc_csharp") {
            outputDir.set(file("build/generated/csharp"))
            plugin {
                val osPrefix = when (PlatformHelper().os) {
                    OperatingSystem.LINUX -> "linux"
                    OperatingSystem.MAC_OSX -> "macosx"
                    OperatingSystem.WINDOWS -> "windows"
                    else -> "unknown"
                }
                val pluginPath = "tools/${osPrefix}_x64/grpc_csharp_plugin${if (PlatformHelper().os == OperatingSystem.WINDOWS) ".exe" else ""}"
                path.set(file(DownloadedToolManager.get(project).getToolDir("grpc_csharp").resolve(pluginPath)))
            }
        }
    }
}

grpc {
    web.set(true)
    webPackageName.set("@curiostack/cafemap-api")
}

tasks {
    val generateProto by named("generateProto") {
        dependsOn(DownloadToolUtil.getSetupTask(project, "grpc_csharp"));
    }
    val copyCsharpProto by registering(Copy::class) {
        dependsOn(generateProto)

        from("build/generated/csharp")
        into("../client/unity/Assets/Generated/Proto")
    }
    val assemble by named("assemble") {
        dependsOn(copyCsharpProto)
    }
}
