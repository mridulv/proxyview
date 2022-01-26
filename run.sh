sbt proxyview-client/docker:publishLocal
sbt proxyview-server/docker:publishLocal

docker-compose -f docker/docker-compose.yml up
