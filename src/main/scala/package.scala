package dev.akif

import zio.json.JsonEncoder

package object showtime {
  implicit class OverrideToStringToJson[A](val a: A)(implicit encoder: JsonEncoder[A]) {
    override def toString: String = encoder.encodeJson(a, None).toString
  }
}
