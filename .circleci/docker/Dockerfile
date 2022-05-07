FROM openjdk:8-jdk-slim

RUN echo "Curl install" && \
    DEBIAN_FRONTEND=noninteractive \
    apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN echo "SBT 0.13.16 install" && \
    curl -sSL https://scala.jfrog.io/artifactory/debian/sbt-0.13.16.deb -o sbt.deb && \
    dpkg -i sbt.deb && \
    apt-get install -f && \
    rm sbt.deb

RUN echo "Packages install" && \
    DEBIAN_FRONTEND=noninteractive \
    apt-get update && apt-get install -y \
      git \
      python \
      python3-pip \
      python3-venv \
      awscli \
      jq \
      shellcheck \
    && rm -rf /var/lib/apt/lists/*

RUN echo "Docker install" && \
    URL="https://download.docker.com/linux/static/stable/x86_64/docker-17.09.0-ce.tgz" && \
    echo "Downloading docker from ${URL} ..." && \
    curl -fsL ${URL}  | tar xfz - --strip-components 1 -C /usr/local/bin
