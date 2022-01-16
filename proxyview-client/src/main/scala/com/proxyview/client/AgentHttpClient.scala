package com.proxyview.client

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.proxyview.common.models.CommonModels.WebsocketConnected
import com.proxyview.common.models.{AgentConf, CommonModels}
import rawhttp.core.{RawHttp, RawHttpRequest}
import rawhttp.core.client.TcpRawHttpClient


class AgentHttpClient(agentConf: AgentConf) extends Actor {

  private val client = new TcpRawHttpClient
  private val rawHttp = new RawHttp

  def receive = {
    case WebsocketConnected(actorRef) =>
      context.become(listening(actorRef))
  }

  def listening(ref: ActorRef): Receive = {
    case req: CommonModels.ClientRequest =>
      val request = rawHttp.parseRequest(req.request)
      val response = makeHttpRequest(request)
      if (agentConf.validateRoute(request.getUri.getHost + ":" + request.getUri.getPort)) {
        ref ! CommonModels.AgentResponse(agentConf.agentId, req.clientId, response)
      } else {
        ref ! CommonModels.AgentResponse(agentConf.agentId, req.clientId, "random")
      }
  }

  private def makeHttpRequest(rawHttpRequest: RawHttpRequest): String = {
    println("Receiving an HTTP Request")
    val response = client.send(rawHttpRequest).eagerly()
    response.toString
  }

}

object AgentHttpClient {
  def props(agentConf: AgentConf)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) = Props(new AgentHttpClient(agentConf))
}