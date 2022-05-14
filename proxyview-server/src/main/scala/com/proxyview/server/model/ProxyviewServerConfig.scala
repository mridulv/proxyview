package com.proxyview.server.model

import net.jcazevedo.moultingyaml.DefaultYamlProtocol

case class ProxyviewServerConfig(
  host: String,
  internal: ProxyviewEndpointConfig,
  external: ProxyviewEndpointConfig) {

  def externalServerConfig(): ServerConfig = {
    ServerConfig(host, external.port, external.token, external.whitelistedIps)
  }

  def internalServerConfig(): ServerConfig = {
    ServerConfig(host, internal.port, internal.token, internal.whitelistedIps)
  }

}

object ProxyviewServerConfig extends DefaultYamlProtocol {
  implicit val ProxyviewServerConfigFormatter = yamlFormat3(ProxyviewServerConfig.apply)
}
