FROM openjdk:11-jre-slim

ADD https://storage.googleapis.com/cloud-profiler/java/latest/profiler_java_agent.tar.gz /tmp/gcloud/profiler_java_agent.tar.gz

RUN mkdir /opt/cprof && tar -xzf /tmp/gcloud/profiler_java_agent.tar.gz -C /opt/cprof && rm -rf /tmp/gcloud
