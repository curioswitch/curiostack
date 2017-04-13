# gradle-grpc-api-plugin

A highly opinionated plugin for defining GRPC APIs in a gradle project with minimal configuration.
Adding the plugin to the package is enough to set up the protobuf gradle plugin to generate code,
including GRPC stubs. A descriptor set with docstrings is also generated for use in documentation
services (e.g., armeria).

## Usage

This plugin is not in JCenter or the Gradle Plugin Portal yet - it is currently in the process of
being approved for inclusion in JCenter, after which it will also be published to the Gradle Plugin
Portal.

In the meantime, you can add the Curiostack bintray repository to use this plugin.

```groovy

buildscript {
    repositories {
        jcenter()
        maven {
            url  'http://dl.bintray.com/curioswitch/curiostack'
        }
    }
    dependencies {
        classpath 'org.curioswitch.curiostack:gradle-grpc-api-plugin:0.0.1'
    }
}

apply plugin: 'org.curioswitch.gradle-grpc-api-plugin'
```

This will set up the protobuf gradle plugin appropriately. Add a ```proto``` subdirectory under
```main``` and add some proto files, and you're ready to go.

An example usage can be found in [curio-auth-api](https://github.com/curioswitch/curiostack/blob/master/auth/api/build.gradle).
The buildscript configuration is in the [top-level build file](https://github.com/curioswitch/curiostack/blob/master/build.gradle#L41).
