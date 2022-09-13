package dev.akif.showtime
package service.csv

import zio.Scope
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

object ParsedCSVSpec extends ZIOSpecDefault {
  val toSuite: Spec[Any, Any] =
    suite("converting parsed CSV to something else")(
      test("fails if conversion fails") {
        val expected = Left(CSVParser.Error.CannotParse("test"))

        ParsedCSV(List(List("1", "2"))).to(_ => Left("test")).either.map(result => assertTrue(result == expected))
      },
      test("succeeds") {
        val expected = List("""{"first":1,"second":2}""", """{"first":3,"second":4}""")

        ParsedCSV(List(List("1", "2"), List("3", "4")))
          .to(row => Right(s"""{"first":${row(0)},"second":${row(1)}}"""))
          .map(result => assertTrue(result == expected))
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ParsedCSVSpec")(toSuite)
}
