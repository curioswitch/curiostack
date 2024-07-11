This repository is archived: it has not had any maintenance in several years with 1 or 0 current users.
curioswitch has migrated to Go as its main language and the successor can be found at
[go-curiostack](https://github.com/curioswitch/go-curiostack), with a full service in development using it
at [tasuke](https://github.com/curioswitch/tasuke).

# curiostack
Very full stack to help satisfy curiosity


Curiostack enforces best practices without the headaches. All users
- Have a CI that only builds affected projects. Let the monorepo grow as needed.
- Deploy alpha servers on every commit. All changes can be checked right away.
- Are secure by default. Servers only accept TLS connections, even on localhost. Services
require TLS mutual authentication.
- Always have the latest tools. The build automatically downloads tools and versions are 
tied to the repo, not the developer's machine.
- Can write an API focusing on the business logic, without restrictions of serverless tools.
- Have fast, modern web clients that have code to render pages, not boilerplate.

and more!

## Developer resources

Developers should start at our developer portal, [https://developers.curioswitch.org](https://developers.curioswitch.org).
The content is still light, but the codelabs should help get started with concepts quickly.

Also until the coverage of the codelabs goes up, it is also important to go through this `README` and
maybe even some of the code to understand what's available. 

## Company Resources

Looking for guidelines and documents outlining best practices for designing a company, not just the
engineering codebase? Check out our resources [here](./docs/company) which can hopefully help.

## Have a question?

Feel free to open an [issue](https://github.com/curioswitch/curiostack/issues/new) if you have any questions. 
You are also welcome to join our [Slack workspace](https://join.slack.com/t/curiostack/shared_invite/enQtODMxODkzMDg4NDM1LTllMmZiNzU3MTUxZjYzYTA2YjdlYWNkYTM1YjBlMTY1ODMxN2YxYTI2ZjFhOWZiYmVjZGY4ZTZkZWViNDRlZWU) 
and ask there.

## Set up

See gradle-curiostack-plugin [README](tools/gradle-plugins/gradle-curiostack-plugin/README.md) for setting up the gradle plugins.

## Developing

Curiostack only requires `bash`.

First run

```bash
$ ./gradlew :setup
```

to set up all required tools, by downloading openjdk, python, gcloud, node, golang, etc, and inserting a PATH setting into zshrc and bashrc. There won't be any version conflicts between e.g., system-installed nodejs and gradle-installed nodejs as gradle-installed nodejs will be the one on the path when running Gradle invocations. In addition, any `bash` invocations of commands when inside the repository will use the managed tool. Outside the repository, system tools will be used as normal.

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
