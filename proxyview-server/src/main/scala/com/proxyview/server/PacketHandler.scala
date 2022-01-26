package com.proxyview.server

import akka.actor.{ Actor, ActorRef, Props }
import akka.util.ByteString
import PacketHandler._
import akka.event.Logging
import com.proxyview.common.models.AgentConf
import com.proxyview.common.models.CommonModels.{ AgentResponse, ClientRequest }
import com.proxyview.server.TcpConnectionHandler.InvalidRequest

import scala.collection.mutable
import com.proxyview.common.models.Logging._

class PacketHandler extends Actor {

  protected val logging = Logging(context.system, this)

  private val agents: mutable.Map[AgentConf, ActorRef] = mutable.Map[AgentConf, ActorRef]()
  private val tcpConnections: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()

  override def receive: Receive = {
    case agentInfo: RegisterAgentInformation =>
      logging.info(s"Registering agent with id: ${agentInfo.agentId}")
      agents.put(agentInfo.agentInfo, agentInfo.actorRef)
    case DeregisterAgent(agentId) =>
      logging.info(s"Removing agent with id: $agentId")
      findKey(agentId).map(c => agents.remove(c))
    case DeregisterClient(clientId) =>
      logging.info(s"Removing client with id: $clientId")
      tcpConnections.remove(clientId)
    case request: ClientRequest =>
      logging.info(s"Receiving request from: ${request.clientId} for ${request.domain}")
      tcpConnections.put(request.clientId, sender())
      findActor(request.domain) match {
        case Some(actorRef) => actorRef ! request
        case None => sender() ! InvalidRequest(request.domain)
      }
    case response: AgentResponse =>
      logging.info(s"Received response from: ${response.agentId} for ${response.clientId}")
      tcpConnections(response.clientId) ! TcpConnectionHandler.ResponseData(ByteString.apply(response.response))
  }

  private def findKey(agentId: String): Option[AgentConf] = {
    agents.find(_._1.agentId == agentId).map(_._1)
  }

  private def findActor(domain: String): Option[ActorRef] = {
    agents.keys.find(_.validateRoute(domain)).map(agents(_))
  }
}

object PacketHandler {

  case class RegisterAgentInformation(agentId: String, agentInfo: AgentConf, actorRef: ActorRef)
  case class RegisterClientInformation(clientId: String, domain: String)

  case class DeregisterAgent(agentId: String)
  case class DeregisterClient(clientId: String)
  case class FailureMessage(agentId: String)

  def props() = Props(classOf[PacketHandler])
}
