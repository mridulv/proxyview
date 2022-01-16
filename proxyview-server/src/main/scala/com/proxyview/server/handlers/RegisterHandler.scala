package com.proxyview.server.handlers

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.proxyview.common.models.AgentConf
import com.proxyview.server.model.ServerConfig
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class RegisterHandler(override val serverConfig: ServerConfig,
                      override val agentsInfo: mutable.Map[String, AgentConf])
                     (implicit val materializer: Materializer)
  extends Handler {

  protected def handleAuthenticated(agentId: String, req: HttpRequest): HttpResponse = {
    println(s"Registering with Agent with $agentId")
    val postEntity = Await.result(Unmarshal(req.entity).to[String], 10.seconds)
    Json.toJson(Json.parse(postEntity)).asOpt[AgentConf] match {
      case Some(agentConf) =>
        agentsInfo.put(agentId, agentConf)
        HttpResponse(200, entity = "Registered")
      case None => HttpResponse(StatusCodes.BadRequest, entity = "Bad Request")
    }
  }

}
