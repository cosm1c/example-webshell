import sbt.Keys._

val akkaVersion = "2.4.16"
val akkaHttpVersion = "10.0.1"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(

    name := "example-webshell",

    version := "1.0",

    scalaVersion := "2.12.1",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "ch.qos.logback" % "logback-classic" % "1.1.8" % Runtime,
      "org.slf4j" % "jul-to-slf4j" % "1.7.22",

      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.1" % Test),

    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-deprecation",
      "-encoding", "UTF-8", // yes, this is 2 args
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      //"-Ywarn-dead-code", // N.B. doesn't work well with the ??? hole
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture" //,
      //"-Ywarn-unused-import"     // seems to cause issues with generated sources?
    ),

    // Ensures that static assets in the ui/dist directory are packaged
    unmanagedResourceDirectories in Compile += ((baseDirectory in Compile) (_ / "ui" / "dist")).value,

    // Build Info
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoPackage := "prowse",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildInstant") {
        java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())
      },
      BuildInfoKey.action("gitChecksum") {
        // git describe would be better but requires annotations exist
        Process("git rev-parse HEAD").lines.head
      })
  )

lazy val buildJs = taskKey[Unit]("Build JavaScript frontend")

buildJs := {
  println("Building JavaScript frontend...")
  "cd ui" #&& "npm update" #&& "npm run package" !
}

assembly := (assembly dependsOn buildJs).value

packageBin in Compile := (packageBin in Compile dependsOn buildJs).value
