package dev.akif.showtime
package model

import config.Show as ShowConfig

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.LocalDate

final case class Show(title: String, openingDay: LocalDate, genre: Genre) {
  def discountStartDate(discountAfterDays: Long): LocalDate =
    openingDay.plusDays(discountAfterDays)

  def isPlaying(date: LocalDate, durationInDays: Long): Boolean = {
    val dayBeforeOpening = openingDay.minusDays(1L)
    val dayAfterLastDay  = openingDay.plusDays(durationInDays)
    date.isAfter(dayBeforeOpening) && date.isBefore(dayAfterLastDay)
  }

  def price(date: LocalDate, config: ShowConfig): Option[Int] =
    if (!isPlaying(date, config.durationInDays.toLong)) {
      None
    } else {
      config.pricesByGenre.get(genre).map { price =>
        if (date.isAfter(discountStartDate(config.discount.afterDays.toLong).minusDays(1L))) {
          (price - ((price * config.discount.percentage) / 100))
        } else {
          price
        }
      }
    }
}

object Show {
  implicit val showCodec: JsonCodec[Show] = DeriveJsonCodec.gen[Show]
}
