package com.proxyview.server.handlers

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.{ HttpHeader, HttpRequest, HttpResponse }
import com.proxyview.common.models.ClientConf
import com.proxyview.common.models.ClientConf.{ ClientIdHeaderKey, AuthToken }
import com.proxyview.server.model.ServerConfig
import com.proxyview.common.models.Logging._

import scala.collection.mutable

trait Handler {

  implicit val actorSystem: ActorSystem

  protected val logging = Logging(actorSystem, this)

  val serverConfig: ServerConfig
  val clientsInfo: mutable.Map[String, ClientConf]

  def handle(req: HttpRequest): HttpResponse = {
    if (authenticateClient(req.headers)) {
      val clientId = req
        .headers
        .find(_.name() == ClientIdHeaderKey)
        .map(_.value())
        .getOrElse("random")
      handleAuthenticated(clientId, req)
    } else {
      HttpResponse(200, entity = "UnAuthenticated")
    }
  }

  protected def handleAuthenticated(clientId: String, req: HttpRequest): HttpResponse

  private def authenticateClient(headers: Seq[HttpHeader]): Boolean = {
    val authenticated = headers.exists { header =>
      if (header.name() == AuthToken) {
        header.value() == serverConfig.token
      } else {
        false
      }
    }

    authenticated && headers.map(_.name()).contains(ClientIdHeaderKey)
  }

}
