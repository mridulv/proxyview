import Common._

lazy val proxyview = (project in file("."))
  .settings(

    // Default settings for all subprojects.
    inThisBuild(
      List(
        // TODO(jack, 2017-10-07): We need to upgrade Spark first before upgrading to Scala 2.12.
        scalaVersion := "2.12.12",
        resolvers ++= Seq(
          DefaultMavenRepository,
          Resolver.sonatypeRepo("releases")
        ),
        scalacOptions ++= Seq(
          "-unchecked",
          "-feature",
          "-deprecation",
          "-language:existentials", // "-Ymacro-debug-lite",
          "-language:implicitConversions",
          "-Xlint",
          // This fixes a strange failure on circleci: FlowController.scala: File name too long
          // https://stackoverflow.com/questions/45022231/getting-file-name-too-long-when-running-tests-on-circleci
          "-Xmax-classfile-name",
          "242"
        ),
        fork in Test := true,
        publishArtifact := false,
        // Noop to workaround sbt bug. See https://stackoverflow.com/a/18522706.
        publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
      )
    )
  )
  .aggregate(core, proxyviewClient, proxyviewServer)

lazy val core = Project(id = "core", base = file("core"))
  .settings(buildSettings ++ Seq(exportJars := true))

lazy val proxyviewClient = Project(id = "proxyview-client", base = file("proxyview-client"))
  .settings(buildSettings)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(AshScriptPlugin)
  .settings(dockerBaseImage := "openjdk:jre-alpine")
  .dependsOn(core)
  .aggregate(core)

lazy val proxyviewServer = Project(id = "proxyview-server", base = file("proxyview-server"))
  .settings(buildSettings)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(AshScriptPlugin)
  .settings(dockerBaseImage := "openjdk:jre-alpine")
  .settings(dockerExposedPorts := Seq(8080, 8081))
  .dependsOn(core)
  .aggregate(core)
