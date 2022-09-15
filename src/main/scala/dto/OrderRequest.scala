package dev.akif.showtime
package dto

import zio.json.{DeriveJsonCodec, JsonCodec, jsonField}

import java.time.LocalDate

final case class OrderRequest(show: String, @jsonField("performance_date") performanceDate: LocalDate, tickets: Int)

object OrderRequest {
  implicit val orderRequestCodec: JsonCodec[OrderRequest] = DeriveJsonCodec.gen[OrderRequest]
}
