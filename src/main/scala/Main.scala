package dev.akif.showtime

import zhttp.http.{Http, HttpApp, Response}
import zhttp.service.Server
import zio.*
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {
  type AppEnvironment = Config

  val httpApp: HttpApp[AppEnvironment, Throwable] =
    Http.collectZIO { case _ =>
      ZIO.logInfo("Hello world!").as(Response.ok)
    }

  val program: ZIO[AppEnvironment, Throwable, Unit] =
    ZIO.serviceWithZIO[Config] { config =>
      val port = config.server.port
      ZIO.logInfo(s"Server started at http://localhost:$port") *> Server.start(port, httpApp)
    }

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    program.exitCode
      .provide(Config.live)
      .provideLayer(Runtime.removeDefaultLoggers ++ SLF4J.slf4j)
}
