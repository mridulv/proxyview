# proxyview

## Introduction
Proxyview helps you to make HTTP Requests to all your internally deployed services which cannot be reached out from local system.

## Details

## Getting Started

### Building a Java Distribution
```
sbt proxyview-client/dist
sbt proxyview-server/dist
```

### Building a Docker Distribution

```
export TAG=0.1
sbt proxyview-client/docker:publishLocal
sbt proxyview-server/docker:publishLocal
```

### Running on Local Machine

- Installing Docker
- Running `run.sh` file with appropriate `TAG` Env variable. This will create the docker images and bring up a local proxyview-server + proxyview-client setup.
- Run this curl command `curl -v -H 'Host: localhost-sample2:80' -H 'auth-token: xasxxx-yyyy-xxxx-yyyy' localhost:8080`

## Technical Details

Here is a basic architecture explaining the internals of Proxyview

<img src="https://i.ibb.co/YTSxF40/proxyview.png" alt="Overview" style="float: left; margin-right: 10px;" width="750" height="700"/> 
