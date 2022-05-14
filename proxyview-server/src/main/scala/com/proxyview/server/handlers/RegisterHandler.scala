package com.proxyview.server.handlers

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.proxyview.common.models.ClientConf
import com.proxyview.server.model.ServerConfig
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class RegisterHandler(
  override val serverConfig: ServerConfig,
  override val clientsInfo: mutable.Map[String, ClientConf])(implicit val materializer: Materializer, val actorSystem: ActorSystem)
  extends Handler {

  protected def handleAuthenticated(clientId: String, req: HttpRequest): HttpResponse = {
    logging.info(s"Registering with Client with $clientId")
    val postEntity = Await.result(Unmarshal(req.entity).to[String], 10.seconds)
    Json.toJson(Json.parse(postEntity)).asOpt[ClientConf] match {
      case Some(clientConf) =>
        clientsInfo.put(clientId, clientConf)
        val res = HttpResponse(200, entity = "Registered")
        logging.info(s"Registration Successful for Client with id: $clientId")
        res
      case None =>
        val res = HttpResponse(StatusCodes.BadRequest, entity = "Bad Request")
        logging.error(s"Registration Unsuccessful due to Bad Request from $clientId")
        res
    }
  }

}
