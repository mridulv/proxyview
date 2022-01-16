package com.proxyview.server

import ai.x.play.json.Jsonx
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Sink, Source}
import com.proxyview.common.models.AgentConf
import com.proxyview.server.handlers.{ConnectHandler, RegisterHandler}
import com.proxyview.server.model.ServerConfig
import play.api.libs.json.Format

import scala.collection.mutable
import scala.concurrent.Future

class ProxyViewConnectionHandler(serverConfig: ServerConfig,
                                 packetHandler: ActorRef)
                                (implicit val actorSystem: ActorSystem,
                                 implicit val actorMaterializer: ActorMaterializer){

  import serverConfig._

  private val agentsInfo: mutable.Map[String, AgentConf] = mutable.Map[String, AgentConf]()
  private val connectHandler = new ConnectHandler(serverConfig, agentsInfo, packetHandler)
  private val registerHandler = new RegisterHandler(serverConfig, agentsInfo)

  private val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] = Http().bind(
    interface = host,
    port = port)

  def start(): Future[Http.ServerBinding] = {
    serverSource
      .to(Sink.foreach { connection =>
        println("Accepted new connection from " + connection.remoteAddress)
        connection.handleWithSyncHandler(requestHandler)
      }).run()
  }

  // this is a future but parallelism is 1 so we have the guarantee of sequential execution
  def requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(GET, Uri.Path("/connect"), _, _, _) =>
      connectHandler.handle(req)
    case req @ HttpRequest(POST, Uri.Path("/register"), _, _, _) =>
      registerHandler.handle(req)
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }
}
