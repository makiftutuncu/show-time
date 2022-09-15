package dev.akif.showtime
package config

import model.Genre

import zio.config.*
import zio.config.ConfigDescriptor.*

final case class Show(durationInDays: Long, pricesByGenre: Map[Genre, Int], discount: Discount)

object Show {
  val durationInDays: ConfigDescriptor[Long] =
    long("duration-in-days").describe("How many days do shows run")

  val pricesByGenre: ConfigDescriptor[Map[Genre, Int]] =
    nested("prices-by-genre")(mapDescriptor[Genre, Int](Genre.from, _.name, int).describe("Ticket prices by show genre"))

  val discount: ConfigDescriptor[Discount] =
    nested("discount")(Discount.descriptor)

  val descriptor: ConfigDescriptor[Show] = (durationInDays zip pricesByGenre zip discount).to[Show]

  private def mapDescriptor[Key, Value](
    decodeKey: String => Either[String, Key],
    encodeKey: Key => String,
    valueDescriptor: ConfigDescriptor[Value]
  ): ConfigDescriptor[Map[Key, Value]] =
    map(valueDescriptor).transformOrFail[Map[Key, Value]](
      stringKeyMap =>
        stringKeyMap.foldLeft[Either[String, Map[Key, Value]]](Right(Map.empty)) {
          case (result @ Left(_), _)      => result
          case (Right(map), (key, value)) => decodeKey(key).map(k => map + (k -> value))
        },
      map => Right(map.map { case (k, v) => encodeKey(k) -> v })
    )
}
