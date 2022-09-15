package dev.akif.showtime

import dto.OrderResponse.Failure.orderResponseFailureCodec
import dto.{OrderRequest, OrderResponse}
import service.InventoryService
import utility.AppUtilities

import zhttp.http.*
import zio.ZIO

import java.time.LocalDate

object InventoryApp extends AppUtilities {
  type Environment = InventoryService

  val app: HttpApp[Environment, AppError] =
    Http.collectZIO[Request] {
      case request @ Method.POST -> Path.root / "inventory" / "order" =>
        placeOrder(request)

      case Method.GET -> Path.root / "inventory" / dateString =>
        findAvailabilitiesFor(dateString)
    }

  def placeOrder(request: Request): ZIO[Environment, AppError, Response] =
    for {
      order <- bodyAs[OrderRequest](request)

      response <- ZIO
        .serviceWithZIO[InventoryService](_.placeOrder(order.show, order.performanceDate, order.tickets))
        .foldZIO(
          {
            case InventoryService.Error.CannotPlaceOrder(message) =>
              ZIO.succeed(jsonResponse(OrderResponse.failure(order, message), Status.BadRequest))

            case error =>
              ZIO.fail(AppError.InternalError(error.message))
          },
          ticketsAvailable => ZIO.succeed(jsonResponse(OrderResponse.success(order, ticketsAvailable)))
        )
    } yield {
      response
    }

  def findAvailabilitiesFor(dateString: String): ZIO[Environment, AppError, Response] =
    for {
      date <- pathParameterAs[LocalDate](dateString, "date", LocalDate.parse)

      response <- ZIO
        .serviceWithZIO[InventoryService](_.findAvailabilitiesFor(date))
        .mapBoth(error => AppError.InternalError(error.message), inventoryResponse => jsonResponse(inventoryResponse))
    } yield {
      response
    }
}
