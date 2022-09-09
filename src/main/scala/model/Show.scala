package dev.akif.showtime
package model

import config.Show as ShowConfig

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.LocalDate

final case class Show(title: String, openingDay: LocalDate, genre: Genre) {
  def price(date: LocalDate, config: ShowConfig): Option[Int] =
    config.pricesByGenre.get(genre).map { price =>
      if (date.isAfter(openingDay.plusDays(config.discount.afterDays.toLong))) {
        (price - ((price * config.discount.percentage) / 100))
      } else {
        price
      }
    }
}

object Show {
  implicit val showCodec: JsonCodec[Show] = DeriveJsonCodec.gen[Show]
}
