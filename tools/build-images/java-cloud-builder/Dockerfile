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

FROM openjdk:11-jdk-slim AS build

RUN apt-get update && \
    apt-get install -y python-pip && pip install crcmod

FROM google/cloud-sdk:slim AS gcloud

FROM openjdk:11-jdk-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends python2.7-minimal libpython2.7-stdlib liblz4-tool libatomic1 bzip2 && \
    update-alternatives --install /usr/bin/python python /usr/bin/python2.7 1 && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /usr/local/lib/python2.7/dist-packages/ /usr/local/lib/python2.7/dist-packages/
COPY --from=gcloud /usr/lib/google-cloud-sdk /usr/lib/google-cloud-sdk

ENV PATH="/usr/lib/google-cloud-sdk/bin:${PATH}"
