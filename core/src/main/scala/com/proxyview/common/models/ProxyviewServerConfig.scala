package com.proxyview.common.models

import ai.x.play.json.Jsonx
import net.jcazevedo.moultingyaml.DefaultYamlProtocol
import play.api.libs.json.Format

case class ProxyviewServerConfig(url: String, token: String) {

  def connectUri: String = {
    if (sys.env.get("ENV").contains("production")) {
      "https://" + url + "/register"
    } else {
      "http://" + url + "/register"
    }
  }

}

object ProxyviewServerConfig extends DefaultYamlProtocol {
  implicit val ProxyviewServerConfigFormatter = yamlFormat2(ProxyviewServerConfig.apply)
  implicit val ProxyviewServerConfigFormatterJson: Format[ProxyviewServerConfig] = Jsonx.formatCaseClassUseDefaults[ProxyviewServerConfig]
}
