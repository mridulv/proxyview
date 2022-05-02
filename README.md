# proxyview

## Introduction
Proxyview helps you to make HTTP Requests to all your internally deployed services which cannot be reached out from local system.

## Details

Let's say you have following infra
- K8s Cluster which has many services deployed internally
- Services Deployed on Ec2 Machines which cannot be acccessed from outside

So how can you access all of these services from one place with much ease. 
You would have to forward the ports for these services locally everytime you want to access those services. 
This is a huge PITA because everytime, i need to run the same set of commands for port-forwarding 
and if there are any other security solutions in between then that will just make the problem worse.

With proxyview , you can access your internal services deployed within K8s cluster or EC2 machine which cannot be accessed from outside in a simple, reliable and easy way.
Proxyview deploys clients on each of the internal clusters which opens a TCP connection from within the cluster to an external service by which you can access the internal services.

More details can be found out in the [Technical Details](#technical-details)

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
  Eg `TAG=1.2 ./run.sh`
- Run this curl command `curl -v -H 'Host: localhost-sample2:80' -H 'auth-token: xasxxx-yyyy-xxxx-yyyy' localhost:8080`

## Technical Details

Here is a basic architecture explaining the internals of Proxyview

<img src="https://i.ibb.co/YTSxF40/proxyview.png" alt="Overview" style="float: left; margin-right: 10px;" width="750" height="700"/> 
