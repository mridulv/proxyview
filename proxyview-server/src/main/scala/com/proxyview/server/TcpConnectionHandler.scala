package com.proxyview.server

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import akka.io.Tcp
import akka.util.ByteString
import TcpConnectionHandler._
import com.proxyview.common.models.{ AgentConf, HttpResponses }
import com.proxyview.common.models.CommonModels.ClientRequest
import rawhttp.core.{ RawHttp, RawHttpRequest }

import java.util.UUID
import scala.util.Try

object TcpConnectionHandler {
  case class RequestData(bytes: ByteString)
  case class ResponseData(bytes: ByteString)
  case class InvalidRequest(domain: String)

  def props(authToken: String, connection: ActorRef, packetHandler: ActorRef) =
    Props(classOf[TcpConnectionHandler], authToken, connection, packetHandler)

}

class TcpConnectionHandler(authToken: String, connection: ActorRef, packetHandler: ActorRef)
  extends Actor {

  private val logger = Logging(context.system, this)
  private val rawHttp = new RawHttp()
  private val uuid = UUID.randomUUID().toString

  import akka.pattern.pipe
  import scala.concurrent.ExecutionContext.Implicits.global
  context watch connection
  import Tcp._

  override def preStart: Unit = {
    logger.info(s"Connection started")
    connection ! ResumeReading
  }

  override def postStop(): Unit = {
    connection ! Close
  }

  case class Ack(sender: ActorRef) extends Event

  def receive = {
    case Received(data) =>
      logger.info(s"Received data (${data.size} bytes) for $uuid from the client.")
      val rawHttpRequest = rawHttp.parseRequest(data.utf8String)
      handleRequest(rawHttpRequest, data.utf8String)

    case PeerClosed =>
      logger.info(s"peer closed $uuid from client side.")
      connection ! ResumeReading
      packetHandler ! PacketHandler.DeregisterClient(uuid)
      context.stop(self)

    case InvalidRequest(_) =>
      logger.info(s"peer closed $uuid due to invalid request.")
      connection ! ResumeReading
      packetHandler ! PacketHandler.DeregisterClient(uuid)
      connection ! Write(ByteString(HttpResponses.errorResponse.toString), Ack(sender()))

    case ResponseData(bytes) =>
      logger.info(s"received data for id $uuid.")
      connection ! Write(bytes, Ack(sender()))

    case Ack(sender) =>
      logger.info(s"acknowledgement for $uuid.")
      context.stop(self)
  }

  private def handleRequest(rawHttpRequest: RawHttpRequest, data: String): Unit = {
    logger.info(s"validating the token for client: $uuid")
    val tokenOpt = Try(rawHttpRequest.getHeaders.get(AgentConf.AuthToken).get(0)).toOption
    tokenOpt match {
      case Some(token) if token == authToken =>
        logger.info(s"Validation Passed for token from client: $uuid")
        val hostHeader = rawHttpRequest.getHeaders.get(AgentConf.HostHeader).get(0)
        packetHandler ! ClientRequest(uuid, hostHeader, data)
      case _ =>
        logger.error(s"Validation Failed for token from client: $uuid")
        context.stop(self)
    }
  }
}
