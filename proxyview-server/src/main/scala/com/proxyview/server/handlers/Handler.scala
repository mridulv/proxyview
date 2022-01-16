package com.proxyview.server.handlers

import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import com.proxyview.common.models.AgentConf
import com.proxyview.common.models.AgentConf.{AuthToken, AgentIdHeaderKey}
import com.proxyview.server.model.ServerConfig

import scala.collection.mutable

trait Handler {

  val serverConfig: ServerConfig
  val agentsInfo: mutable.Map[String, AgentConf]

  def handle(req: HttpRequest): HttpResponse = {
    if (authenticateAgent(req.headers)) {
      val agentID = req
        .headers
        .find(_.name() == AgentIdHeaderKey)
        .map(_.name())
        .getOrElse("random")
      handleAuthenticated(agentID, req)
    } else {
      HttpResponse(200, entity = "UnAuthenticated")
    }
  }

  protected def handleAuthenticated(agentId: String, req: HttpRequest): HttpResponse

  private def authenticateAgent(headers: Seq[HttpHeader]): Boolean = {
    val authenticated = headers.exists { header =>
      if (header.name() == AuthToken) {
        header.value() == serverConfig.token
      } else {
        false
      }
    }

    authenticated && headers.map(_.name()).contains(AgentIdHeaderKey)
  }

}
