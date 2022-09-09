package dev.akif.showtime
package service.csv

import zio.{IO, ZIO}

final case class ParsedCSV(rows: List[Array[String]]) {
  def to[A](decoder: Array[String] => Either[String, A]): IO[CSVParser.Error, List[A]] =
    ZIO.foreachPar(rows) { row =>
      decoder(row) match {
        case Left(error) => ZIO.fail(CSVParser.Error.CannotParse(error))
        case Right(a)    => ZIO.logDebug(s"Converted row to '$a'").as(a)
      }
    }
}

object ParsedCSV {
  val empty: ParsedCSV = ParsedCSV(List.empty)
}
