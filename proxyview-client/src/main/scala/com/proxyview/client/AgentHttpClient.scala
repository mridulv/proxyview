package com.proxyview.client

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import com.proxyview.common.models.CommonModels.WebsocketConnected
import com.proxyview.common.models.{ AgentConf, CommonModels }
import rawhttp.core.{ RawHttp, RawHttpRequest }
import rawhttp.core.client.TcpRawHttpClient

import scala.util.Try

class AgentHttpClient(agentConf: AgentConf) extends Actor {

  private val logging = Logging(context.system, this)
  private val rawHttp = new RawHttp

  def receive = {
    case WebsocketConnected(actorRef) =>
      context.become(listening(actorRef))
  }

  def listening(ref: ActorRef): Receive = {
    case req: CommonModels.ClientRequest =>
      val request = rawHttp.parseRequest(req.request)
      logging.info(s"Received HTTP Request for ${request.getUri} from ${req.clientId}")
      if (agentConf.validateRoute(request.getUri.getHost + ":" + request.getUri.getPort)) {
        logging.info(s"Sending response for ${request.getUri} from ${req.clientId} $request")
        val responseOpt = Try(makeHttpRequest(request)).toOption
        responseOpt match {
          case Some(response) => ref ! CommonModels.AgentResponse(agentConf.agentId, req.clientId, response)
          case None => sendErrorResponse(ref, req, request)
        }
      } else {
        sendErrorResponse(ref, req, request)
      }
  }

  private def sendErrorResponse(
    ref: ActorRef,
    req: CommonModels.ClientRequest,
    request: RawHttpRequest): Unit = {
    logging.error(s"Validation failed for request ${request.getUri} from ${req.clientId}")
    ref ! CommonModels.AgentResponse(agentConf.agentId, req.clientId, "HTTP/1.1 404 Not Found")
  }

  private def makeHttpRequest(rawHttpRequest: RawHttpRequest): String = {
    val client = new TcpRawHttpClient
    val response = client.send(rawHttpRequest).eagerly()
    response.toString
  }

}

object AgentHttpClient {
  def props(agentConf: AgentConf) = Props(new AgentHttpClient(agentConf))
}