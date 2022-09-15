package dev.akif.showtime
package repository

import model.Show

import zio.{IO, Ref, ULayer, ZLayer}

import java.time.LocalDate

trait ShowRepository {
  def findByTitleDateAndDuration(title: String, date: LocalDate, durationInDays: Long): IO[ShowRepository.Error, Show]

  def findByDateAndDuration(date: LocalDate, durationInDays: Long): IO[ShowRepository.Error, List[Show]]

  def saveAll(shows: List[Show]): IO[ShowRepository.Error, Unit]
}

object ShowRepository {
  sealed abstract class Error(val message: String)

  object Error {
    final case class ShowNotFound(title: String, date: LocalDate) extends Error(s"Show '$title' is not found for date '$date'")

    final case class Unknown(override val message: String) extends Error(message)
  }

  val layer: ULayer[ShowRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, Show]).map(InMemoryShowRepository))
}
