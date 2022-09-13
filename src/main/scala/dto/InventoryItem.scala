package dev.akif.showtime
package dto

import model.Genre

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class InventoryItem(genre: Genre, shows: List[ShowAvailability])

object InventoryItem {
  implicit val inventoryItemCodec: JsonCodec[InventoryItem] = DeriveJsonCodec.gen[InventoryItem]
}
