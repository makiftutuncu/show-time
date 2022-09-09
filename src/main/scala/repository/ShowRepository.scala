package dev.akif.showtime
package repository

import model.Show
import zio.{IO, Ref, ULayer, ZIO, ZLayer}

import java.time.LocalDate

trait ShowRepository {
  def findByOpeningDayAndDuration(openingDay: LocalDate, durationInDays: Int): IO[ShowRepository.Error, List[Show]]

  def saveAll(shows: List[Show]): IO[ShowRepository.Error, Unit]
}

object ShowRepository {
  sealed trait Error

  def findByOpeningDayAndDuration(openingDay: LocalDate, durationInDays: Int): ZIO[ShowRepository, Error, List[Show]] =
    ZIO.serviceWithZIO[ShowRepository](_.findByOpeningDayAndDuration(openingDay, durationInDays))

  def saveAll(shows: List[Show]): ZIO[ShowRepository, Error, Unit] =
    ZIO.serviceWithZIO[ShowRepository](_.saveAll(shows))

  val inMemory: ULayer[ShowRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, Show]).map(InMemoryShowRepository))
}
