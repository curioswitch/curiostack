# gradle-curiostack-plugin

A highly opinionated plugin for setting up a gradle codebase using Curiostack conventions. Defines 
shared configuration and applies other useful plugins in an aim to be the only plugin in a root 
project, while providing best practices and a good development experience.

It is generally expected that `gradle-curiostack-plugin` is applied when using any of the other
plugins listed below. They should still generally work without it but will be more difficult to use
and may have unexpected effects - applying plugins without `gradle-curiostack-plugin` is only
supported on a best-effort basis.

## Usage

This plugin is currently only in JCenter - it will also be published to the Gradle Plugin Portal
soon and will then support the new plugin IDL.

In the meantime, use the old plugin syntax, making sure ```jcenter()``` is in the repositories.

```groovy

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'org.curioswitch.curiostack:gradle-curiostack-plugin:0.0.17'
    }
}

apply plugin: 'org.curioswitch.gradle-curiostack-plugin'
```

The plugin must be applied to the root of a multi-project build, when doing so, no other curiostack
plugins need to be applied to the root project.

The required configuration for Curiostack is the project group for generated artifacts and basic
gcloud settings. For example,

```groovy
allprojects {
    project.group = 'org.curioswitch.curiostack'
}

gcloud {
    clusterBaseName = 'curioswitch'
    cloudRegion = 'asia-northeast1'
    clusterKubernetesVersion = '1.6.1'
}
```

That's it! The project will be configured with annotation processing, include all other Curiostack
gradle plugins (e.g., gradle-curio-server-plugin, gradle-grpc-api-plugin) for use in subprojects,
be ready to use optimized testing on CircleCI, and provide tasks for gcloud management. See
individual plugin READMEs for their usage.

To get started with development in IntelliJ, just

```bash
$ ./gradlew idea
```

to automatically generate a project with code style settings and copyright set up.

### gradle-gcloud-plugin

A plugin for allowing gcloud SDK commands to be run from gradle, with predefined tasks for common 
operations, including setting up a new cluster and build environment. gradle-gcloud-plugin attempts
to make it as easy as possible to get to a production-ready state using Google Cloud.

```groovy
apply plugin: 'org.curioswitch.gradle-gcloud-plugin'
```

This plugin is automatically applied when applying `gradle-curiostack-plugin`. It should only be
applied to the root project of a multi-project build if applied separately.

The plugin will by default automatically download the Google Cloud SDK into the project's 
```.gradle``` directory and use it for all commands. This means there is no extra dependency for a
project's user to use gcloud.

The plugin adds a ```gcloud``` extension which can be used to configure the GCP settings to be used.
The required settings are ```clusterBaseName```, which is a prefix added to automatically generated
names for cluster resources like the GCP project and GKE cluster, and ```cloudRegion``` which sets
the region for the cluster to be brought up in. Currently only simple heuristics are used to
determine the zones for a cluster (region-a and region-c), so for regions where those zones are not
present, ```clusterZone``` and ```clusterAdditionalZones``` must be set.

The default settings for the cluster are only appropriate for experimentation to minimize cost
during this time. When done experimenting and ready to set up a production-ready cluster with 4
standard nodes, make sure to set the parameters appropriately. An example production configuration
is given below.

```groovy
gcloud {
  clusterBaseName = 'curioswitch'
  cloudRegion = 'asia-northeast1'
  clusterZone = 'asia-northeast1-a' // default value
  clusterAdditionalZones = 'asia-northeast1-c' // default value
  clusterNumNodesPerZone = 2
  // Check release notes to see what the latest version of Kubernetes is. By default, it will use
  // the GKE default, which is not always the latest version.
  // https://cloud.google.com/container-engine/release-notes
  clusterKubernetesVersion = '1.6.1'
  // If you know you will need more resources, it is recommended to raise the machine size rather
  // than the number of nodes - GKE master is free up to 5 nodes.
  // Check pricing when determining a node size.
  // https://cloud.google.com/compute/pricing
  clusterMachineType = 'n1-standard-1'
  clusterProject = 'curioswitch-cluster' // default value based on clusterBaseName
  clusterName = 'curioswitch-cluster' // default value based on clusterBaseName
  containerRegistry = 'asia.gcr.io' // default value based on cloudRegion
  sourceRepository = 'curioswitch-source' // default value based on clusterBaseName
  download = true // default value, instructs plugin to automatically download the gcloud SDK
  // Default value - check release notes to see what the latest version is and specify it here.
  // https://cloud.google.com/sdk/docs/release-notes
  version = '164.0.0'
}

```

The plugin adds several tasks to aid with setting up a GCP cluster. Note, some tasks require opening
the GCP web console a single time to enable the required functionality in the project. A way to
automate this hasn't been found yet, but is under investigation. Worst case, we will add links to
open to the tasks themselves to make it easier to activate the APIs.

```bash
# Downloads and sets up the gcloud sdk
$ ./gradlew :gcloudSetup

# Creates the GCP project that will host the cluster, continuous build, etc
$ ./gradlew :gcloudCreateClusterProject

# Creates a source repository to mirror for continuous build. Requires interaction in web console
# to link to Github.
$ ./gradlew :gcloudCreateSourceRepository

# Creates the Kubernetes cluster. Requires having opened the container engine section of the web
# console once before executing.
$ ./gradlew :gcloudCreateCluster

# Sets up kubectl credentials for accessing the cluster
$ ./gradlew :gcloudLoginToCluster

# Generates a cloudbuild.yaml containing steps to build all projects with `curio-server-plugin`.
# Note the lack of a ':' - this forces gradle to configure all projects in the build, rather than
# just a single one. This is required to be able to find the server projects while generating the
# config.
$ ./gradlew gcloudGenerateCloudBuild
```

