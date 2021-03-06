package com.proxyview.server

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import akka.io.{ IO, Tcp }
import com.proxyview.server.model.ServerConfig

import java.net.InetSocketAddress

class TcpProxy(serverConfig: ServerConfig, packetHandler: ActorRef) extends Actor {

  import Tcp._
  import context.system
  import serverConfig._

  private val logger = Logging(context.system, this)

  IO(Tcp) ! Bind(self, new InetSocketAddress(host, port), pullMode = true)

  logger.info(s"TCP handler Actor started on port $port")

  override def postStop(): Unit = {
    logger.info("Shutting down the TCP ")
  }

  def cleanUp(): Unit = {
    logger.warning(s"Cleaning up TcpListener for port $port.")
    context.stop(self)
  }

  def receive = {
    case b @ Bound(_) =>
      context.parent ! b
      sender() ! ResumeAccepting(batchSize = 1)
      logger.info("TCP Started and accepting")
      context.become(listening(sender()))

    case CommandFailed(_: Bind) => context.stop(self)

    case other =>
      logger.error(f"Unexpected Message to TCP Listener: $other")
  }

  def listening(listener: ActorRef): Receive = {
    case Connected(remoteAddress, _) =>
      val incomingIp = remoteAddress.getAddress.getHostAddress
      logger.info(s"TCP Connection on port $port with incoming connection from: $incomingIp")
      val connection = sender()
      whitelistedIps match {
        case Some(ips) => handleConnection(connection, !ips.contains(incomingIp), listener)
        case None => handleConnection(connection, false, listener)
      }

    case Unbound =>
      logger.info(s"TCP connection unbound for port $port. We have shut down.")
      context.stop(self)

    case other =>
      logger.warning(f"Unexpected Message to TCP Listener: $other")
  }

  private def handleConnection(connection: ActorRef, closeConnection: Boolean, listener: ActorRef): Unit = {
    val handler = context.actorOf(TcpConnectionHandler.props(token, closeConnection, connection, packetHandler))
    connection ! Register(handler)
    listener ! ResumeAccepting(batchSize = 1)
  }
}

object TcpProxy {
  def props(serverConfig: ServerConfig, packetHandler: ActorRef) =
    Props(classOf[TcpProxy], serverConfig, packetHandler)
}