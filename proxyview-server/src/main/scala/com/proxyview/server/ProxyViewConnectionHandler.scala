package com.proxyview.server

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{ GET, POST }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import com.proxyview.common.models.AgentConf
import com.proxyview.server.handlers.{ ConnectHandler, RegisterHandler }
import com.proxyview.server.model.ServerConfig

import scala.collection.mutable
import scala.concurrent.Future
import com.proxyview.common.models.Logging._

class ProxyViewConnectionHandler(
  serverConfig: ServerConfig,
  packetHandler: ActorRef)(implicit
  val actorSystem: ActorSystem,
  implicit val actorMaterializer: ActorMaterializer) {

  import serverConfig._

  private val logger = Logging(actorSystem, this)
  private val agentsInfo: mutable.Map[String, AgentConf] = mutable.Map[String, AgentConf]()
  private val connectHandler = new ConnectHandler(serverConfig, agentsInfo, packetHandler)
  private val registerHandler = new RegisterHandler(serverConfig, agentsInfo)

  private val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] = Http().bind(
    interface = host,
    port = port)

  def start(): Future[Http.ServerBinding] = {
    logger.info(s"Starting server with ${serverConfig.host}:${serverConfig.port}")
    serverSource
      .to(Sink.foreach { connection =>
        println("Accepted new connection from " + connection.remoteAddress)
        connection.handleWithSyncHandler(requestHandler)
      }).run()
  }

  // this is a future but parallelism is 1 so we have the guarantee of sequential execution
  def requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(GET, Uri.Path("/connect"), _, _, _) =>
      logger.info(s"Received Connect request from agent")
      connectHandler.handle(req)
    case req @ HttpRequest(POST, Uri.Path("/register"), _, _, _) =>
      logger.info(s"Received Register request from agent")
      registerHandler.handle(req)
    case r: HttpRequest =>
      logger.error(s"Invalid request: ${r.method} on route: ${r.uri}")
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }
}
