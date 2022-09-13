package dev.akif.showtime

import config.Config
import repository.ShowRepository
import service.InventoryService
import service.csv.CSVParser

import zhttp.http.*
import zhttp.service.Server
import zio.*
import zio.logging.backend.SLF4J

import scala.annotation.nowarn

object Main extends ZIOAppDefault {
  type AppEnvironment = Config & InventoryApp.Environment

  val httpApp: HttpApp[AppEnvironment, AppError] =
    InventoryApp.app

  @nowarn("msg=dead code following this construct")
  val program: ZIO[AppEnvironment, Throwable, Nothing] =
    for {
      _ <- ZIO.serviceWithZIO[InventoryService](_.importCSV).mapError(e => AppError.InternalError(e.message))

      config <- ZIO.service[Config]
      port = config.server.port
      _ <- ZIO.logInfo(s"Server started at http://localhost:$port")

      nothing <- Server.start(port, httpApp)
    } yield nothing

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    program.exitCode
      .provide(Config.live, CSVParser.layer, ShowRepository.inMemory, InventoryService.live)
      .provideLayer(Runtime.removeDefaultLoggers ++ SLF4J.slf4j)
}
