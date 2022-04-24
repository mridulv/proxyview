package com.proxyview.common.models

import ai.x.play.json.Jsonx
import akka.actor.ActorRef
import play.api.libs.json.{ Format, Json }

object CommonModels {

  case class WebsocketConnected(actorRef: ActorRef)

  implicit val ClientRequestFormatter: Format[ClientRequest] = Jsonx.formatCaseClassUseDefaults[ClientRequest]
  implicit val AgentResponseFormatter: Format[AgentResponse] = Jsonx.formatCaseClassUseDefaults[AgentResponse]

  case class ClientRequest(clientId: String, domain: String, request: String)

  case class AgentResponse(agentId: String, clientId: String, response: String)

  def serClientRequest(clientRequest: ClientRequest): String = {
    Json.prettyPrint(Json.toJson(clientRequest))
  }

  def deserClientRequest(clientRequest: String): ClientRequest = {
    Json.toJson(Json.parse(clientRequest)).asOpt[ClientRequest] match {
      case Some(clientRequest) => clientRequest
      case None => throw new RuntimeException("Parsing failed")
    }
  }

  def serAgentResponse(agentResponse: AgentResponse): String = {
    Json.prettyPrint(Json.toJson(agentResponse))
  }

  def deserAgentResponse(agentResponse: String): AgentResponse = {
    Json.toJson(Json.parse(agentResponse)).asOpt[AgentResponse] match {
      case Some(agentResponse) => agentResponse
      case None => throw new RuntimeException("Parsing failed")
    }
  }

}
