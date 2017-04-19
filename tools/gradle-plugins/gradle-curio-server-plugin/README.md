# gradle-curio-server-plugin

A heavily opinionated plugin for defining GRPC APIs in a gradle project with minimal configuration.
The plugin will set up a project to package Java applications into a docker image and deploy to a
Kubernetes cluster.

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
        classpath 'org.curioswitch.curiostack:gradle-curio-server-plugin:0.0.1'
    }
}

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