A settings plugin is also provided to use Google Cloud Storage as a remote build cache. This is
currently alpha-level - it probably works ok, but needs more testing under load.

To set up,

```groovy
build.gradle (root)
gcloud {
    // This must match the bucket in settings.gradle, it is not possible to share the config
    // between settings and a project.
    // If unset, this defaults to project-id-gradle-build-cache
    buildCacheStorageBucket = 'curioswitch-gradle-build-cache'
}
```

```bash
# Sets up the Google Storage Bucket which will be used by the build cache, only needs to be run
# once.
$ ./gradlew :createBuildCacheBucket
```

```groovy
settings.gradle
// Use same buildscript as build.gradle to keep things simple.
buildscript {
    repositories {
        jcenter()
        maven {
            url 'https://plugins.gradle.org/m2/'
        }
    }
    dependencies {
        classpath 'org.curioswitch.curiostack:gradle-curiostack-plugin:0.0.17'
    }
}

apply plugin: 'org.curioswitch.gradle-gcloud-build-cache-plugin'

def isCiServer = System.getenv().containsKey('CI')

buildCache {
    local {
        enabled = !isCiServer
    }
    remote(org.curioswitch.gradle.plugins.gcloud.buildcache.CloudStorageBuildCache) {
        bucket = 'curioswitch-gradle-build-cache'
        push = isCiServer
    }
}
```

### gradle-curio-web-plugin

A highly opinionated plugin for easily defining projects that build a static web site using npm.
Built output will be packaged into a jar for easy serving from a Java server.

Apply this plugin to any project that builds web artifacts (e.g., webpack).

```groovy
apply plugin: 'org.curioswitch.gradle-curio-web-plugin'
```

Then you just have to add a ```web``` block specifying the java package to use, this should usually
be relatively unique to prevent collisions when multiple web packages are served from the same
server.
```groovy
web {
    javaPackage = 'org.curioswitch.service.client.web'
}
```

The plugin will configure a ```buildWeb``` task to run ```npm run build``` and contents of
```build/web``` will be packaged by the normal ```jar``` task with the specified package. The NPM 
build must output into ```build/web``` for this to work.

The web build task is configured to be cacheable - it is highly recommended to also enable the
build cache in `gradle-gcloud-plugin` to take advantage of this as webpack is generally quite slow.

### gradle-curio-server-plugin

A heavily opinionated plugin for defining GRPC APIs in a gradle project with minimal configuration.
The plugin will set up a project to package Java applications into a docker image and deploy to a
Kubernetes cluster.

Apply this plugin to any project that builds a Java server application.

```groovy
apply plugin: 'org.curioswitch.gradle-curio-server-plugin'
```

The base plugin's ```archivesBaseName``` is used to control the name of the built image and deployed
container, so it should be set to something easily recognizable. Then just set up the deployment
and docker configuration blocks. For an alpha deployment, the defaults are reasonable, though to get
up and running right away, it is possible to set the namespace to ```default``` (it is highly
recommended to use unique namespaces for services / areas when not experimenting).

```groovy
deployment {
    types {
        alpha {
            namespace = 'default'
        }
        prod {
            namespace = 'service-prod'
            deploymentName = 'my-service-prod'
            replicas = 2
            cpu = '0.5'
            memoryMb = 512
            image = 'gcr.io/my-cluster/my-service:20170101'
        }
    }
}

// Optionally set a maintainer for the docker image (not really important)
docker {
    javaApplication {
        maintainer = 'Choko (choko@curioswitch.org)'
    }
}
```

### gradle-grpc-api-plugin

A highly opinionated plugin for defining GRPC APIs in a gradle project with minimal configuration.
Adding the plugin to the package is enough to set up the protobuf gradle plugin to generate code,
including GRPC stubs. A descriptor set with docstrings is also generated for use in documentation
services (e.g., armeria).

Apply this plugin to any project that compiles protobuf - despite its name, it's reasonable to use
even if the protos only define messages, not GRPC services, to take advantage of automatically
setting up correct output directories for use in IntelliJ.

```groovy
apply plugin: 'org.curioswitch.gradle-grpc-api-plugin'
```

The plugin uses the Spring dependency-management-plugin for resolving the version of GRPC and
protobuf you would like to use. Specify the versions with something like

```groovy
dependencyManagement {
    dependencies {
        dependency 'com.google.protobuf:protoc:3.2.0'
        dependencySet(group: 'io.grpc', version: '1.2.0') {
            entry 'grpc-core'
            entry 'grpc-protobuf'
            entry 'grpc-stub'
        }
    }
}

```

This is enough to set up the protobuf gradle plugin appropriately. Add a ```proto``` subdirectory 
under ```main``` and add some proto files, and you're ready to go.

An example usage can be found in [curio-auth-api](https://github.com/curioswitch/curiostack/blob/master/auth/api/build.gradle).
The buildscript configuration is in the [top-level build file](https://github.com/curioswitch/curiostack/blob/master/build.gradle#L41).
