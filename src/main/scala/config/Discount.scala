package dev.akif.showtime
package config

import zio.config.*
import zio.config.ConfigDescriptor.*

final case class Discount(afterDays: Int, percentage: Int)

object Discount {
  val afterDays: ConfigDescriptor[Int] =
    int("after-days").describe("After how many days of a show's opening day should a discount be applied")

  val percentage: ConfigDescriptor[Int] =
    int("percentage").describe("Discount percentage to apply")

  val descriptor: ConfigDescriptor[Discount] =
    (afterDays zip percentage).to[Discount]
}
