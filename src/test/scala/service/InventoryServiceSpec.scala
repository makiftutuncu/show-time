package dev.akif.showtime
package service

import dto.{InventoryItem, InventoryResponse, ShowAvailability}
import model.{Genre, Show}
import repository.{InMemoryShowRepository, ShowRepository}
import service.csv.CSVParser

import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

import java.time.LocalDate

object InventoryServiceSpec extends ZIOSpecDefault with TestLayers {
  val importCSVSuite: Spec[TestEnvironment & Scope, Any] =
    suite("importing CSV")(
      test("fails when CSV file cannot be read") {
        val error    = CSVParser.Error.CannotProcessResource("test", "test")
        val expected = Left(InventoryService.Error.CannotImportCSV(s"Cannot import CSV: ${error.message}"))

        val test = ZIO.serviceWithZIO[InventoryService](_.importCSV).either.map(result => assertTrue(result == expected))

        test.provide(failingCSVParser(error), InventoryService.live, inMemoryShowRepository(), configLayer)
      },
      test("fails when CSV file cannot be parsed") {
        val expected = Left(
          InventoryService.Error.CannotImportCSV(
            "Cannot import CSV: Cannot parse CSV: Cannot parse date for show 'foo': java.time.format.DateTimeParseException: Text 'bar' could not be parsed at index 0"
          )
        )

        val test = ZIO.serviceWithZIO[InventoryService](_.importCSV).either.map(result => assertTrue(result == expected))

        test.provide(inMemoryCSVParser(List("foo", "bar", "baz")), InventoryService.live, inMemoryShowRepository(), configLayer)
      },
      test("fails when saving parsed CSV content fails") {
        val expected = Left(InventoryService.Error.CannotImportCSV(s"Cannot save imported shows: test"))

        val test = ZIO.serviceWithZIO[InventoryService](_.importCSV).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(List("Test Show", "2022-09-13", "COMEDY")),
          InventoryService.live,
          mockShowRepository(mockSaveAll = _ => ZIO.fail(ShowRepository.Error("test"))),
          configLayer
        )
      },
      test("successfully saves parsed CSV content") {
        val test =
          for {
            dbBefore <- ZIO.serviceWithZIO[ShowRepository](_.asInstanceOf[InMemoryShowRepository].dbRef.get)
            _        <- ZIO.serviceWithZIO[InventoryService](_.importCSV)
            dbAfter  <- ZIO.serviceWithZIO[ShowRepository](_.asInstanceOf[InMemoryShowRepository].dbRef.get)
          } yield {
            assertTrue(dbBefore.isEmpty)
            && assertTrue(dbAfter.nonEmpty)
            && assertTrue(dbAfter.get("1984").contains(Show("1984", LocalDate.of(2022, 10, 13), Genre.Drama)))
          }

        test.provide(CSVParser.layer, InventoryService.live, ShowRepository.inMemory, configLayer)
      }
    )

  val findAvailabilitiesForSuite: Spec[TestEnvironment & Scope, Any] =
    suite("finding availabilities for shows")(
      test("fails when loading shows fails") {
        val date     = LocalDate.of(2022, 9, 13)
        val expected = Left(InventoryService.Error.CannotFindAvailabilities(s"Cannot find show availabilities for date '$date': test"))

        val test = ZIO.serviceWithZIO[InventoryService](_.findAvailabilitiesFor(date)).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          mockShowRepository(mockFindByDateAndDuration = (_, _) => ZIO.fail(ShowRepository.Error("test"))),
          configLayer
        )
      },
      test("fails when getting price for a show fails") {
        val date     = LocalDate.of(2022, 9, 13)
        val show     = Show("Test Show", date, Genre.Musical)
        val expected = Left(InventoryService.Error.CannotFindAvailabilities(s"Cannot get price of show '$show' for date '$date'"))

        val test = ZIO.serviceWithZIO[InventoryService](_.findAvailabilitiesFor(date)).either.map(result => assertTrue(result == expected))

        test.provide(inMemoryCSVParser(), InventoryService.live, inMemoryShowRepository(show), configLayer)
      },
      test("successfully finds some shows") {
        val date = LocalDate.of(2022, 9, 13)
        val expected = InventoryResponse(
          List(
            InventoryItem(
              Genre.Comedy,
              List(
                ShowAvailability("Test Show 1", config.theaterSize, showConfig.pricesByGenre(Genre.Comedy)),
                ShowAvailability("Test Show 2", config.theaterSize, showConfig.pricesByGenre(Genre.Comedy))
              )
            ),
            InventoryItem(Genre.Drama, List(ShowAvailability("Test Show 3", config.theaterSize, showConfig.pricesByGenre(Genre.Drama))))
          )
        )

        val test = ZIO.serviceWithZIO[InventoryService](_.findAvailabilitiesFor(date)).map(inventory => assertTrue(inventory == expected))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          inMemoryShowRepository(
            Show("Test Show 1", date, Genre.Comedy),
            Show("Test Show 2", date, Genre.Comedy),
            Show("Test Show 3", date, Genre.Drama)
          ),
          configLayer
        )
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("InventoryServiceSpec")(importCSVSuite, findAvailabilitiesForSuite)
}
