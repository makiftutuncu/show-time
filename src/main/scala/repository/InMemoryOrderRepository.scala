package dev.akif.showtime
package repository

import zio.{IO, Ref}

import java.time.LocalDate

final case class InMemoryOrderRepository(dbRef: Ref[Map[String, Map[LocalDate, List[Int]]]]) extends OrderRepository {
  override def create(title: String, performanceDate: LocalDate, tickets: Int): IO[OrderRepository.Error, Unit] =
    dbRef.update { db =>
      val showOrders = db.getOrElse(title, Map.empty)
      val newOrders  = showOrders.getOrElse(performanceDate, List.empty) :+ tickets
      db + (title -> (showOrders + (performanceDate -> newOrders)))
    }

  override def findByShowTitleAndPerformanceDate(title: String, performanceDate: LocalDate): IO[OrderRepository.Error, List[Int]] =
    dbRef.get.map { db =>
      db.get(title).flatMap(_.get(performanceDate)).getOrElse(List.empty)
    }
}
