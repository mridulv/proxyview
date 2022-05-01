package com.proxyview.common.models

import ai.x.play.json.Jsonx
import net.jcazevedo.moultingyaml.DefaultYamlProtocol
import play.api.libs.json.Format

case class AgentRoute(name: String, value: String, port: Int, `type`: String)

object AgentRoute extends DefaultYamlProtocol {
  val CNAME = "cname"
  val IP = "ip"
  val REGEX = "regex"

  implicit val AgentRouteFormatter = yamlFormat4(AgentRoute.apply)
  implicit val AgentRouteFormatterJson: Format[AgentRoute] = Jsonx.formatCaseClassUseDefaults[AgentRoute]
}
