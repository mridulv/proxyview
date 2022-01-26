package com.proxyview.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.proxyview.server.model.ProxyviewServerConfig
import org.apache.commons.io.FileUtils
import net.jcazevedo.moultingyaml._

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

object Main {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("proxyview-server")
    implicit val materializer = ActorMaterializer()

    val defaultConfigDir = "/opt/docker/conf"
    val configDir = Paths.get(Option(System.getenv("LHUB_CONFIG_DIR")).getOrElse(defaultConfigDir))
    val fileContent = FileUtils.readFileToString(configDir.resolve("application.yaml").toFile, StandardCharsets.UTF_8)
    val proxyviewServerConfig = fileContent.parseYaml.convertTo[ProxyviewServerConfig]

    val packetHandler = system.actorOf(PacketHandler.props())
    system.actorOf(TcpProxy.props(proxyviewServerConfig.externalServerConfig(), packetHandler))
    val connectionHandler = new ProxyViewConnectionHandler(proxyviewServerConfig.internalServerConfig(), packetHandler)
    connectionHandler.start()
  }

}
