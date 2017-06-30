FROM debian:jessie-backports
MAINTAINER gustavo.amigo@gmail.com

RUN apt-get update; \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends -t jessie-backports \
        bash \
        curl \
        git \
        openjdk-8-jdk \
        openssh-client \
        openssl \
        tar; \
    curl -sL https://deb.nodesource.com/setup_7.x | bash -; \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends nodejs

ENV SBT_VERSION 0.13.15
ENV SBT_HOME /usr/local/sbt
ENV PATH ${PATH}:${SBT_HOME}/bin

# Install sbt
RUN curl -sL "http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz" | gunzip | tar -x -C /usr/local --strip-components=1 && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built

WORKDIR /app