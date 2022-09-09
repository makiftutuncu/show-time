package dev.akif.showtime
package config

import zio.config.*
import zio.config.ConfigDescriptor.*

final case class Server(port: Int)

object Server {
  val port: ConfigDescriptor[Int] =
    int("port").describe("Running port of the application").default(8080)

  val descriptor: ConfigDescriptor[Server] = port.to[Server]
}
