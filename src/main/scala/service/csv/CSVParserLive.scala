package dev.akif.showtime
package service.csv

import zio.{IO, ZIO}

import scala.io.{BufferedSource, Source}

object CSVParserLive extends CSVParser {
  override def parse(name: String): IO[CSVParser.Error, ParsedCSV] = {
    val acquire =
      ZIO.attemptBlockingIO {
        Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(name))
      }

    val release = { (source: BufferedSource) =>
      ZIO.attemptBlockingIO(source.close()).orDie
    }

    ZIO.scoped {
      ZIO
        .acquireRelease(acquire)(release)
        .catchAllCause { cause =>
          ZIO.fail(CSVParser.Error.CannotProcessResource(name, cause.squashTrace))
        }
        .flatMap { source =>
          source
            .getLines()
            .zipWithIndex
            .foldLeft[IO[CSVParser.Error, List[Array[String]]]](ZIO.succeed(List.empty)) { case (result, (line, index)) =>
              result.flatMap { rows =>
                parseLine(index + 1, line).flatMap { row =>
                  ZIO.logTrace(s"Parsed row '${row.mkString("[", ",", "]")}'").as(rows :+ row)
                }
              }
            }
        }
        .map(ParsedCSV.apply)
    }
  }

  def parseLine(lineNumber: Int, line: String): IO[CSVParser.Error, Array[String]] = {
    val firstQuote = line.indexOf('"')
    if (firstQuote == -1) {
      ZIO.succeed(line.split(","))
    } else {
      val secondQuote = line.indexOf('"', firstQuote + 1)

      if (secondQuote == -1) {
        ZIO.fail(CSVParser.Error.CannotParse(s"Invalid quotation at line #$lineNumber: $line"))
      } else {
        val beforeQuote   = line.substring(0, firstQuote).split(",").toList
        val betweenQuotes = line.substring(firstQuote + 1, secondQuote)
        val afterQuote    = line.substring(Math.min(secondQuote + 1, line.length)).split(",").toList

        val parts = (beforeQuote :+ betweenQuotes) ::: afterQuote

        ZIO.succeed(parts.map(_.trim).filter(_.nonEmpty).toArray)
      }
    }
  }
}
