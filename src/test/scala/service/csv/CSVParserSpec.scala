package dev.akif.showtime
package service.csv

import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

import java.io.IOException

object CSVParserSpec extends ZIOSpecDefault {
  val parseSuite: Spec[Any, Any] =
    suite("parsing a CSV file")(
      test("fails to parse non-existing CSV file") {
        val file     = "non-existing-data.csv"
        val expected = Left(CSVParser.Error.CannotProcessResource(file, new IOException("File is not found")))

        ZIO.serviceWithZIO[CSVParser](_.parse(file)).either.map(result => assertTrue(result == expected))
      },
      test("fails to parse an invalid CSV file") {
        val file     = "invalid-data.csv"
        val expected = Left(CSVParser.Error.CannotParse("""Invalid quotation at line #1: foo",bar"""))

        ZIO.serviceWithZIO[CSVParser](_.parse(file)).either.map(result => assertTrue(result == expected))
      },
      test("can parse a valid CSV file") {
        val file     = "test-data.csv"
        val expected = ParsedCSV(List(List("foo", "bar", "123"), List("hello,world", "test", "456")))

        ZIO.serviceWithZIO[CSVParser](_.parse(file)).map(result => assertTrue(result == expected))
      }
    ).provide(CSVParser.layer)

  val parseLineSuite: Spec[Any, Any] =
    suite("parsing a CSV line")(
      test("can parse a line with no quotations") {
        for {
          parts1 <- CSVParserLive.parseLine(1, "")
          parts2 <- CSVParserLive.parseLine(1, "a")
          parts3 <- CSVParserLive.parseLine(1, "a,b,c")
        } yield {
          assertTrue(parts1.isEmpty)
          && assertTrue(parts2 sameElements Array("a"))
          && assertTrue(parts3 sameElements Array("a", "b", "c"))
        }
      },
      test("fails when line contains invalid quotations") {
        val expected = Left(CSVParser.Error.CannotParse("""Invalid quotation at line #1: a,"b,c"""))

        CSVParserLive.parseLine(1, """a,"b,c""").either.map(result => assertTrue(result == expected))
      },
      test("can parse a line with quotations and commas") {
        val expected = Array("a", "b,c", "d")

        CSVParserLive.parseLine(1, """a,"b,c",d""").map(parts => assertTrue(parts sameElements expected))
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CSVParserSpec")(parseSuite, parseLineSuite)
}
