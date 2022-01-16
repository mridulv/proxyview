package com.proxyview.server

import akka.actor.{Actor, ActorRef, Props}
import akka.util.ByteString
import PacketHandler._
import com.proxyview.common.models.AgentConf
import com.proxyview.common.models.CommonModels.{AgentResponse, ClientRequest}
import com.proxyview.server.TcpConnectionHandler.InvalidRequest

import scala.collection.mutable

class PacketHandler extends Actor {

  private val agents: mutable.Map[AgentConf, ActorRef] = mutable.Map[AgentConf, ActorRef]()
  private val tcpConnections: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()

  override def receive: Receive = {
    case agentInfo: RegisterAgentInformation =>
      agents.put(agentInfo.agentInfo, agentInfo.actorRef)
    case client: RegisterClientInformation =>
      tcpConnections.put(client.clientId, sender())
    case DeregisterAgent(agentId) => findKey(agentId).map(c => agents.remove(c))
    case DeregisterClient(clientId) => tcpConnections.remove(clientId)
    case request: ClientRequest => findActor(request.domain) match {
      case Some(actorRef) => actorRef ! request
      case None => tcpConnections(request.clientId) ! InvalidRequest(request.domain)
    }
    case response: AgentResponse =>
      tcpConnections(response.clientId) ! TcpConnectionHandler.ResponseData(ByteString.apply(response.response))
  }

  private def findKey(agentId: String): Option[AgentConf] = {
    agents.find(_._1.agentId == agentId).map(_._1)
  }

  private def findValue(agentId: String): Option[ActorRef] = {
    agents.find(_._1.agentId == agentId).map(_._2)
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

  def props() = Props(classOf[PacketHandler])
}
