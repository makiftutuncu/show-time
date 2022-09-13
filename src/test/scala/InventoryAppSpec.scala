package dev.akif.showtime

import model.{Genre, Show}
import service.InventoryService

import zhttp.http.{Method, Request, Status, URL}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

import java.time.LocalDate

object InventoryAppSpec extends ZIOSpecDefault with TestLayers {
  val findAvailabilitiesForSuite: Spec[Any, Any] =
    suite("finding availabilities for shows")(
      test("fails when date parameter is invalid") {
        val expected = Left(
          Some(
            AppError.InvalidParameter("date", "foo", "java.time.format.DateTimeParseException: Text 'foo' could not be parsed at index 0")
          )
        )

        val test = InventoryApp
          .app(Request(method = Method.GET, url = URL.fromString("/inventory/foo").fold(throw _, identity)))
          .either
          .map(result => assertTrue(result == expected))

        test.provide(InventoryService.live, inMemoryCSVParser(), inMemoryShowRepository(), configLayer)
      },
      test("fails when getting availabilities fails") {
        val date     = LocalDate.of(2022, 9, 13)
        val expected = Left(Some(AppError.InternalError("test")))

        val test =
          InventoryApp
            .app(Request(method = Method.GET, url = URL.fromString(s"/inventory/$date").fold(throw _, identity)))
            .either
            .map(result => assertTrue(result == expected))

        test.provide(
          mockInventoryService(mockFindAvailabilitiesFor = _ => ZIO.fail(InventoryService.Error.CannotFindAvailabilities("test")))
        )
      },
      test("successfully finds some shows") {
        val date = LocalDate.of(2022, 9, 13)
        val expected =
          s"""{"inventory":[{"genre":"comedy","shows":[{"title":"Test Show 1","ticketsAvailable":10,"price":100},{"title":"Test Show 2","ticketsAvailable":10,"price":100}]},{"genre":"drama","shows":[{"title":"Test Show 3","ticketsAvailable":10,"price":50}]}]}"""

        val test =
          for {
            response <- InventoryApp.app(Request(method = Method.GET, url = URL.fromString(s"/inventory/$date").fold(throw _, identity)))
            json     <- response.body.asString
          } yield {
            assertTrue(response.status == Status.Ok) && assertTrue(json == expected)
          }

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
    suite("InventoryAppSpec")(findAvailabilitiesForSuite)
}
