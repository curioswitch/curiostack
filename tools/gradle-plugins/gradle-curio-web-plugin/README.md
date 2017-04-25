# gradle-curio-web-plugin

A highly opinionated plugin for easily defining projects that build a static web site using npm.
Built output will be packaged into a jar for easy serving from a Java sercver.

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
        classpath 'org.curioswitch.curiostack:gradle-curio-web-plugin:0.0.1'
    }
}

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
