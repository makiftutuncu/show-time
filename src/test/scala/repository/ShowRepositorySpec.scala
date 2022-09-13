package dev.akif.showtime
package repository

import model.{Genre, Show}

import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

import java.time.LocalDate

object ShowRepositorySpec extends ZIOSpecDefault with TestLayers {
  val show1: Show = Show("Test Show 1", LocalDate.of(2022, 9, 11), Genre.Comedy)
  val show2: Show = Show("Test Show 2", LocalDate.of(2022, 9, 1), Genre.Musical)
  val show3: Show = Show("Test Show 3", LocalDate.of(2022, 9, 26), Genre.Drama)

  val findByOpeningDayAndDurationSuite: Spec[Any, Any] =
    suite("finding shows by opening dat and duration")(
      test("returns empty list when there are no shows in the repository") {
        ZIO
          .serviceWithZIO[ShowRepository](_.findByDateAndDuration(LocalDate.of(2022, 10, 1), 7))
          .provide(inMemoryShowRepository())
          .map(shows => assertTrue(shows.isEmpty))
      },
      test("returns empty list when there are no shows playing for given date") {
        ZIO
          .serviceWithZIO[ShowRepository](_.findByDateAndDuration(LocalDate.of(2022, 9, 19), 7))
          .map(shows => assertTrue(shows.isEmpty))
      },
      test("returns shows that are still playing") {
        ZIO
          .serviceWithZIO[ShowRepository](_.findByDateAndDuration(LocalDate.of(2022, 9, 13), 14))
          .map(shows => assertTrue(shows == List(show1, show2)))
      }
    ).provide(inMemoryShowRepository(show1, show2, show3))

  val saveAllSpec: Spec[Any, Any] =
    test("saving all shows inserts new shows and updates existing shows") {
      val testShow = Show("Test Show 2", LocalDate.of(2022, 8, 1), Genre.Comedy)

      val test = for {
        dbBefore <- ZIO.serviceWithZIO[ShowRepository](_.asInstanceOf[InMemoryShowRepository].dbRef.get)
        _        <- ZIO.serviceWithZIO[ShowRepository](_.saveAll(List(show1, show2, show3)))
        dbAfter  <- ZIO.serviceWithZIO[ShowRepository](_.asInstanceOf[InMemoryShowRepository].dbRef.get)
      } yield {
        assertTrue(dbBefore == Map(show1.title -> show1, testShow.title -> testShow))
        && assertTrue(dbAfter == Map(show1.title -> show1, show2.title -> show2, show3.title -> show3))
      }

      test.provide(inMemoryShowRepository(show1, testShow))
    }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ShowRepositorySpec")(findByOpeningDayAndDurationSuite, saveAllSpec)
}
