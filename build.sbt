import PlayKeys._

val commonSettings = Seq(
  scalaVersion := "2.12.5",
  description := "grid",
  organization := "com.gu",
  version := "0.1",
  scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.5"
  )
)

lazy val root = project("grid", path = Some("."))
  .aggregate(commonLib, auth, collections, cropper, imageLoader, leases, thrall)
//  .aggregate(kahuna, mediaApi, metadataEditor, usage, scripts)

addCommandAlias("runAll", "all auth/run collections/run cropper/run image-loader/run leases/run thrall/run")

lazy val commonLib = project("common-lib").settings(
  libraryDependencies ++= Seq(
    // also exists in plugins.sbt, TODO deduplicate this
    "com.typesafe.play" %% "play" % "2.6.12", ws,
    "com.typesafe.play" %% "play-json-joda" % "2.6.9",
    "com.gu" %% "pan-domain-auth-core" % "0.7.0",
    "com.gu" %% "pan-domain-auth-play_2-6" % "0.7.0",
    "com.gu" %% "editorial-permissions-client" % "0.8",
    "com.amazonaws" % "aws-java-sdk" % "1.11.302",
    "org.elasticsearch" % "elasticsearch" % "1.7.6",
    "com.gu" %% "box" % "0.2.0",
    "org.scalaz.stream" %% "scalaz-stream" % "0.8.6",
    "com.drewnoakes" % "metadata-extractor" % "2.8.1",
    "org.im4java" % "im4java" % "1.4.0",
    "com.gu" % "kinesis-logback-appender" % "1.4.2",
    "net.logstash.logback" % "logstash-logback-encoder" % "5.0"
  )
)

lazy val auth = playProject("auth", 9011)

lazy val collections = playProject("collections", 9010)

lazy val cropper = playProject("cropper", 9006)

lazy val imageLoader = playProject("image-loader", 9003)

lazy val kahuna = playProject("kahuna", 9005)

lazy val leases = playProject("leases", 9012).settings(
  libraryDependencies ++= Seq(
    "com.gu" %% "scanamo" % "1.0.0-M5"
  )
)

lazy val mediaApi = playProject("media-api", 9001).settings(
  libraryDependencies ++= Seq(
    "org.apache.commons" % "commons-email" % "1.4"
  )
)

// TODO MRB: can this be combined with media-api
lazy val metadataEditor = playProject("metadata-editor", 9007)

lazy val thrall = playProject("thrall", 9002).settings(
  libraryDependencies ++= Seq(
    "org.codehaus.groovy" % "groovy-json" % "2.3.7",
    "com.yakaz.elasticsearch.plugins" % "elasticsearch-action-updatebyquery" % "2.2.0"
  )
)

lazy val usage = playProject("usage", 9009)

lazy val scripts = project("scripts")
  .dependsOn(commonLib)

def project(projectName: String, path: Option[String] = None): Project =
  Project(projectName, file(path.getOrElse(projectName)))
    .settings(commonSettings)

def playProject(projectName: String, port: Int): Project =
  project(projectName, None)
    .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin, RiffRaffArtifact, BuildInfoPlugin)
    .dependsOn(commonLib)
    .settings(commonSettings ++ Seq(
      playDefaultPort := port,

      debianPackageDependencies := Seq("openjdk-8-jre-headless"),
      maintainer in Linux := "Guardian Developers <dig.dev.software@theguardian.com>",
      packageSummary in Linux := description.value,
      packageDescription := description.value,

      riffRaffManifestProjectName := s"media-service::grid::${name.value}",
      riffRaffUploadArtifactBucket := Some("riffraff-artifact"),
      riffRaffUploadManifestBucket := Some("riffraff-builds"),
      riffRaffArtifactResources := Seq(
        (packageBin in Debian).value -> s"${name.value}/${name.value}.deb",
        file("conf/riff-raff.yaml") -> "riff-raff.yaml"
      )
    ))