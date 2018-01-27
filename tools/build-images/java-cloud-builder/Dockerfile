#
# MIT License
#
# Copyright (c) 2017 Choko (choko@curioswitch.org)
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

# An image that inherits from itself. Any files stored outside the working directory volume, like
# dependency jars, will be saved into the image. By rebuilding itself before any build of the
# repository, dependency jars can be cached continuously.

# TODO(choko): Define a condition to clear and recreate the cache from scratch. The smartest
# approach would look at the current source repositories dependencies and compare with the cached
# dependencies, a basic approach would be to store the original build timestamp and look at it

ARG cluster
FROM gcr.io/${cluster}/java-cloud-builder

ADD . /src
RUN cd /src && ./gradlew resolveDependencies yarn --stacktrace

FROM gcr.io/${cluster}/java-cloud-builder:bootstrap

COPY --from=0 /root/.gradle/wrapper /root/.gradle/wrapper
COPY --from=0 /root/.npm /root/.npm
COPY --from=0 /usr/local/share/.cache /usr/local/share/.cache
COPY --from=0 /root/.gradle/caches /root/.gradle/caches
