package dev.akif.showtime
package service

import config.Config
import dto.{InventoryItem, InventoryResponse, ShowAvailability}
import model.{Genre, Show}
import repository.ShowRepository
import service.InventoryService.csvResourceName
import service.csv.CSVParser

import zio.{IO, ZIO}

import java.time.LocalDate
import scala.annotation.unused
import scala.util.Try

final case class InventoryServiceLive(csvParser: CSVParser, showRepository: ShowRepository, config: Config) extends InventoryService {
  override def importCSV: IO[InventoryService.Error, Unit] = {
    for {
      _ <- ZIO.logInfo(s"Importing shows from CSV")
      _ <- csvParser
        .parse(csvResourceName)
        .flatMap { csv =>
          csv.to[Show] { columns =>
            for {
              name  <- Right(columns(0))
              date  <- Try(LocalDate.parse(columns(1))).fold(e => Left(s"Cannot parse date for show '$name': $e"), Right.apply)
              genre <- Genre.from(columns(2).toLowerCase)
            } yield {
              Show(name, date, genre)
            }
          }
        }
        .foldZIO(
          error => {
            val message = s"Cannot import CSV: ${error.message}"
            ZIO.logError(message) *> ZIO.fail(InventoryService.Error.CannotImportCSV(message))
          },
          shows =>
            showRepository
              .saveAll(shows)
              .zipLeft(ZIO.logInfo(s"Imported ${shows.size} shows from CSV"))
              .flatMapError { error =>
                val message = s"Cannot save imported shows: ${error.message}"
                ZIO.logError(message).as(InventoryService.Error.CannotImportCSV(message))
              }
        )
    } yield ()
  }

  override def findAvailabilitiesFor(date: LocalDate): IO[InventoryService.Error, InventoryResponse] =
    for {
      _ <- ZIO.logInfo(s"Finding show availabilities for date '$date'")

      findShows = showRepository
        .findByDateAndDuration(date, config.show.durationInDays.toLong)
        .flatMapError { error =>
          val message = s"Cannot find show availabilities for date '$date': ${error.message}"
          ZIO.logError(message).as(InventoryService.Error.CannotFindAvailabilities(message))
        }

      inventoryResponse <- findShows
        .flatMap { shows =>
          val showsByGenre = shows.groupBy(_.genre)

          for {
            _ <- ZIO.logInfo(s"Found ${shows.size} shows in ${showsByGenre.size} different genres for date '$date'")

            inventoryItems <- ZIO.foreachPar(showsByGenre.toList) { case (genre, shows) =>
              for {
                _ <- ZIO.logDebug(s"Checking show availabilities of genre '$genre' for date '$date'")

                availabilities <- ZIO.foreachPar(shows) { show =>
                  for {
                    availability <- availabilityFor(show)
                    price        <- priceFor(show, date)
                    showAvailability = ShowAvailability(show.title, availability, price)
                    _ <- ZIO.logTrace(s"Found show '$showAvailability' for genre '$genre' date '$date'")
                  } yield {
                    showAvailability
                  }
                }

                sortedAvailabilities = availabilities.sortBy { a =>
                  val first  = if (a.ticketsAvailable > 0) a.ticketsAvailable else Int.MaxValue // sold out shows should be at the end
                  val second = a.price
                  val third  = a.title
                  (first, second, third)
                }
              } yield {
                InventoryItem(genre, sortedAvailabilities)
              }
            }
          } yield {
            inventoryItems.sortBy(_.genre.name)
          }
        }
        .map(InventoryResponse.apply)
    } yield {
      inventoryResponse
    }

  def availabilityFor(@unused show: Show): IO[InventoryService.Error, Int] =
    // No ticket sales yet, just return the theater size
    ZIO.succeed(config.theaterSize)

  def priceFor(show: Show, date: LocalDate): IO[InventoryService.Error, Int] =
    show.price(date, config.show) match {
      case None =>
        val message = s"Cannot get price of show '$show' for date '$date'"
        ZIO.logError(message) *> ZIO.fail(InventoryService.Error.CannotFindAvailabilities(message))

      case Some(price) =>
        ZIO.succeed(price)
    }
}
