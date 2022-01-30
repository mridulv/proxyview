package com.proxyview.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.proxyview.server.model.ProxyviewServerConfig
import org.apache.commons.io.FileUtils
import net.jcazevedo.moultingyaml._

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

object Main {
  private val ACTOR_SYSTEM = "proxyview-server"
  private val APPLICATION_CONFIG = "application.yaml"
  private val CONFIG_DIR = "LHUB_CONFIG_DIR"

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem(ACTOR_SYSTEM)
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val configDirOpt = Option(System.getenv(CONFIG_DIR)).map(location => Paths.get(location))
    val fileContent = configDirOpt.map { configDir =>
      FileUtils.readFileToString(configDir.resolve(APPLICATION_CONFIG).toFile, StandardCharsets.UTF_8)
    }.getOrElse {
      val is = getClass.getResourceAsStream(APPLICATION_CONFIG)
      scala.io.Source.fromInputStream(is).mkString
    }
    val proxyviewServerConfig = fileContent.parseYaml.convertTo[ProxyviewServerConfig]

    val packetHandler = system.actorOf(PacketHandler.props())
    system.actorOf(TcpProxy.props(proxyviewServerConfig.externalServerConfig(), packetHandler))
    val connectionHandler = new ProxyViewConnectionHandler(proxyviewServerConfig.internalServerConfig(), packetHandler)
    connectionHandler.start()
  }

}
