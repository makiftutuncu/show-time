package dev.akif.showtime
package config

import zio.ZLayer
import zio.config.*
import zio.config.ConfigDescriptor.*
import zio.config.typesafe.*

final case class Config(server: Server, show: Show, theaterSize: Int)

object Config {
  val server: ConfigDescriptor[Server] =
    nested("server")(Server.descriptor)

  val show: ConfigDescriptor[Show] =
    nested("show")(Show.descriptor)

  val theaterSize: ConfigDescriptor[Int] =
    int("theater-size").describe("How many seats are there in the theater")

  val descriptor: ConfigDescriptor[Config] = (server zip show zip theaterSize).to[Config]

  val live: ZLayer[Any, ReadError[String], Config] =
    ZLayer(read(descriptor.from(TypesafeConfigSource.fromResourcePath)))
}
