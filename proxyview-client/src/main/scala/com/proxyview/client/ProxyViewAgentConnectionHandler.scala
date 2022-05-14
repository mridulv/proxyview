package com.proxyview.client

import akka.NotUsed
import akka.actor.{ ActorRef, PoisonPill }
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.model.ws.{ Message, TextMessage, WebSocketRequest }
import akka.stream.{ ActorMaterializer, OverflowStrategy }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.actor.ActorSystem
import akka.Done
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.util.ByteString
import com.proxyview.common.models.{ ClientConf, CommonModels }
import com.proxyview.common.models.Logging._

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ProxyViewAgentConnectionHandler(agentConf: ClientConf, agentHttpClient: ActorRef)(implicit
  val actorSystem: ActorSystem,
  implicit val actorMaterializer: ActorMaterializer) {

  private val logging = Logging(actorSystem, this)
  implicit val ec = scala.concurrent.ExecutionContext.global

  def register(): Unit = {
    val retryConfig = RetryConfig()
    logging.info(s"Sending HTTP Request to ${agentConf.proxyview.connectUri} Register")
    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = agentConf.proxyview.connectUri,
      headers = agentConf.clientHeaders(),
      entity = HttpEntity(ContentTypes.`application/json`, ClientConf.ser(agentConf)))
    Await.result(
      RetryableHttpClient.apply(retryConfig).request(httpRequest).flatMap(_.entity.toStrict(retryConfig.overallDelay)),
      retryConfig.overallDelay)

    logging.info(s"Registered agent with id: ${agentConf.clientId} with ${agentConf.proxyview.connectUri}")
  }

  def start(): Unit = {
    val incoming: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(msg) =>
          val request = CommonModels.deserConnectionRequest(msg)
          logging.info(s"Received request with id: ${request.connectionId} for ${request.domain}")
          agentHttpClient ! request
      }.to(Sink.actorRef(agentHttpClient, PoisonPill))

    val outgoing: Source[Message, NotUsed] = Source
      .actorRef[CommonModels.ClientResponse](10, OverflowStrategy.fail)
      .mapMaterializedValue { outgoingActor =>
        logging.info(s"Registering actor for sending response agent to server")
        agentHttpClient ! CommonModels.WebsocketConnected(outgoingActor)
        NotUsed
      }.map { request =>
        logging.info(s"Sending response back for request with id: ${request.clientId} from ${request.clientId}")
        TextMessage.Strict(CommonModels.serClientResponse(request))
      }

    val flow: Flow[Message, Message, NotUsed] = Flow.fromSinkAndSource(incoming, outgoing)

    val defaultSettings = ClientConnectionSettings(actorSystem)

    val pingCounter = new AtomicInteger()
    val customWebsocketSettings =
      defaultSettings.websocketSettings
        .withPeriodicKeepAliveData(() => ByteString(s"debug-${pingCounter.incrementAndGet()}"))
        .withPeriodicKeepAliveMaxIdle(5.seconds)

    val customSettings =
      defaultSettings.withWebsocketSettings(customWebsocketSettings)

    val (upgradeResponse, _) =
      Http().singleWebSocketRequest(
        WebSocketRequest(s"ws://${agentConf.proxyview.url}/connect", agentConf.clientHeaders()),
        flow,
        settings = customSettings)

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
