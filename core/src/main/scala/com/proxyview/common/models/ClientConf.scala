package com.proxyview.common.models

import ai.x.play.json.Jsonx
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import com.proxyview.common.models.ClientConf.{ AuthToken, ClientIdHeaderKey }
import net.jcazevedo.moultingyaml.DefaultYamlProtocol
import play.api.libs.json.{ Format, Json }
import rawhttp.core.{ RawHttpRequest, RequestLine }

import java.net.URI
import scala.collection.immutable

case class ClientConf(clientId: String, routes: Seq[ClientRoute], proxyview: ProxyviewServerConfig) {

  def clientHeaders(): immutable.Seq[HttpHeader] = {
    immutable.Seq(
      RawHeader(ClientIdHeaderKey, clientId),
      RawHeader(AuthToken, proxyview.token))
  }

  def validateRoute(uri: URI): Boolean = {
    routes.exists(route => uri.getHost == route.name)
  }

  def findRoute(uri: URI): Option[ClientRoute] = {
    routes.find(route => uri.getHost == route.name)
  }

  def constructValidHttpRequest(rawHttpRequest: RawHttpRequest): Option[RawHttpRequest] = {
    findRoute(rawHttpRequest.getUri) match {
      case Some(clientRoute) =>
        val requestLine = rawHttpRequest.getStartLine
        val uri = requestLine.getUri
        val updatedUri = new URI(uri.getScheme, uri.getUserInfo, clientRoute.value, clientRoute.port, uri.getPath, uri.getQuery, uri.getFragment)
        val updatedRequestLine = new RequestLine(requestLine.getMethod, updatedUri, requestLine.getHttpVersion)
        Some(rawHttpRequest.withRequestLine(updatedRequestLine))
      case None => None
    }
  }

}

object ClientConf extends DefaultYamlProtocol {
  val ClientIdHeaderKey = "client-id"
  val AuthToken = "auth-token"
  val HostHeader = "Host"

  def ser(clientConf: ClientConf): String = {
    Json.prettyPrint(Json.toJson(clientConf))
  }

  implicit val ClientConfFormatter = yamlFormat3(ClientConf.apply)
  implicit val ClientConfJsonFormatter: Format[ClientConf] = Jsonx.formatCaseClassUseDefaults[ClientConf]
}

