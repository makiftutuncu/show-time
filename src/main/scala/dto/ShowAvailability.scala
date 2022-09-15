package dev.akif.showtime
package dto

import zio.json.{DeriveJsonCodec, JsonCodec, jsonField}

final case class ShowAvailability(title: String, @jsonField("tickets_available") ticketsAvailable: Int, price: Int)

object ShowAvailability {
  implicit val showAvailabilityCodec: JsonCodec[ShowAvailability] = DeriveJsonCodec.gen[ShowAvailability]
}
