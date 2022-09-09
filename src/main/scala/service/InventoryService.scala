package dev.akif.showtime
package service

import dto.InventoryResponse
import repository.ShowRepository
import service.csv.CSVParser

import config.Config
import zio.{IO, URLayer, ZLayer}

import java.time.LocalDate

trait InventoryService {
  def importCSV: IO[InventoryService.Error, Unit]

  def findAvailabilitiesFor(date: LocalDate): IO[InventoryService.Error, InventoryResponse]
}

object InventoryService {
  sealed trait Error {
    val message: String
  }

  object Error {
    final case class CannotImportCSV(message: String)          extends Error
    final case class CannotFindAvailabilities(message: String) extends Error
  }

  val csvResourceName: String = "data.csv"

  val live: URLayer[CSVParser & ShowRepository & Config, InventoryService] =
    ZLayer.fromFunction(InventoryServiceLive.apply _)
}
