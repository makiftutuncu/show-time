package dev.akif.showtime
package service.csv

import zio.{IO, UIO, ZIO}

import java.io.IOException
import scala.io.{BufferedSource, Source}

object CSVParserLive extends CSVParser {
  override def parse(name: String): IO[CSVParser.Error, ParsedCSV] = {
    val acquire: IO[IOException, BufferedSource] =
      Option(getClass.getClassLoader.getResourceAsStream(name))
        .fold[IO[IOException, BufferedSource]](ZIO.fail(new IOException("File is not found"))) { stream =>
          ZIO.attemptBlockingIO {
            Source.fromInputStream(stream)
          }
        }

    val release: BufferedSource => UIO[Unit] = { (source: BufferedSource) =>
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
            .foldLeft[IO[CSVParser.Error, List[List[String]]]](ZIO.succeed(List.empty)) { case (result, (line, index)) =>
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

  def parseLine(lineNumber: Int, line: String): IO[CSVParser.Error, List[String]] = {
    val firstQuote = line.indexOf('"')
    val result = if (firstQuote == -1) {
      ZIO.succeed(line.split(",").toList)
    } else {
      val secondQuote = line.indexOf('"', firstQuote + 1)

      if (secondQuote == -1) {
        ZIO.fail(CSVParser.Error.CannotParse(s"Invalid quotation at line #$lineNumber: $line"))
      } else {
        val beforeQuote   = line.substring(0, firstQuote).split(",").toList
        val betweenQuotes = line.substring(firstQuote + 1, secondQuote)
        val afterQuote    = line.substring(Math.min(secondQuote + 1, line.length)).split(",").toList

        val parts = (beforeQuote :+ betweenQuotes) ::: afterQuote

        ZIO.succeed(parts)
      }
    }
    result.map(_.map(_.trim).filter(_.nonEmpty))
  }
}
