# Curiostack Project Brief

###### Goals, concepts, challenges, timeline, inspiration

### What is it?

Curiostack is a full-featured framework for creating new web applications and experiences. It is
designed to not stop at any single need - writing code, setting up monitoring, automatic deployment,
and any other aspect of product development is covered. Users should be able to use the stack to
focus on writing business logic for a service, not production / dev-ops, and follow all the best
practices for free.

### Who is it for?

- Inexperienced developers not yet familiar with development best practices
- Developers that want to get up-and-running quickly without sacrificing production-readiness
- Startups with a small development team but large aspirations for scale

### Why build it?

To have a platform for quickly developing Curioswitch services in a production-ready manner.

To allow anyone else to take advantage of the platform to be able to focus on creation rather than
operations.

### Non-goals

To create a flexible, plugabble stack. Curiostack is heavily opinionated to achieve a simple and
quick starter experience. Those that already have a good grasp of best practices and have systems
in place for Curiostack's features don't need it.

To support multiple cloud providers. Currently Curiostack only targets Google Cloud Platform (GCP).
GCP has many high-quality, scalable features and fits well with the Curiostack goals. AWS support
would also be interesting but is not currently a priority. If undertaken, it would need to have
1-1 feature parity with GCP support.

### Team

Choko, and anyone else in the world that would like to contribute.

### Features
- Simple, generally one-step, commands for setting up a cloud project with a Google Container 
Engine (GKE) cluster, creating databases, and other common cluster operations.
- Build system support for setting up client/server API-based services with minimal boilerplate.
- Automatic test and beta deployment of services.
- Security first. All endpoints will use HTTPs, and security best practices will be baked in without
the need for user configuration.

### Challenges and open questions

- A more thorough design document is needed for probing the technical aspects of Curiostack.
- Is there a finer balance needed between easy-to-use pre-baked opinions and user configurability?
- Is the minimum cost of a Google Container Engine cluster too high for experimentation?

### Timeline

##### Milestone 1
- Single command that creates a GCP project, GKE cluster, sets up Google Container Builder for
autobuild and deploy
- An authentication service based on Firebase - LINE login support is nice to have
- An easy to use, well-explained process for releasing new versions of services
- A command for generating the boilerplate for a GRPC API, API server, and web client
- Web client boilerplate based on react-boilerplate

##### Milestone 2
- Logging and monitoring fully set up automatically using Stackdriver
- RPC authentication and ACL policies
- RPC tracing using Stackdriver

##### Milestone 3
- A framework for sharing code effectively between web, Android, iOS
- Extend boilerplate generation to include native apps

##### Milestone 3
- Experiment framework, with a full experience for running, updating, and viewing results of 
experiments.

And more. Curiostack will continue to evolve to include modern best practices.

### Research & Inspiration

- Spring Cloud. A full-featured development experience for Java. Heavily tied to CloudFoundry. For
CloudFoundry users, no question not to use it.

- Google. A general summary of the goals of Curiostack could be that general users should have all
the tools at their tips that Google engineers have.

- Netflix OSS. Netflix components contain features for many of the pains found in service
development. Curiostack aims to have similar features in a single, well-integrated experience.
