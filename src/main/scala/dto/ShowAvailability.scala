package dev.akif.showtime
package dto

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class ShowAvailability(title: String, ticketsAvailable: Int, price: Int)

object ShowAvailability {
  implicit val showAvailabilityCodec: JsonCodec[ShowAvailability] = DeriveJsonCodec.gen[ShowAvailability]
}
