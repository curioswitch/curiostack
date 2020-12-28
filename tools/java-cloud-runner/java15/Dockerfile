#
# MIT License
#
# Copyright (c) 2020 Choko (choko@curioswitch.org)
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

FROM adoptopenjdk:15-jdk-hotspot AS jre

RUN apt-get -y update && apt-get -y install binutils

RUN cd / && jlink --no-header-files --no-man-pages --compress=2 --strip-debug \
    --add-modules java.base \
    --add-modules java.desktop \
    --add-modules java.instrument \
    --add-modules java.logging \
    --add-modules java.management \
    --add-modules java.management.rmi \
    --add-modules java.naming \
    --add-modules java.sql \
    --add-modules java.sql.rowset \
    --add-modules jdk.crypto.ec \
    --add-modules jdk.naming.dns \
    --add-modules jdk.unsupported \
    --output jre

FROM debian:bullseye AS debian

RUN apt-get -y update && apt-get -y install ca-certificates-java

ADD https://storage.googleapis.com/cloud-profiler/java/latest/profiler_java_agent.tar.gz /tmp/gcloud/profiler_java_agent.tar.gz
ADD https://github.com/GoogleCloudPlatform/cloud-debug-java/releases/download/v2.26/compute-java_debian-wheezy_cdbg_java_agent_service_account.tar \
  /tmp/gcloud/compute-java_debian-wheezy_cdbg_java_agent_service_account.tar

RUN mkdir /opt/cprof && \
  tar -xzf /tmp/gcloud/profiler_java_agent.tar.gz -C /opt/cprof && \
  mkdir /opt/cdbg && \
  tar -xf /tmp/gcloud/compute-java_debian-wheezy_cdbg_java_agent_service_account.tar -C /opt/cdbg && \
  keytool -importkeystore -srckeystore /etc/ssl/certs/java/cacerts -destkeystore /etc/ssl/certs/java/cacerts.jks -deststoretype JKS -srcstorepass changeit -deststorepass changeit -noprompt && \
  mv /etc/ssl/certs/java/cacerts.jks /etc/ssl/certs/java/cacerts && \
  /var/lib/dpkg/info/ca-certificates-java.postinst configure

FROM gcr.io/distroless/cc:debug

COPY --from=debian /etc/ssl/certs/java/ /etc/ssl/certs/java/

COPY --from=debian /lib/x86_64-linux-gnu/libz.so.1.2.11 /lib/x86_64-linux-gnu/libz.so.1.2.11
RUN ["/busybox/sh", "-c", "ln -s /lib/x86_64-linux-gnu/libz.so.1.2.11 /lib/x86_64-linux-gnu/libz.so.1"]

ENV JAVA_HOME=/usr/lib/jvm/java-15-adoptopenjdk-amd64-slim
COPY --from=jre /jre /usr/lib/jvm/java-15-adoptopenjdk-amd64-slim
RUN ["/busybox/sh", "-c", "ln -s /usr/lib/jvm/java-15-adoptopenjdk-amd64-slim/bin/java /usr/bin/java"]

COPY --from=debian /opt/cprof /opt/cprof
COPY --from=debian /opt/cdbg /opt/cdbg

ENTRYPOINT ["/usr/bin/java", "-jar"]
