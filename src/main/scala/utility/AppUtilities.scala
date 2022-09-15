package dev.akif.showtime
package utility

import zhttp.http.{Request, Response, Status}
import zio.json.*
import zio.{IO, Tag, ZIO}

trait AppUtilities {
  def errorResponse(error: String, status: Status): Response =
    Response.text(error).setStatus(status)

  def jsonResponse[A](a: A, status: Status = Status.Ok)(implicit encoder: JsonEncoder[A]): Response =
    Response.json(a.toJson).setStatus(status)

  def bodyAs[A](request: Request)(implicit decoder: JsonDecoder[A], tag: Tag[A]): IO[AppError, A] =
    request.body.asString
      .mapError(error => AppError.InvalidBody(tag.tag.shortName, "N/A", error.toString))
      .flatMap { json =>
        ZIO
          .fromEither(json.fromJson[A])
          .mapError(error => AppError.InvalidBody(tag.tag.shortName, json, error))
      }
      .tapError { error =>
        ZIO.logError(error.message)
      }

  def pathParameterAs[A](string: String, name: String, parse: String => A): IO[AppError, A] =
    ZIO
      .attempt(parse(string))
      .mapError(e => AppError.InvalidParameter(name, string, e.toString))
}
