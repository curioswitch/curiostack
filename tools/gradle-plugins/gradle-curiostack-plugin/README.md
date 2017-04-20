# gradle-curiostack-plugin

A highly opinionated plugin for setting up a gradle codebase using Curiostack conventions. Defines 
shared configuration and applies other useful plugins in an aim to be the only plugin in a root 
project, while providing best practices and a good development experience.

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
        classpath 'org.curioswitch.curiostack:gradle-curiostack-plugin:0.0.1'
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

TODO: Also automatically setup an initial ```.baseline``` directory. For now, just copy the
directory from [here](https://github.com/curioswitch/curiostack/tree/master/.baseline).
