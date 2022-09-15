package dev.akif.showtime
package model

import zio.Scope
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

import java.time.LocalDate

object ShowSpec extends ZIOSpecDefault with TestLayers {
  val show: Show = Show("Test Show", LocalDate.of(2022, 9, 11), Genre.Comedy)

  val discountStartDateSpec: Spec[Any, Any] =
    test("discount start date is show's opening date + configured days") {
      assertTrue(show.discountStartDate(showConfig.discount.afterDays) == LocalDate.of(2022, 9, 13))
    }

  val isPlayingSuite: Spec[Any, Any] =
    suite("checking if show is playing")(
      test("returns false when date is before show's opening date") {
        assertTrue(!show.isPlaying(LocalDate.of(2022, 9, 10), showConfig.durationInDays))
      },
      test("returns false when date is after show's opening day + configured show duration") {
        assertTrue(!show.isPlaying(LocalDate.of(2022, 9, 14), showConfig.durationInDays))
      },
      test("returns true when show is still playing") {
        assertTrue(show.isPlaying(LocalDate.of(2022, 9, 11), showConfig.durationInDays))
        && assertTrue(show.isPlaying(LocalDate.of(2022, 9, 12), showConfig.durationInDays))
        && assertTrue(show.isPlaying(LocalDate.of(2022, 9, 13), showConfig.durationInDays))
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ShowSpec")(discountStartDateSpec, isPlayingSuite)
}
