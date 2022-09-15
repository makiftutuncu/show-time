package dev.akif.showtime

import model.{Genre, Show}
import service.InventoryService
import utility.AppUtilities

import zhttp.http.*
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

import java.time.LocalDate

object InventoryAppSpec extends ZIOSpecDefault with TestLayers with AppUtilities {
  val placeOrderSuite: Spec[Any, Any] =
    suite("placing an order for a show")(
      test("fails when body is invalid") {
        val expected = Left(Some(AppError.InvalidBody("OrderRequest", """{"foo":"bar"}""", ".show(missing)")))

        val test = InventoryApp
          .app(
            Request(
              method = Method.POST,
              url = URL.fromString("/inventory/order").fold(throw _, identity),
              body = Body.fromString("""{"foo":"bar"}""")
            )
          )
          .either
          .map(result => assertTrue(result == expected))

        test.provide(InventoryService.live, inMemoryCSVParser(), inMemoryShowRepository(), inMemoryOrderRepository(), configLayer)
      },
      test("fails when placing order fails with an expected error") {
        val expected = """{"status":"failure","show":"Test Show","performance_date":"2022-09-13","message":"test"}"""

        val test =
          for {
            response <- InventoryApp
              .app(
                Request(
                  method = Method.POST,
                  url = URL.fromString("/inventory/order").fold(throw _, identity),
                  body = Body.fromString("""{"show":"Test Show","performance_date":"2022-09-13","tickets":1}""")
                )
              )
            body <- response.body.asString
          } yield {
            assertTrue(response.status == Status.BadRequest) && assertTrue(body == expected)
          }

        test.provide(mockInventoryService(mockPlaceOrder = (_, _, _) => ZIO.fail(InventoryService.Error.CannotPlaceOrder("test"))))
      },
      test("fails when placing order fails with an unexpected error") {
        val expected = Left(Some(AppError.InternalError("test")))

        val test = InventoryApp
          .app(
            Request(
              method = Method.POST,
              url = URL.fromString("/inventory/order").fold(throw _, identity),
              body = Body.fromString("""{"show":"Test Show","performance_date":"2022-09-13","tickets":1}""")
            )
          )
          .either
          .map(result => assertTrue(result == expected))

        test.provide(mockInventoryService(mockPlaceOrder = (_, _, _) => ZIO.fail(InventoryService.Error.CannotGetPrice("test"))))
      },
      test("succeeds and returns remaining availability") {
        val expected =
          """{"status":"success","show":"Test Show","performance_date":"2022-09-13","tickets_bought":1,"tickets_available":9}"""

        val test =
          for {
            response <- InventoryApp
              .app(
                Request(
                  method = Method.POST,
                  url = URL.fromString("/inventory/order").fold(throw _, identity),
                  body = Body.fromString("""{"show":"Test Show","performance_date":"2022-09-13","tickets":1}""")
                )
              )
            body <- response.body.asString
          } yield {
            assertTrue(response.status == Status.Ok) && assertTrue(body == expected)
          }

        test.provide(
          InventoryService.live,
          inMemoryCSVParser(),
          inMemoryShowRepository(Show("Test Show", LocalDate.of(2022, 9, 13), Genre.Comedy)),
          inMemoryOrderRepository(),
          configLayer
        )
      }
    )

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

        test.provide(InventoryService.live, inMemoryCSVParser(), inMemoryShowRepository(), inMemoryOrderRepository(), configLayer)
      },
      test("fails when getting availabilities fails") {
        val date     = LocalDate.of(2022, 9, 13)
        val expected = Left(Some(AppError.InternalError("test")))

        val test =
          InventoryApp
            .app(Request(method = Method.GET, url = URL.fromString(s"/inventory/$date").fold(throw _, identity)))
            .either
            .map(result => assertTrue(result == expected))

        test.provide(mockInventoryService(mockFindAvailabilitiesFor = _ => ZIO.fail(InventoryService.Error.CannotGetAvailability("test"))))
      },
      test("successfully finds some shows") {
        val date = LocalDate.of(2022, 9, 13)
        val expected =
          s"""{"inventory":[{"genre":"comedy","shows":[{"title":"Test Show 1","tickets_available":10,"price":100},{"title":"Test Show 2","tickets_available":10,"price":100}]},{"genre":"drama","shows":[{"title":"Test Show 3","tickets_available":10,"price":50}]}]}"""

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
          inMemoryOrderRepository(),
          configLayer
        )
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("InventoryAppSpec")(placeOrderSuite, findAvailabilitiesForSuite)
}
