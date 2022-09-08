import com.typesafe.sbt.packager.docker._
import scala.util.Try

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version      := "0.1.0"
ThisBuild / scalaVersion := "2.13.8"

val logbackVersion    = "1.4.0"
val zioVersion        = "2.0.2"
val zioConfigVersion  = "3.0.2"
val zioLoggingVersion = "2.1.0"

val logback           = "ch.qos.logback" % "logback-classic"     % logbackVersion
val zio               = "dev.zio"       %% "zio"                 % zioVersion
val zioConfig         = "dev.zio"       %% "zio-config"          % zioConfigVersion
val zioConfigTypesafe = "dev.zio"       %% "zio-config-typesafe" % zioConfigVersion
val zioLogging        = "dev.zio"       %% "zio-logging-slf4j"   % zioLoggingVersion
val zioTest           = "dev.zio"       %% "zio-test"            % zioVersion % Test
val zioTestSbt        = "dev.zio"       %% "zio-test-sbt"        % zioVersion % Test

val dockerSettings = Seq(
  dockerBaseImage := "amazoncorretto:18.0.2-al2",
  // By default there are many commands to create a user, group, set permissions etc. which I don't need
  dockerCommands := dockerCommands.value.flatMap {
    case Cmd("WORKDIR", _*) => Seq(Cmd("WORKDIR", "."))
    case Cmd("COPY", arg) if arg.contains("--chown") =>
      Seq(Cmd("COPY", arg.split(" ").filterNot(_.startsWith("--chown")).mkString(" ")))
    case Cmd("USER", _*)    => Seq.empty
    case Cmd("RUN", _*)     => Seq.empty
    case ExecCmd("RUN", _*) => Seq.empty
    case c                  => Seq(c)
  },
  dockerEnvVars := {
    val keys = Set("PORT")
    sys.env.filterKeys(keys.contains)
  },
  dockerLabels       := Map("maintainer" -> "Mehmet Akif Tutuncu <m.akif.tutuncu@gmail.com>"),
  dockerUpdateLatest := true,
  dockerExposedPorts := sys.env.getOrElse("PORT", "8080").flatMap(s => Try(s.toInt).toOption)
)

lazy val root = (project in file("."))
  .settings(
    name                                          := "show-time",
    Compile / mainClass                           := Some("dev.akif.showtime.Main"),
    idePackagePrefix.withRank(KeyRanks.Invisible) := Some("dev.akif.showtime"),
    libraryDependencies ++= Seq(logback, zio, zioConfig, zioConfigTypesafe, zioLogging, zioTest, zioTestSbt),
    scalacOptions += "-Xsource:3",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .settings(dockerSettings)
  .enablePlugins(JavaAppPackaging, DockerPlugin)
