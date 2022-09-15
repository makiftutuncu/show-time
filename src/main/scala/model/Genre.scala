package dev.akif.showtime
package model

import zio.json.JsonCodec

sealed abstract class Genre(val name: String) {
  override def toString: String = name
}

object Genre {
  implicit val genreCodec: JsonCodec[Genre] = JsonCodec.string.transformOrFail(from, _.name)

  case object Musical extends Genre("musical")
  case object Comedy  extends Genre("comedy")
  case object Drama   extends Genre("drama")

  def from(string: String): Either[String, Genre] =
    string match {
      case Musical.name => Right(Musical)
      case Comedy.name  => Right(Comedy)
      case Drama.name   => Right(Drama)
      case _            => Left(s"Invalid genre: $string")
    }
}
