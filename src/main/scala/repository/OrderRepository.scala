package dev.akif.showtime
package repository

import zio.{IO, Ref, ULayer, ZLayer}

import java.time.LocalDate

trait OrderRepository {
  def create(title: String, performanceDate: LocalDate, tickets: Int): IO[OrderRepository.Error, Unit]

  def findByShowTitleAndPerformanceDate(title: String, performanceDate: LocalDate): IO[OrderRepository.Error, List[Int]]
}

object OrderRepository {
  final case class Error(message: String)

  val layer: ULayer[OrderRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, Map[LocalDate, List[Int]]]).map(InMemoryOrderRepository))
}
