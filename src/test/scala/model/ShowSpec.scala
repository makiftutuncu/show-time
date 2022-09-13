package dev.akif.showtime
package model

import zio.Scope
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

import java.time.LocalDate

object ShowSpec extends ZIOSpecDefault with TestLayers {
  val show: Show = Show("Test Show", LocalDate.of(2022, 9, 11), Genre.Comedy)

  val discountStartDateSpec: Spec[Any, Any] =
    test("discount start date is show's opening date + configured days") {
      assertTrue(show.discountStartDate(showConfig.discount.afterDays.toLong) == LocalDate.of(2022, 9, 13))
    }

  val isPlayingSuite: Spec[Any, Any] =
    suite("checking if show is playing")(
      test("returns false when date is before show's opening date") {
        assertTrue(!show.isPlaying(LocalDate.of(2022, 9, 10), showConfig.durationInDays.toLong))
      },
      test("returns false when date is after show's opening day + configured show duration") {
        assertTrue(!show.isPlaying(LocalDate.of(2022, 9, 14), showConfig.durationInDays.toLong))
      },
      test("returns true when show is still playing") {
        assertTrue(show.isPlaying(LocalDate.of(2022, 9, 11), showConfig.durationInDays.toLong))
        && assertTrue(show.isPlaying(LocalDate.of(2022, 9, 12), showConfig.durationInDays.toLong))
        && assertTrue(show.isPlaying(LocalDate.of(2022, 9, 13), showConfig.durationInDays.toLong))
      }
    )

  val priceSuite: Spec[Any, Any] =
    suite("getting price of a show")(
      test("returns None when date is out of show's range") {
        assertTrue(show.price(LocalDate.of(2022, 9, 10), showConfig).isEmpty)
        && assertTrue(show.price(LocalDate.of(2022, 9, 14), showConfig).isEmpty)
      },
      test("returns None when price for show's genre is not configured") {
        assertTrue(show.copy(genre = Genre.Musical).price(LocalDate.of(2022, 9, 12), showConfig).isEmpty)
      },
      test("returns discounted price when show is eligible for discount") {
        assertTrue(show.price(LocalDate.of(2022, 9, 13), showConfig).contains(80))
      },
      test("returns genre price") {
        assertTrue(show.price(LocalDate.of(2022, 9, 12), showConfig).contains(100))
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("PriceSpec")(discountStartDateSpec, isPlayingSuite, priceSuite)
}
