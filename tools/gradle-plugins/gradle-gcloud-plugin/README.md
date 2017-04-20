# gradle-gcloud-plugin

A plugin for allowing gcloud SDK commands to be run from gradle, with predefined tasks for common 
operations, including setting up a new cluster and build environment. gradle-gcloud-plugin attempts
to make it as easy as possible to get to a production-ready state using Google Cloud.

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
        classpath 'org.curioswitch.curiostack:gradle-gcloud-plugin:0.0.1'
    }
}

apply plugin: 'org.curioswitch.gradle-gcloud-plugin'
```

The plugin should only be applied to the root project for a multi-project build.

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
  version = '151.0.1'
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

# Generates a cloudbuild.yaml containing steps to build all projects with [curio-server-plugin](https://github.com/curioswitch/curiostack/tree/master/tools/gradle-plugins/gradle-curio-server-plugin)
$ ./gradlew :gcloudGenerateCloudBuild
```
