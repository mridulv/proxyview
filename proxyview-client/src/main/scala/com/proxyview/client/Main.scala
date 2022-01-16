package com.proxyview.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.charset.StandardCharsets
import net.jcazevedo.moultingyaml._
import com.proxyview.common.models.AgentConf

import java.nio.file.Paths

object Main {

  def main(args: Array[String]): Unit = {
    val defaultConfigDir = "/opt/docker/data/service/conf"
    val configDir = Paths.get(Option(System.getenv("LHUB_CONFIG_DIR")).getOrElse(defaultConfigDir))
    val fileContent = FileUtils.readFileToString(configDir.resolve("application.yaml").toFile, StandardCharsets.UTF_8)
    val agentConf = fileContent.parseYaml.convertTo[AgentConf]

    implicit val system = ActorSystem(s"agent-client-${agentConf.agentId}")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val agentHttpClient = system.actorOf(AgentHttpClient.props(agentConf))

    val connectionHandler = new ProxyViewAgentConnectionHandler(agentConf, agentHttpClient)
    connectionHandler.register()
    connectionHandler.start()
  }

}
