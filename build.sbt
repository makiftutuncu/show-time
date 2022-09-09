Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version      := "0.1.0"
ThisBuild / scalaVersion := "2.13.8"

val logbackVersion    = "1.4.0"
val zhttpVersion      = "2.0.0-RC11"
val zhttpTestVersion  = "2.0.0-RC9"
val zioVersion        = "2.0.2"
val zioConfigVersion  = "3.0.2"
val zioJsonVersion    = "0.3.0-RC10"
val zioLoggingVersion = "2.1.0"

val logback           = "ch.qos.logback" % "logback-classic"     % logbackVersion
val zhttp             = "io.d11"        %% "zhttp"               % zhttpVersion
val zhttpTest         = "io.d11"        %% "zhttp-test"          % zhttpTestVersion % Test
val zio               = "dev.zio"       %% "zio"                 % zioVersion
val zioConfig         = "dev.zio"       %% "zio-config"          % zioConfigVersion
val zioConfigTypesafe = "dev.zio"       %% "zio-config-typesafe" % zioConfigVersion
val zioLogging        = "dev.zio"       %% "zio-logging-slf4j"   % zioLoggingVersion
val zioJson           = "dev.zio"       %% "zio-json"            % zioJsonVersion
val zioTest           = "dev.zio"       %% "zio-test"            % zioVersion       % Test
val zioTestSbt        = "dev.zio"       %% "zio-test-sbt"        % zioVersion       % Test

val environmentVariablesToPassToDocker =
  Set("SHOW_DURATION_IN_DAYS", "MUSICAL_PRICE", "COMEDY_PRICE", "DRAMA_PRICE", "DISCOUNT_AFTER_DAYS", "DISCOUNT_PERCENTAGE", "THEATER_SIZE")

val dockerSettings = Seq(
  dockerBaseImage                              := "eclipse-temurin:18.0.2.1_1-jre",
  Docker / maintainer                          := "Mehmet Akif Tütüncü <m.akif.tutuncu@gmail.com>",
  Docker / daemonUser                          := "show-time",
  dockerAutoremoveMultiStageIntermediateImages := true,
  dockerUpdateLatest                           := true,
  dockerEnvVars                                := sys.env.filterKeys(environmentVariablesToPassToDocker.contains)
)

lazy val root = (project in file("."))
  .settings(
    name                                          := "show-time",
    Compile / mainClass                           := Some("dev.akif.showtime.Main"),
    idePackagePrefix.withRank(KeyRanks.Invisible) := Some("dev.akif.showtime"),
    libraryDependencies ++= Seq(logback, zhttp, zhttpTest, zio, zioConfig, zioConfigTypesafe, zioLogging, zioJson, zioTest, zioTestSbt),
    scalacOptions += "-Xsource:3",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .settings(dockerSettings)
  .enablePlugins(JavaAppPackaging, DockerPlugin)
