package com.proxyview.client

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import com.proxyview.common.models.CommonModels.WebsocketConnected
import com.proxyview.common.models.{ ClientConf, CommonModels, HttpResponses }
import rawhttp.core.{ RawHttp, RawHttpRequest }
import rawhttp.core.client.TcpRawHttpClient

class ProxyHttpClient(clientConf: ClientConf) extends Actor {

  private val logging = Logging(context.system, this)
  private val rawHttp = new RawHttp

  def receive = {
    case WebsocketConnected(actorRef) =>
      context.become(listening(actorRef))
  }

  def listening(ref: ActorRef): Receive = {
    case req: CommonModels.ConnectionRequest =>
      val request = rawHttp.parseRequest(req.request)
      logging.info(s"Received HTTP Request for ${request.getUri} from ${req.connectionId}.")
      clientConf.constructValidHttpRequest(request) match {
        case Some(updatedRawHttpRequest) =>
          logging.info(s"Sending response for ${updatedRawHttpRequest.getUri} from ${req.connectionId} $updatedRawHttpRequest")
          val responseOpt = try {
            Some(makeHttpRequest(updatedRawHttpRequest))
          } catch {
            case e: Exception =>
              logging.error(s"Error while response is: ${e.getMessage}")
              None
          }
          responseOpt match {
            case Some(response) => ref ! CommonModels.ClientResponse(clientConf.clientId, req.connectionId, response)
            case None => sendErrorResponse(ref, req, request)
          }
        case None => sendErrorResponse(ref, req, request)
      }
  }

  private def sendErrorResponse(ref: ActorRef, req: CommonModels.ConnectionRequest, request: RawHttpRequest): Unit = {
    logging.error(s"Sending Error Response for Request ${request.getUri} from ${req.connectionId}")
    ref ! CommonModels.ClientResponse(clientConf.clientId, req.connectionId, HttpResponses.errorResponse.toString)
  }

  private def makeHttpRequest(rawHttpRequest: RawHttpRequest): String = {
    val client = new TcpRawHttpClient
    val response = client.send(rawHttpRequest).eagerly()
    response.toString
  }

}

object ProxyHttpClient {
  def props(agentConf: ClientConf) = Props(new ProxyHttpClient(agentConf))
}