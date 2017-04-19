# An image that inherits from itself. Any files stored outside the working directory volume, like
# dependency jars, will be saved into the image. By rebuilding itself before any build of the
# repository, dependency jars can be cached continuously.

# TODO(choko): Define a condition to clear and recreate the cache from scratch. The smartest
# approach would look at the current source repositories dependencies and compare with the cached
# dependencies, a basic approach would be to store the original build timestamp and look at it

# TODO(choko): Define and document how to bootstrap this image from openjdk:8-jdk

FROM gcr.io/curioswitch-cluster/java-cloud-builder:latest

ADD . /src
RUN cd /src && ./gradlew resolveDependencies && cd / && rm -rf /src

ENTRYPOINT /bin/bash
