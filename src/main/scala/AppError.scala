package dev.akif.showtime

sealed trait AppError {
  val message: String
}

object AppError {
  final case class InvalidBody(`type`: String, value: String, why: String) extends AppError {
    override val message: String = s"Body cannot be read as type '${`type`}' for value '$value': $why"
  }

  final case class InvalidParameter(name: String, value: String, why: String) extends AppError {
    override val message: String = s"Parameter '$name' with value '$value' is invalid: $why"
  }

  final case class InternalError(message: String) extends AppError
}
