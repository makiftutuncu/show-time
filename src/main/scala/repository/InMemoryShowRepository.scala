package dev.akif.showtime
package repository

import model.Show

import zio.{IO, Ref}

import java.time.LocalDate

final case class InMemoryShowRepository(dbRef: Ref[Map[String, Show]]) extends ShowRepository {
  override def findByTitleDateAndDuration(title: String, date: LocalDate, showDurationInDays: Long): IO[ShowRepository.Error, Show] =
    dbRef.get
      .map { db =>
        db.values.find { show =>
          show.title == title && show.isPlaying(date, showDurationInDays)
        }
      }
      .someOrFail(ShowRepository.Error.ShowNotFound(title, date))

  override def findByDateAndDuration(date: LocalDate, showDurationInDays: Long): IO[ShowRepository.Error, List[Show]] =
    dbRef.get.map { db =>
      db.values.filter { show =>
        show.isPlaying(date, showDurationInDays)
      }.toList
    }

  override def saveAll(shows: List[Show]): IO[ShowRepository.Error, Unit] =
    dbRef.update { db =>
      db ++ shows.map(show => show.title -> show)
    }
}
