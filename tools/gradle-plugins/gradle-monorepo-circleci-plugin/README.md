# gradle-monorepo-circleci-plugin

A plugin to speed up continuous integration of a monorepo on CircleCI by only building changed 
targets. Introspects the CircleCI environment variables and git repository to determine what files
were changed for the current build and only runs tests for those projects and their dependents.

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
        classpath 'org.curioswitch.curiostack:gradle-monorepo-circleci-plugin:0.0.1'
    }
}

apply plugin: 'org.curioswitch.gradle-monorepo-circleci-plugin'
```

This plugin must be applied to the root project of a multi-project build.

With the plugin applied, change the test command in your circle.yml to continuousTest to use the
plugin's change detection.

```yaml
test:
  override:
  - ./gradlew continuousTest --stacktrace
```
