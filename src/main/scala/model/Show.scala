package dev.akif.showtime
package model

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.LocalDate

final case class Show(title: String, openingDay: LocalDate, genre: Genre) {
  def discountStartDate(discountAfterDays: Long): LocalDate =
    openingDay.plusDays(discountAfterDays)

  def isPlaying(date: LocalDate, showDurationInDays: Long): Boolean = {
    val dayBeforeOpening = openingDay.minusDays(1L)
    val dayAfterLastDay  = openingDay.plusDays(showDurationInDays)
    date.isAfter(dayBeforeOpening) && date.isBefore(dayAfterLastDay)
  }
}

object Show {
  implicit val showCodec: JsonCodec[Show] = DeriveJsonCodec.gen[Show]
}
