package com.proxyview.server.model

import net.jcazevedo.moultingyaml.DefaultYamlProtocol

case class ProxyviewEndpointConfig(token: String, port: Int, whitelistedIps: Option[Seq[String]])

object ProxyviewEndpointConfig extends DefaultYamlProtocol {
  implicit val ProxyviewEndpointConfigFormatter = yamlFormat3(ProxyviewEndpointConfig.apply)
}
