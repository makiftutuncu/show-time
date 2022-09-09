package dev.akif.showtime
package dto

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class InventoryResponse(inventory: List[InventoryItem])

object InventoryResponse {
  implicit val inventoryResponseCodec: JsonCodec[InventoryResponse] = DeriveJsonCodec.gen[InventoryResponse]
}
