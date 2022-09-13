package dev.akif.showtime
package service.csv

import zio.{IO, ULayer, ZLayer}

trait CSVParser {
  def parse(name: String): IO[CSVParser.Error, ParsedCSV]
}

object CSVParser {
  sealed abstract class Error(val message: String)

  object Error {
    final case class CannotProcessResource(name: String, error: String) extends Error(s"Cannot process CSV resource '$name': $error")

    object CannotProcessResource {
      def apply(name: String, error: Throwable): CannotProcessResource = CannotProcessResource(name, error.toString)
    }

    final case class CannotParse(error: String) extends Error(s"Cannot parse CSV: $error")

    object CannotParse {
      def apply(error: Throwable): CannotParse = CannotParse(error.toString)
    }
  }

  val layer: ULayer[CSVParser] = ZLayer.succeed(CSVParserLive)
}
