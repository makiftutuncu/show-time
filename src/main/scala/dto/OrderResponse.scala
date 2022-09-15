package dev.akif.showtime
package dto

import zio.json.{DeriveJsonCodec, JsonCodec, jsonField}

import java.time.LocalDate

sealed trait OrderResponse {
  val status: String
  val show: String
  val performanceDate: LocalDate
}

object OrderResponse {
  final case class Failure(status: String, show: String, @jsonField("performance_date") performanceDate: LocalDate, message: String)
      extends OrderResponse

  object Failure {
    implicit val orderResponseFailureCodec: JsonCodec[Failure] = DeriveJsonCodec.gen[Failure]
  }

  final case class Success(
    status: String,
    show: String,
    @jsonField("performance_date") performanceDate: LocalDate,
    @jsonField("tickets_bought") ticketsBought: Int,
    @jsonField("tickets_available") ticketsAvailable: Int
  ) extends OrderResponse

  object Success {
    implicit val orderResponseSuccessCodec: JsonCodec[Success] = DeriveJsonCodec.gen[Success]
  }

  def failure(request: OrderRequest, message: String): Failure =
    Failure("failure", request.show, request.performanceDate, message)

  def success(request: OrderRequest, ticketsAvailable: Int): Success =
    Success("success", request.show, request.performanceDate, request.tickets, ticketsAvailable)
}
