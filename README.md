# curiostack
Full stack to help satisfy curiosity

## Set up

See gradle-curiostack-plugin [README](tools/gradle-plugins/gradle-curiostack-plugin/README.md) for setting up the gradle plugins.

## Developing

Curiostack requires JDK9. For Ubuntu / Debian users, the included OpenJDK9 will not work due to an
embarrassing [bug](https://bugs.launchpad.net/ubuntu/+source/openjdk-9/+bug/1727002). To work around
this, install the Oracle JDK9 from a PPA.

```bash
$ sudo add-apt-repository ppa:webupd8team/java
$ sudo apt-get update
$ sudo apt-get install oracle-java9-installer
```

Curiostack only has a dependency on Java. However, IntelliJ is highly recommended for development.
The free community edition is sufficient for Java development, while the pro edition may provide a
more integrated IDE for both server and client development. Many users will choose to use IntelliJ
community edition for server side development and Atom or Visual Studio Code for client side.

To work on IntelliJ, clone the repository and in a command line, run

```
$ ./gradlew idea
```

to generate initial IntelliJ configuration. Then open up the folder in IntelliJ and say yes when it
asks you to link the project.

All code-style, license, etc setup will be done automatically, and you're ready to code.
