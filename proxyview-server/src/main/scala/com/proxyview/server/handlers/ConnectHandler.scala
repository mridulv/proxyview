package com.proxyview.server.handlers

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.ws.{ Message, TextMessage, UpgradeToWebSocket }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.proxyview.common.models.{ AgentConf, CommonModels }
import com.proxyview.server.model.ServerConfig
import com.proxyview.server.PacketHandler

import scala.collection.mutable
import scala.util.Try

class ConnectHandler(
  override val serverConfig: ServerConfig,
  override val agentsInfo: mutable.Map[String, AgentConf],
  packetHandler: ActorRef)(implicit val actorSystem: ActorSystem)
  extends Handler {

  protected def handleAuthenticated(agentId: String, req: HttpRequest): HttpResponse = {
    req.header[UpgradeToWebSocket].get.handleMessages(registerNewAgent(agentId))
  }

  private def registerNewAgent(agentId: String): Flow[Message, Message, NotUsed] = {
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(msg) =>
          val agentResponseOpt = Try(CommonModels.deserA(msg)).toOption
          agentResponseOpt match {
            case Some(agentResponse) =>
              logging.info(s"Receiving response for ${agentResponse.clientId} from $agentId")
              agentResponse
            case None => logging.error(s"Something else")
          }
        case msg =>
          logging.error(s"Receiving response in invalid format : " + msg.toString)
      }.to(
        Sink.actorRef(
          packetHandler,
          onCompleteMessage = PacketHandler.DeregisterAgent(agentId)))

    val outgoingMessages: Source[Message, NotUsed] =
      Source
        .actorRef[CommonModels.ClientRequest](10, OverflowStrategy.fail)
        .mapMaterializedValue { outgoingActor =>
          logging.info(s"Registering Actor for Sending requests for $agentId")
          packetHandler ! PacketHandler.RegisterAgentInformation(agentId, agentsInfo(agentId), outgoingActor)
          NotUsed
        }
        .map {
          request => TextMessage.Strict(CommonModels.ser(request))
        }
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
