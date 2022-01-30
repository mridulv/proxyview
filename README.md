# proxyview

## Introduction
Proxyview helps you to make HTTP Requests to all your internally deployed services which cannot be reached out from your 
browser.

## Build

## Building a Java Distribution
```
sbt proxyview-client/dist
sbt proxyview-server/dist
```

## Building a Docker Distribution

```
export TAG=0.1
sbt proxyview-client/docker:publishLocal
sbt proxyview-server/docker:publishLocal
```

## Running on Local Machine

- Installing Docker
- Running `run.sh` file with appropriate `TAG` Env variable. This will create the docker images and bring up a local proxyview-server + proxyview-client setup.  



