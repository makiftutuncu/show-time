package dev.akif.showtime
package service

import config.Config
import dto.InventoryResponse
import repository.{OrderRepository, ShowRepository}
import service.csv.CSVParser

import zio.{IO, URLayer, ZLayer}

import java.time.LocalDate

trait InventoryService {
  def importCSV: IO[InventoryService.Error, Unit]

  def findAvailabilitiesFor(date: LocalDate): IO[InventoryService.Error, InventoryResponse]

  def placeOrder(title: String, performanceDate: LocalDate, tickets: Int): IO[InventoryService.Error, Int]
}

object InventoryService {
  sealed trait Error {
    val message: String
  }

  object Error {
    final case class CannotImportCSV(message: String)       extends Error
    final case class CannotGetAvailability(message: String) extends Error
    final case class CannotGetPrice(message: String)        extends Error
    final case class CannotPlaceOrder(message: String)      extends Error
  }

  val csvResourceName: String = "data.csv"

  val live: URLayer[CSVParser & ShowRepository & OrderRepository & Config, InventoryService] =
    ZLayer.fromFunction(InventoryServiceLive.apply _)
}
