package com.proxyview.client

import akka.NotUsed
import akka.actor.{ActorRef, PoisonPill}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.actor.ActorSystem
import akka.Done
import akka.http.scaladsl.Http
import com.proxyview.common.models.{AgentConf, CommonModels}

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ProxyViewAgentConnectionHandler(agentConf: AgentConf,
                                      agentHttpClient: ActorRef)
                                     (implicit val actorSystem: ActorSystem,
                                      implicit val actorMaterializer: ActorMaterializer) {

  implicit val ec = scala.concurrent.ExecutionContext.global

  def register(): Unit = {
    println("Sending HTTP Request to Register")
    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = agentConf.proxyview.connectUri,
      headers = agentConf.agentHeaders(),
      entity = HttpEntity(ContentTypes.`application/json`, AgentConf.ser(agentConf))
    )
    Await.result(Http().singleRequest(httpRequest).flatMap(_.entity.toStrict(10.seconds)), 10.seconds)
  }

  def start(): Unit = {
    val incoming: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(msg) => agentHttpClient ! CommonModels.deserC(msg)
      }.to(Sink.actorRef(agentHttpClient, PoisonPill))

    val outgoing: Source[Message, NotUsed] = Source
      .actorRef[CommonModels.AgentResponse](10, OverflowStrategy.fail)
      .mapMaterializedValue { outgoingActor =>
        agentHttpClient ! CommonModels.WebsocketConnected(outgoingActor)
        NotUsed
      }.map { request => TextMessage.Strict(CommonModels.ser(request)) }

    val flow: Flow[Message, Message, NotUsed] = Flow.fromSinkAndSource(incoming, outgoing)

    val (upgradeResponse, _) =
      Http().singleWebSocketRequest(
        WebSocketRequest(
          s"ws://${agentConf.proxyview.url}/connect",
          agentConf.agentHeaders()
        ), flow
      )

    val connected = upgradeResponse.flatMap { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Future.successful(Done)
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }

    connected.onComplete(println)
  }
}
