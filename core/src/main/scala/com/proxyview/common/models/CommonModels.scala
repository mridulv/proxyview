package com.proxyview.common.models

import ai.x.play.json.Jsonx
import akka.actor.ActorRef
import play.api.libs.json.{ Format, Json }

object CommonModels {

  case class WebsocketConnected(actorRef: ActorRef)

  implicit val ConnectionRequestFormatter: Format[ConnectionRequest] = Jsonx.formatCaseClassUseDefaults[ConnectionRequest]
  implicit val ClientResponseFormatter: Format[ClientResponse] = Jsonx.formatCaseClassUseDefaults[ClientResponse]

  case class ConnectionRequest(connectionId: String, domain: String, request: String)

  case class ClientResponse(clientId: String, requestId: String, response: String)

  def serConnectionRequest(connectionRequest: ConnectionRequest): String = {
    Json.prettyPrint(Json.toJson(connectionRequest))
  }

  def deserConnectionRequest(connectionRequest: String): ConnectionRequest = {
    Json.toJson(Json.parse(connectionRequest)).asOpt[ConnectionRequest] match {
      case Some(clientRequest) => clientRequest
      case None => throw new RuntimeException("Parsing failed")
    }
  }

  def serClientResponse(clientResponse: ClientResponse): String = {
    Json.prettyPrint(Json.toJson(clientResponse))
  }

  def deserClientResponse(clientResponse: String): ClientResponse = {
    Json.toJson(Json.parse(clientResponse)).asOpt[ClientResponse] match {
      case Some(clientResponse) => clientResponse
      case None => throw new RuntimeException("Parsing failed")
    }
  }

}
