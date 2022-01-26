package com.proxyview.server.handlers

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.proxyview.common.models.AgentConf
import com.proxyview.server.model.ServerConfig
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class RegisterHandler(
  override val serverConfig: ServerConfig,
  override val agentsInfo: mutable.Map[String, AgentConf])(implicit val materializer: Materializer, val actorSystem: ActorSystem)
  extends Handler {

  protected def handleAuthenticated(agentId: String, req: HttpRequest): HttpResponse = {
    logging.info(s"Registering with Agent with $agentId")
    val postEntity = Await.result(Unmarshal(req.entity).to[String], 10.seconds)
    Json.toJson(Json.parse(postEntity)).asOpt[AgentConf] match {
      case Some(agentConf) =>
        agentsInfo.put(agentId, agentConf)
        val res = HttpResponse(200, entity = "Registered")
        logging.info(s"Registration Successful for Agent with id: $agentId")
        res
      case None =>
        val res = HttpResponse(StatusCodes.BadRequest, entity = "Bad Request")
        logging.error(s"Registration Unsuccessful due to Bad Request from $agentId")
        res
    }
  }

}
