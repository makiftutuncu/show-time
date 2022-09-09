package dev.akif.showtime
package repository

import model.Show
import zio.{IO, Ref}

import java.time.LocalDate

final case class InMemoryShowRepository(dbRef: Ref[Map[String, Show]]) extends ShowRepository {
  override def findByOpeningDayAndDuration(openingDay: LocalDate, durationInDays: Int): IO[ShowRepository.Error, List[Show]] =
    dbRef.get.map { db =>
      db.values.filter { show =>
        openingDay.equals(show.openingDay) || (
          openingDay.isAfter(show.openingDay) && openingDay.isBefore(show.openingDay.plusDays(durationInDays.toLong))
        )
      }.toList
    }

  override def saveAll(shows: List[Show]): IO[ShowRepository.Error, Unit] =
    dbRef.update { db =>
      db ++ shows.map(show => show.title -> show)
    }
}
