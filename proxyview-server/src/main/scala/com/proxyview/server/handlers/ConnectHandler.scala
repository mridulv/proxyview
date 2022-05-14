package com.proxyview.server.handlers

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.ws.{ Message, TextMessage, UpgradeToWebSocket }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.proxyview.common.models.{ ClientConf, CommonModels }
import com.proxyview.server.model.ServerConfig
import com.proxyview.server.PacketHandler

import scala.collection.mutable
import scala.util.Try

class ConnectHandler(override val serverConfig: ServerConfig,
                     override val clientsInfo: mutable.Map[String, ClientConf],
                     packetHandler: ActorRef)
                    (implicit val actorSystem: ActorSystem)
  extends Handler {

  protected def handleAuthenticated(clientId: String, req: HttpRequest): HttpResponse = {
    req.header[UpgradeToWebSocket].get.handleMessages(registerNewClient(clientId))
  }

  private def registerNewClient(clientId: String): Flow[Message, Message, NotUsed] = {
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(msg) =>
          val clientResponseOpt = Try(CommonModels.deserClientResponse(msg)).toOption
          clientResponseOpt match {
            case Some(clientResponse) =>
              logging.info(s"Receiving response for ${clientResponse.clientId} from $clientId")
              clientResponse
            case None => logging.error(s"Something else")
          }
        case msg =>
          logging.error(s"Receiving response in invalid format : " + msg.toString)
      }.to(
        Sink.actorRef(
          packetHandler,
          onCompleteMessage = PacketHandler.DeregisterClient(clientId)))

    val outgoingMessages: Source[Message, NotUsed] =
      Source
        .actorRef[CommonModels.ConnectionRequest](10, OverflowStrategy.fail)
        .mapMaterializedValue { outgoingActor =>
          logging.info(s"Registering Actor for Sending requests for $clientId")
          packetHandler ! PacketHandler.RegisterClientInformation(clientId, clientsInfo(clientId), outgoingActor)
          NotUsed
        }
        .map {
          request => TextMessage.Strict(CommonModels.serConnectionRequest(request))
        }
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
