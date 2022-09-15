package dev.akif.showtime

import config.Config
import repository.{OrderRepository, ShowRepository}
import service.InventoryService
import service.csv.CSVParser
import utility.AppUtilities

import zhttp.http.*
import zhttp.service.Server
import zio.*
import zio.logging.backend.SLF4J

import scala.annotation.nowarn

object Main extends ZIOAppDefault with AppUtilities {
  type AppEnvironment = Config & InventoryApp.Environment

  val httpApp: HttpApp[AppEnvironment, AppError] =
    InventoryApp.app

  val errorHandler: AppError => Response = {
    case e: AppError.InvalidBody      => errorResponse(e.message, Status.BadRequest)
    case e: AppError.InvalidParameter => errorResponse(e.message, Status.BadRequest)
    case e: AppError.InternalError    => errorResponse(e.message, Status.InternalServerError)
  }

  @nowarn("msg=dead code following this construct")
  val program: ZIO[AppEnvironment, Throwable, Nothing] =
    for {
      _ <- ZIO.serviceWithZIO[InventoryService](_.importCSV).mapError(e => new RuntimeException(e.message))

      config <- ZIO.service[Config]
      port = config.server.port
      _ <- ZIO.logInfo(s"Server started at http://localhost:$port")

      errorHandledApp = httpApp.foldHttp[AppEnvironment, Request, Nothing, Response](
        failure => Http.response(errorHandler(failure)),
        defect => Http.response(errorHandler(AppError.InternalError(s"Unexpected error: $defect"))),
        response => Http.response(response),
        Http.empty
      )

      nothing <- Server.start(port, errorHandledApp)
    } yield nothing

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    program.exitCode
      .provide(Config.live, CSVParser.layer, ShowRepository.layer, OrderRepository.layer, InventoryService.live)
      .provideLayer(Runtime.removeDefaultLoggers ++ SLF4J.slf4j)
}
