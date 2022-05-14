package com.proxyview.server

import akka.actor.{ Actor, ActorRef, Props }
import akka.util.ByteString
import PacketHandler._
import akka.event.Logging
import com.proxyview.common.models.ClientConf
import com.proxyview.common.models.CommonModels.{ ClientResponse, ConnectionRequest }
import com.proxyview.server.TcpConnectionHandler.InvalidRequest

import scala.collection.mutable
import com.proxyview.common.models.Logging._
import rawhttp.core.RawHttp

import java.net.URI

class PacketHandler extends Actor {

  private val rawHttp = new RawHttp
  private val clients: mutable.Map[ClientConf, ActorRef] = mutable.Map[ClientConf, ActorRef]()
  private val tcpConnections: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()

  protected val logging = Logging(context.system, this)

  override def receive: Receive = {
    case clientInformation: RegisterClientInformation =>
      logging.info(s"Registering Client with id: ${clientInformation.clientId}")
      clients.put(clientInformation.clientInfo, clientInformation.actorRef)
    case DeregisterClient(clientId) =>
      logging.info(s"Removing Client with id: $clientId")
      findKey(clientId).map(c => clients.remove(c))
    case DeregisterConnection(connectionId) =>
      logging.info(s"Removing connection with id: $connectionId")
      tcpConnections.remove(connectionId)
    case request: ConnectionRequest =>
      logging.info(s"Receiving request from: ${request.connectionId} for ${request.domain}")
      val rawHttpRequest = rawHttp.parseRequest(request.request)
      tcpConnections.put(request.connectionId, sender())
      findActor(rawHttpRequest.getUri) match {
        case Some(actorRef) => actorRef ! request
        case None => sender() ! InvalidRequest(request.domain)
      }
    case response: ClientResponse =>
      logging.info(s"Received response from: ${response.clientId} for ${response.requestId}")
      tcpConnections(response.requestId) ! TcpConnectionHandler.ResponseData(ByteString.apply(response.response))
  }

  private def findKey(clientId: String): Option[ClientConf] = {
    clients.find(_._1.clientId == clientId).map(_._1)
  }

  private def findActor(uri: URI): Option[ActorRef] = {
    clients.keys.find(_.validateRoute(uri)).map(clients(_))
  }
}

object PacketHandler {

  case class RegisterClientInformation(clientId: String, clientInfo: ClientConf, actorRef: ActorRef)
  case class RegisterConnectionInformation(clientId: String, domain: String)

  case class DeregisterConnection(connectionId: String)
  case class DeregisterClient(clientId: String)
  case class FailureMessage(clientId: String)

  def props() = Props(classOf[PacketHandler])
}
