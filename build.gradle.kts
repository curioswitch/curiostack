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

import com.jfrog.bintray.gradle.BintrayExtension
import net.ltgt.gradle.nullaway.NullAwayExtension
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin

allprojects {
    project.group = "org.curioswitch.curiostack"

    plugins.withId("java") {
        project.tasks.register<DependencyReportTask>("allDeps")

        configure<NullAwayExtension> {
            annotatedPackages.add("org.curioswitch")
        }
    }

    plugins.withType(LicensePlugin::class) {
        configure<LicenseExtension> {
            skipExistingHeaders = true
        }
    }

    plugins.withType(org.gradle.api.publish.maven.plugins.MavenPublishPlugin::class) {
        project.plugins.apply("com.jfrog.bintray")
        // This should happen automatically, not clear why not.
        tasks.named("bintrayUpload") {
            dependsOn(tasks.named("publishToMavenLocal"))
        }

        val bintrayUser = project.findProperty("bintray.user") as String?
        val bintrayKey = project.findProperty("bintray.key") as String?

        afterEvaluate {
            configure<BintrayExtension> {
                publish = true
                user = bintrayUser
                key = bintrayKey
                pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
                    name = the<BasePluginConvention>().archivesBaseName
                    repo = "curiostack"
                    userOrg = "curioswitch"
                    setLicenses("MIT")
                    vcsUrl = "https://github.com/curioswitch/curiostack.git"
                    githubRepo = "curioswitch/curiostack"
                    version(delegateClosureOf<BintrayExtension.VersionConfig> {
                        name = project.version as String
                        gpg(delegateClosureOf<BintrayExtension.GpgConfig> {
                            // Use bintray"s keys for signing since there isn"t much of a difference
                            // in security vs giving them your own private keys - either way bintray
                            // is trusted as the identity provider of your packages.
                            sign = true
                        })
                    })
                })
                setPublications(*the<PublishingExtension>().publications.map{ it.name }.toTypedArray())
            }

            configure<PublishingExtension> {
                publications.withType<MavenPublication> {
                    groupId = project.group as String
                    artifactId = the<BasePluginConvention>().archivesBaseName

                    // Plugin and BOM publications do not need this.
                    if (name == "maven" && project.name != "curiostack-bom") {
                        from(components["java"])
                    }

                    pom {
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("chokoswitch")
                                name.set("Choko")
                                email.set("choko@curioswitch.org")
                                organization.set("CurioSwitch")
                                organizationUrl.set("https://github.com/curioswitch/curiostack")
                            }
                        }

                        scm {
                            connection.set("scm:git:git://github.com/curioswitch/curiostack.git")
                            developerConnection.set("scm:git:ssh://github.com:curioswitch/curiostack.git")
                            url.set("https://github.com/curioswitch/curiostack")
                        }
                    }
                }
            }
        }
    }
}

gcloud {
    clusterBaseName.set("curioswitch")
    clusterName.set("curioswitch-cluster-jp")
    cloudRegion.set("asia-northeast1")
}

ci {
    releaseTagPrefixes {
        register("RELEASE_SERVERS_") {
            project(":auth:server")
            project(":eggworld:server")
        }
    }
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}

tasks.named("yarn").configure {
    // Hack to make sure yarn symlinks are set up after building the base packages.
    finalizedBy(":eggworld:client:web:install")
}

tools {
    create("grpc_csharp") {
        version.set("2.31.0")
        artifact.set("Grpc.Tools")
        baseUrl.set("https://www.nuget.org/api/v2/package/")
        artifactPattern.set("[artifact]/[revision]")
    }
}
