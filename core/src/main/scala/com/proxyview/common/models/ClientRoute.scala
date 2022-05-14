package com.proxyview.common.models

import ai.x.play.json.Jsonx
import net.jcazevedo.moultingyaml.DefaultYamlProtocol
import play.api.libs.json.Format

case class ClientRoute(name: String, value: String, port: Int, `type`: String)

object ClientRoute extends DefaultYamlProtocol {
  val CNAME = "cname"
  val IP = "ip"

  implicit val ClientRouteFormatter = yamlFormat4(ClientRoute.apply)
  implicit val ClientRouteFormatterJson: Format[ClientRoute] = Jsonx.formatCaseClassUseDefaults[ClientRoute]
}
