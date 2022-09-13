package dev.akif.showtime
package repository

import model.Show

import zio.{IO, Ref, ULayer, ZLayer}

import java.time.LocalDate

trait ShowRepository {
  def findByDateAndDuration(date: LocalDate, durationInDays: Long): IO[ShowRepository.Error, List[Show]]

  def saveAll(shows: List[Show]): IO[ShowRepository.Error, Unit]
}

object ShowRepository {
  final case class Error(message: String)

  val inMemory: ULayer[ShowRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, Show]).map(InMemoryShowRepository))
}
