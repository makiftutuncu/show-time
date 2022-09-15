package dev.akif.showtime
package service

import config.Config
import dto.{InventoryItem, InventoryResponse, ShowAvailability}
import model.{Genre, Show}
import repository.{OrderRepository, ShowRepository}
import service.InventoryService.{Error, csvResourceName}
import service.csv.CSVParser

import zio.{IO, ZIO}

import java.time.LocalDate
import scala.util.Try

final case class InventoryServiceLive(
  csvParser: CSVParser,
  showRepository: ShowRepository,
  orderRepository: OrderRepository,
  config: Config
) extends InventoryService {
  override def importCSV: IO[InventoryService.Error, Unit] = {
    for {
      _ <- ZIO.logInfo(s"Importing shows from CSV")
      _ <- csvParser
        .parse(csvResourceName)
        .flatMap { csv =>
          csv.to[Show] { columns =>
            for {
              name  <- Right(columns.head)
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
        .findByDateAndDuration(date, config.show.durationInDays)
        .flatMapError { error =>
          val message = s"Cannot find show availabilities for date '$date': ${error.message}"
          ZIO.logError(message).as(InventoryService.Error.CannotGetAvailability(message))
        }

      inventoryResponse <- findShows
        .flatMap { shows =>
          val showsByGenre = shows.groupBy(_.genre)

          for {
            inventoryItems <- ZIO.foreachPar(showsByGenre.toList) { case (genre, shows) =>
              for {
                _ <- ZIO.logDebug(s"Checking show availabilities of genre '$genre' for date '$date'")

                availabilities <- ZIO.foreachPar(shows) { show =>
                  (availabilityFor(show, date) zipPar priceFor(show, date)).flatMap { case (availability, price) =>
                    val showAvailability = ShowAvailability(show.title, availability, price)
                    ZIO.logTrace(s"Found show '$showAvailability' for genre '$genre' date '$date'").as(showAvailability)
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

            _ <- ZIO.logInfo(s"Found ${shows.size} shows in ${showsByGenre.size} different genres for date '$date'")
          } yield {
            inventoryItems.sortBy(_.genre.name)
          }
        }
        .map(InventoryResponse.apply)
    } yield {
      inventoryResponse
    }

  override def placeOrder(title: String, performanceDate: LocalDate, tickets: Int): IO[InventoryService.Error, Int] =
    for {
      _ <- ZIO.logInfo(s"Placing order for show '$title' on date '$performanceDate' with $tickets tickets")

      show <- showRepository
        .findByTitleDateAndDuration(title, performanceDate, config.show.durationInDays)
        .flatMapError { cause =>
          val error = cannotPlaceOrder(title, performanceDate, tickets, cause.message)
          ZIO.logError(error.message).as(error)
        }

      availability <- availabilityFor(show, performanceDate)

      _ <- ZIO
        .fail(
          InventoryService.Error.CannotPlaceOrder(
            s"Cannot place order for show '$title' on date '$performanceDate', $tickets tickets requested but the show has $availability tickets left"
          )
        )
        .when(availability < tickets)

      _ <- orderRepository.create(title, performanceDate, tickets).flatMapError { cause =>
        val error = cannotPlaceOrder(title, performanceDate, tickets, cause.message)
        ZIO.logError(error.message).as(error)
      }
    } yield {
      availability - tickets
    }

  def availabilityFor(show: Show, date: LocalDate): IO[InventoryService.Error, Int] =
    for {
      _ <- ZIO.logTrace(s"Getting availability for show '${show.title}'")

      _ <- ZIO
        .fail(
          InventoryService.Error
            .CannotGetAvailability(s"Cannot get availability of show '${show.title}': The show isn't playing on date '$date'")
        )
        .unless(show.isPlaying(date, config.show.durationInDays))

      orderedTickets <- orderRepository
        .findByShowTitleAndPerformanceDate(show.title, date)
        .flatMapError { error =>
          val message = s"Cannot find orders for show '${show.title}' on date '$date': ${error.message}"
          ZIO.logError(message).as(InventoryService.Error.CannotGetAvailability(message))
        }

      availability = Math.max(0, config.theaterSize - orderedTickets.sum)
    } yield {
      availability
    }

  def priceFor(show: Show, date: LocalDate): IO[InventoryService.Error, Int] = {
    val message = s"Cannot get price of show '${show.title}'"
    if (!show.isPlaying(date, config.show.durationInDays)) {
      val error = InventoryService.Error.CannotGetPrice(s"$message: The show isn't playing on date '$date'")
      ZIO.logError(error.message) *> ZIO.fail(error)
    } else {
      config.show.pricesByGenre.get(show.genre) match {
        case None =>
          val error = InventoryService.Error.CannotGetPrice(s"$message: Price for genre '${show.genre}' isn't configured")
          ZIO.logError(error.message) *> ZIO.fail(error)

        case Some(price) =>
          if (date.isAfter(show.discountStartDate(config.show.discount.afterDays).minusDays(1L))) {
            ZIO.succeed(price - ((price * config.show.discount.percentage) / 100))
          } else {
            ZIO.succeed(price)
          }
      }
    }
  }

  private def cannotPlaceOrder(title: String, performanceDate: LocalDate, tickets: Int, error: String): Error.CannotPlaceOrder =
    InventoryService.Error.CannotPlaceOrder(
      s"Cannot place order for show '$title' on date '$performanceDate' with $tickets tickets: $error"
    )
}
