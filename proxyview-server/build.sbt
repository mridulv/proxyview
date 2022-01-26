import Common._
import Dependencies._
import sbt.Keys.libraryDependencies

name := "proxyview-server"

mainClass in Compile := Some("com.proxyview.server.Main")

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.9.9",
  "com.typesafe.akka" %% "akka-http" % "10.1.3",
  "com.typesafe.akka" %% "akka-stream" % "2.6.18",
  "com.typesafe.akka" %% "akka-actor" % "2.6.18",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "com.typesafe" % "config" % "1.3.1",
  "com.athaydes.rawhttp" % "rawhttp-core" % "2.4.1",
  "com.typesafe.play" %% "play-json" % "2.10.0-RC5",
  "ai.x" %% "play-json-extensions" % "0.10.0",
  "org.yaml" % "snakeyaml" % "1.8",
  "net.jcazevedo" %% "moultingyaml" % "0.4.2",
  "commons-io" % "commons-io" % "2.6"
) ++ testDependencies

