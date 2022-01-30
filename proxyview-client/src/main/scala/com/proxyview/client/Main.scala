package com.proxyview.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.commons.io.FileUtils

import java.nio.charset.StandardCharsets
import net.jcazevedo.moultingyaml._
import com.proxyview.common.models.AgentConf

import java.nio.file.Paths

object Main {
  private val ACTOR_SYSTEM = "proxyview-client"
  private val APPLICATION_CONFIG = "application.yaml"
  private val CONFIG_DIR = "LHUB_CONFIG_DIR"

  def main(args: Array[String]): Unit = {
    val configDirOpt = Option(System.getenv(CONFIG_DIR)).map(location => Paths.get(location))
    val fileContent = configDirOpt.map { configDir =>
      FileUtils.readFileToString(configDir.resolve(APPLICATION_CONFIG).toFile, StandardCharsets.UTF_8)
    }.getOrElse {
      val is = getClass.getResourceAsStream(APPLICATION_CONFIG)
      scala.io.Source.fromInputStream(is).mkString
    }
    val agentConf = fileContent.parseYaml.convertTo[AgentConf]

    implicit val system: ActorSystem = ActorSystem(s"$ACTOR_SYSTEM-${agentConf.agentId}")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val agentHttpClient = system.actorOf(AgentHttpClient.props(agentConf))

    val connectionHandler = new ProxyViewAgentConnectionHandler(agentConf, agentHttpClient)
    connectionHandler.register()
    connectionHandler.start()
  }

}
