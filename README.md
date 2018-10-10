# curiostack
Full stack to help satisfy curiosity

## Set up

See gradle-curiostack-plugin [README](tools/gradle-plugins/gradle-curiostack-plugin/README.md) for setting up the gradle plugins.

## Developing

Curiostack requires JDK8 and JDK11.

```bash
$ sudo add-apt-repository ppa:openjdk-r/ppa
$ sudo apt-get update
$ sudo apt-get install openjdk-8-jdk-headless
$ sudo apt-get install openjdk-11-jdk-headless
$ sudo  update-java-alternatives --set java-1.11.0-openjdk-amd64
```

First run

```bash
$ ./gradlew :setup
```

to set up all required tools, by downloading python, gcloud, node, golang, and inserting a PATH setting into zshrc and bashrc. There won't be any version conflicts between e.g., system-installed nodejs and gradle-installed nodejs as gradle-installed nodejs will be the one on the path.

Note, the PATH setting is inserted at the end of your startup script to ensure stack tools get priority. If you use pyenv and want to continue using it, you will want to move pyenv settings below it - also if you really don't like tools messing with your PATH feel free to delete it. Gradle tasks will continue to be configured to use the downloaded tools regardless of the user PATH settings, though non-gradle command invocations may not work correctly, if you replace the PATH settings YMMV and it's not recommended.

### IDE
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
