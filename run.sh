#!/bin/sh

# TAG=$SOME_TAG ./run.sh

sbt proxyview-client/docker:publishLocal
sbt proxyview-server/docker:publishLocal

docker-compose -f deploy/docker/docker-compose.yml up
