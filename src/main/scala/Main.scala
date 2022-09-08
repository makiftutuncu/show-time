package dev.akif.showtime

import zio.*
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {
  type AppEnvironment = Any

  val program: ZIO[AppEnvironment, Throwable, Unit] =
    ZIO
      .logInfo("Hello world!")

  override def run: ZIO[AppEnvironment & ZIOAppArgs & Scope, Any, Any] =
    program
      .provideLayer(Runtime.removeDefaultLoggers ++ SLF4J.slf4j)
}
