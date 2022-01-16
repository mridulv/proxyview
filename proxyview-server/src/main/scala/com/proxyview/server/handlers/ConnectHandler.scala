package com.proxyview.server.handlers

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.proxyview.common.models.{AgentConf, CommonModels}
import com.proxyview.server.model.ServerConfig
import com.proxyview.server.PacketHandler

import scala.collection.mutable

class ConnectHandler(override val serverConfig: ServerConfig,
                     override val agentsInfo: mutable.Map[String, AgentConf],
                     packetHandler: ActorRef)
  extends Handler {

  protected def handleAuthenticated(agentId: String, req: HttpRequest): HttpResponse = {
    req.header[UpgradeToWebSocket].get.handleMessages(registerNewAgent(agentId))
  }

  private def registerNewAgent(agentId: String): Flow[Message, Message, NotUsed] = {
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(msg) => CommonModels.deserA(msg)
      }.to(Sink.actorRef(packetHandler, onCompleteMessage = PacketHandler.DeregisterAgent(agentId)))

    val outgoingMessages: Source[Message, NotUsed] =
      Source
        .actorRef[CommonModels.ClientRequest](10, OverflowStrategy.fail)
        .mapMaterializedValue { outgoingActor =>
          packetHandler ! PacketHandler.RegisterAgentInformation(agentId, agentsInfo(agentId), outgoingActor)
          NotUsed
        }
        .map {
          request => TextMessage.Strict(CommonModels.ser(request))
        }
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
