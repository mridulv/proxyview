package com.proxyview.common.models

import ai.x.play.json.Jsonx
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import com.proxyview.common.models.AgentConf.{ AgentIdHeaderKey, AuthToken }
import net.jcazevedo.moultingyaml.DefaultYamlProtocol
import play.api.libs.json.{ Format, Json }

import java.net.URI
import scala.collection.immutable
import scala.util.matching.Regex

case class AgentConf(agentId: String, routes: Seq[AgentRoute], proxyview: ProxyviewServerConfig) {

  def agentHeaders(): immutable.Seq[HttpHeader] = {
    immutable.Seq(
      RawHeader(AgentIdHeaderKey, agentId),
      RawHeader(AuthToken, proxyview.token))
  }

  def validateRoute(uri: URI): Boolean = {
    routes.exists { route =>
      route.`type` match {
        case AgentRoute.IP | AgentRoute.CNAME => uri.getHost == route.value
        case AgentRoute.REGEX => new Regex(route.value).pattern.matcher(uri.getHost).matches()
      }
    }
  }

}

object AgentConf extends DefaultYamlProtocol {
  val AgentIdHeaderKey = "agent-id"
  val AuthToken = "auth-token"
  val HostHeader = "Host"

  def ser(agentConf: AgentConf): String = {
    Json.prettyPrint(Json.toJson(agentConf))
  }

  implicit val AgentConfFormatter = yamlFormat3(AgentConf.apply)
  implicit val AgentConfJsonFormatter: Format[AgentConf] = Jsonx.formatCaseClassUseDefaults[AgentConf]
}

