package com.proxyview.server.model

import net.jcazevedo.moultingyaml.DefaultYamlProtocol

case class ProxyviewEndpointConfig(token: String, port: Int)

object ProxyviewEndpointConfig extends DefaultYamlProtocol {
  implicit val ProxyviewEndpointConfigFormatter = yamlFormat2(ProxyviewEndpointConfig.apply)
}
