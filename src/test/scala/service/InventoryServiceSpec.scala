package dev.akif.showtime
package service

import dto.{InventoryItem, InventoryResponse, ShowAvailability}
import model.{Genre, Show}
import repository.{InMemoryShowRepository, OrderRepository, ShowRepository}
import service.csv.CSVParser

import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

import java.time.LocalDate

object InventoryServiceSpec extends ZIOSpecDefault with TestLayers {
  val importCSVSuite: Spec[Any, Any] =
    suite("importing CSV")(
      test("fails when CSV file cannot be read") {
        val error    = CSVParser.Error.CannotProcessResource("test", "test")
        val expected = Left(InventoryService.Error.CannotImportCSV(s"Cannot import CSV: ${error.message}"))

        val test = ZIO.serviceWithZIO[InventoryService](_.importCSV).either.map(result => assertTrue(result == expected))

        test.provide(failingCSVParser(error), InventoryService.live, inMemoryShowRepository(), inMemoryOrderRepository(), configLayer)
      },
      test("fails when CSV file cannot be parsed") {
        val expected = Left(
          InventoryService.Error.CannotImportCSV(
            "Cannot import CSV: Cannot parse CSV: Cannot parse date for show 'foo': java.time.format.DateTimeParseException: Text 'bar' could not be parsed at index 0"
          )
        )

        val test = ZIO.serviceWithZIO[InventoryService](_.importCSV).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(List("foo", "bar", "baz")),
          InventoryService.live,
          inMemoryShowRepository(),
          inMemoryOrderRepository(),
          configLayer
        )
      },
      test("fails when saving parsed CSV content fails") {
        val expected = Left(InventoryService.Error.CannotImportCSV(s"Cannot save imported shows: test"))

        val test = ZIO.serviceWithZIO[InventoryService](_.importCSV).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(List("Test Show", "2022-09-13", "COMEDY")),
          InventoryService.live,
          mockShowRepository(mockSaveAll = _ => ZIO.fail(ShowRepository.Error.Unknown("test"))),
          inMemoryOrderRepository(),
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

        test.provide(CSVParser.layer, InventoryService.live, ShowRepository.layer, OrderRepository.layer, configLayer)
      }
    )

  val findAvailabilitiesForSuite: Spec[Any, Any] =
    suite("finding availabilities for shows")(
      test("fails when loading shows fails") {
        val date     = LocalDate.of(2022, 9, 13)
        val expected = Left(InventoryService.Error.CannotGetAvailability(s"Cannot find show availabilities for date '$date': test"))

        val test = ZIO.serviceWithZIO[InventoryService](_.findAvailabilitiesFor(date)).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          mockShowRepository(mockFindByDateAndDuration = (_, _) => ZIO.fail(ShowRepository.Error.Unknown("test"))),
          inMemoryOrderRepository(),
          configLayer
        )
      },
      test("fails when getting availability for a show fails") {
        val date = LocalDate.of(2022, 9, 13)
        val show = Show("Test Show", date, Genre.Musical)
        val expected =
          Left(InventoryService.Error.CannotGetAvailability(s"Cannot find orders for show 'Test Show' on date '$date': test"))

        val test = ZIO.serviceWithZIO[InventoryService](_.findAvailabilitiesFor(date)).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          inMemoryShowRepository(show),
          mockOrderRepository(mockFindByShowAndDate = (_, _) => ZIO.fail(OrderRepository.Error("test"))),
          configLayer
        )
      },
      test("fails when getting price for a show fails") {
        val date = LocalDate.of(2022, 9, 13)
        val show = Show("Test Show", date, Genre.Musical)
        val expected = Left(
          InventoryService.Error
            .CannotGetPrice(s"Cannot get price of show 'Test Show': Price for genre '${show.genre}' isn't configured")
        )

        val test = ZIO.serviceWithZIO[InventoryService](_.findAvailabilitiesFor(date)).either.map(result => assertTrue(result == expected))

        test.provide(inMemoryCSVParser(), InventoryService.live, inMemoryShowRepository(show), inMemoryOrderRepository(), configLayer)
      },
      test("successfully finds some shows") {
        val date = LocalDate.of(2022, 9, 13)
        val expected = InventoryResponse(
          List(
            InventoryItem(
              Genre.Comedy,
              List(
                ShowAvailability("Test Show 2", config.theaterSize - 1, showConfig.pricesByGenre(Genre.Comedy)),
                ShowAvailability("Test Show 1", config.theaterSize, showConfig.pricesByGenre(Genre.Comedy))
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
          inMemoryOrderRepository(("Test Show 2", date, 1)),
          configLayer
        )
      }
    )

  val placeOrderSuite: Spec[Any, Any] =
    suite("placing an order")(
      test("fails when loading the show fails") {
        val date = LocalDate.of(2022, 9, 13)
        val expected =
          Left(InventoryService.Error.CannotPlaceOrder(s"Cannot place order for show 'Test Show' on date '$date' with 1 tickets: test"))

        val test =
          ZIO.serviceWithZIO[InventoryService](_.placeOrder("Test Show", date, 1)).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          mockShowRepository(mockFindByTitleDateAndDuration = (_, _, _) => ZIO.fail(ShowRepository.Error.Unknown("test"))),
          inMemoryOrderRepository(),
          configLayer
        )
      },
      test("fails when getting availability for a show fails") {
        val date = LocalDate.of(2022, 9, 13)
        val show = Show("Test Show", date, Genre.Musical)
        val expected =
          Left(InventoryService.Error.CannotGetAvailability(s"Cannot find orders for show 'Test Show' on date '$date': test"))

        val test =
          ZIO.serviceWithZIO[InventoryService](_.placeOrder("Test Show", date, 1)).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          inMemoryShowRepository(show),
          mockOrderRepository(mockFindByShowAndDate = (_, _) => ZIO.fail(OrderRepository.Error("test"))),
          configLayer
        )
      },
      test("fails when show doesn't have enough tickets left") {
        val date = LocalDate.of(2022, 9, 13)
        val show = Show("Test Show", date, Genre.Comedy)
        val expected = Left(
          InventoryService.Error.CannotPlaceOrder(
            s"Cannot place order for show 'Test Show' on date '$date', 1 tickets requested but the show has 0 tickets left"
          )
        )

        val test =
          ZIO.serviceWithZIO[InventoryService](_.placeOrder("Test Show", date, 1)).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          inMemoryShowRepository(show),
          inMemoryOrderRepository(("Test Show", date, config.theaterSize)),
          configLayer
        )
      },
      test("fails when creating a new order fails") {
        val date = LocalDate.of(2022, 9, 13)
        val show = Show("Test Show", date, Genre.Comedy)
        val expected =
          Left(InventoryService.Error.CannotPlaceOrder(s"Cannot place order for show 'Test Show' on date '$date' with 1 tickets: test"))

        val test =
          ZIO.serviceWithZIO[InventoryService](_.placeOrder("Test Show", date, 1)).either.map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          inMemoryShowRepository(show),
          mockOrderRepository(mockCreate = (_, _, _) => ZIO.fail(OrderRepository.Error("test"))),
          configLayer
        )
      },
      test("succeeds and returns remaining availability") {
        val date = LocalDate.of(2022, 9, 13)
        val show = Show("Test Show", date, Genre.Comedy)

        val test =
          ZIO
            .serviceWithZIO[InventoryService](_.placeOrder("Test Show", date, 1))
            .map(remaining => assertTrue(remaining == config.theaterSize - 1))

        test.provide(inMemoryCSVParser(), InventoryService.live, inMemoryShowRepository(show), inMemoryOrderRepository(), configLayer)
      }
    )

  val availabilityForSuite: Spec[Any, Any] =
    suite("getting availability of a show for a date")(
      test("fails when show is not playing") {
        val show = Show("Test Show", LocalDate.of(2022, 9, 13), Genre.Comedy)
        val expected = Left(
          InventoryService.Error
            .CannotGetAvailability("Cannot get availability of show 'Test Show': The show isn't playing on date '2022-09-12'")
        )

        val test =
          ZIO
            .serviceWithZIO[InventoryService](_.asInstanceOf[InventoryServiceLive].availabilityFor(show, LocalDate.of(2022, 9, 12)))
            .either
            .map(result => assertTrue(result == expected))

        test.provide(inMemoryCSVParser(), InventoryService.live, inMemoryShowRepository(), inMemoryOrderRepository(), configLayer)
      },
      test("fails when loading orders fails") {
        val date = LocalDate.of(2022, 9, 13)
        val show = Show("Test Show", LocalDate.of(2022, 9, 13), Genre.Comedy)
        val expected =
          Left(InventoryService.Error.CannotGetAvailability(s"Cannot find orders for show 'Test Show' on date '$date': test"))

        val test =
          ZIO
            .serviceWithZIO[InventoryService](_.asInstanceOf[InventoryServiceLive].availabilityFor(show, LocalDate.of(2022, 9, 13)))
            .either
            .map(result => assertTrue(result == expected))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          inMemoryShowRepository(),
          mockOrderRepository(mockFindByShowAndDate = (_, _) => ZIO.fail(OrderRepository.Error("test"))),
          configLayer
        )
      },
      test("returns availability") {
        val date = LocalDate.of(2022, 9, 13)
        val show = Show("Test Show", LocalDate.of(2022, 9, 13), Genre.Comedy)

        val test =
          ZIO
            .serviceWithZIO[InventoryService](_.asInstanceOf[InventoryServiceLive].availabilityFor(show, LocalDate.of(2022, 9, 13)))
            .map(availability => assertTrue(availability == config.theaterSize - 1))

        test.provide(
          inMemoryCSVParser(),
          InventoryService.live,
          inMemoryShowRepository(),
          inMemoryOrderRepository(("Test Show", date, 1)),
          configLayer
        )
      }
    )

  val priceForSuite: Spec[Any, Any] =
    suite("getting price of a show for a date")(
      test("fails when show is not playing") {
        val show = Show("Test Show", LocalDate.of(2022, 9, 13), Genre.Comedy)

        for {
          assertion1 <- ZIO
            .serviceWithZIO[InventoryService](_.asInstanceOf[InventoryServiceLive].priceFor(show, LocalDate.of(2022, 9, 12)))
            .either
            .map(result =>
              assertTrue(
                result == Left(
                  InventoryService.Error
                    .CannotGetPrice("Cannot get price of show 'Test Show': The show isn't playing on date '2022-09-12'")
                )
              )
            )

          assertion2 <- ZIO
            .serviceWithZIO[InventoryService](_.asInstanceOf[InventoryServiceLive].priceFor(show, LocalDate.of(2022, 9, 20)))
            .either
            .map(result =>
              assertTrue(
                result == Left(
                  InventoryService.Error
                    .CannotGetPrice("Cannot get price of show 'Test Show': The show isn't playing on date '2022-09-20'")
                )
              )
            )
        } yield {
          assertion1 && assertion2
        }
      },
      test("fails when price for show's genre is not configured") {
        val show = Show("Test Show", LocalDate.of(2022, 9, 13), Genre.Comedy)

        ZIO
          .serviceWithZIO[InventoryService](
            _.asInstanceOf[InventoryServiceLive].priceFor(show.copy(genre = Genre.Musical), LocalDate.of(2022, 9, 13))
          )
          .either
          .map(result =>
            assertTrue(
              result == Left(
                InventoryService.Error
                  .CannotGetPrice(s"Cannot get price of show 'Test Show': Price for genre '${Genre.Musical}' isn't configured")
              )
            )
          )
      },
      test("returns discounted price when show is eligible for discount") {
        val show = Show("Test Show", LocalDate.of(2022, 9, 13), Genre.Comedy)

        ZIO
          .serviceWithZIO[InventoryService](_.asInstanceOf[InventoryServiceLive].priceFor(show, LocalDate.of(2022, 9, 15)))
          .map(price => assertTrue(price == 80))
      },
      test("returns genre price") {
        val show = Show("Test Show", LocalDate.of(2022, 9, 13), Genre.Comedy)

        ZIO
          .serviceWithZIO[InventoryService](_.asInstanceOf[InventoryServiceLive].priceFor(show, LocalDate.of(2022, 9, 13)))
          .map(price => assertTrue(price == 100))
      }
    ).provide(inMemoryCSVParser(), InventoryService.live, inMemoryShowRepository(), inMemoryOrderRepository(), configLayer)

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("InventoryServiceSpec")(importCSVSuite, findAvailabilitiesForSuite, placeOrderSuite, availabilityForSuite, priceForSuite)
}
