import Common._
import Dependencies._

name := "proxyview-client"

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.9.9",
  "com.typesafe.akka" %% "akka-http" % "10.1.3",
  "com.typesafe.akka" %% "akka-stream" % "2.5.14",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "com.athaydes.rawhttp" % "rawhttp-core" % "2.4.1",
  "com.typesafe.play" %% "play-json" % "2.10.0-RC5",
  "ai.x" %% "play-json-extensions" % "0.10.0",
  "org.yaml" % "snakeyaml" % "1.8",
  "net.jcazevedo" %% "moultingyaml" % "0.4.2",
  "commons-io" % "commons-io" % "2.6"
) ++ testDependencies

