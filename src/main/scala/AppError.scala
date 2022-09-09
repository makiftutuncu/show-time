package dev.akif.showtime

import scala.util.control.NoStackTrace

sealed trait AppError extends RuntimeException with NoStackTrace {
  val message: String

  override def getMessage: String = message
}

object AppError {
  final case class InvalidParameter(name: String, value: String, why: String) extends AppError {
    override val message: String = s"Parameter '$name' with value '$value' is invalid: $why"
  }

  final case class InternalError(message: String) extends AppError
}
