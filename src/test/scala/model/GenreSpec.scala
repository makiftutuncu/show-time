package dev.akif.showtime
package model

import model.Genre.{Comedy, Drama, Musical}

import zio.Scope
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

object GenreSpec extends ZIOSpecDefault {
  val fromSuite: Spec[Any, Any] =
    suite("building a Genre from a String")(
      test("fails when the String is not a valid Genre") {
        assertTrue(Genre.from("test") == Left("Invalid genre: test"))
      },
      test("succeeds") {
        assertTrue(Genre.from("musical").contains(Musical))
        && assertTrue(Genre.from("comedy").contains(Comedy))
        && assertTrue(Genre.from("drama").contains(Drama))
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("GenreSpec")(fromSuite)
}
