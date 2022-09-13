package dev.akif.showtime

import service.InventoryService
import service.InventoryService.Error

import zhttp.http.*
import zio.ZIO
import zio.json.*

import java.time.LocalDate

object InventoryApp {
  type Environment = InventoryService

  val handleInventoryServiceError: InventoryService.Error => AppError = {
    case Error.CannotImportCSV(message)          => AppError.InternalError(message)
    case Error.CannotFindAvailabilities(message) => AppError.InternalError(message)
  }

  val app: HttpApp[Environment, AppError] =
    Http.collectZIO[Request] { case Method.GET -> Path.root / "inventory" / dateString =>
      findAvailabilitiesFor(dateString)
    }

  def findAvailabilitiesFor(dateString: String): ZIO[Environment, AppError, Response] =
    for {
      date <- ZIO
        .attempt(LocalDate.parse(dateString))
        .mapError(e => AppError.InvalidParameter("date", dateString, e.toString))

      response <- ZIO
        .serviceWithZIO[Environment](_.findAvailabilitiesFor(date))
        .mapBoth(handleInventoryServiceError, inventoryResponse => Response.json(inventoryResponse.toJson))
    } yield {
      response
    }
}
