package dev.akif.showtime

import dev.akif.showtime.Config.Server
import zio.ZLayer
import zio.config.*
import zio.config.ConfigDescriptor.*
import zio.config.typesafe.*

final case class Config(server: Server)

object Config {
  final case class Server(port: Int)

  object Server {
    val port: ConfigDescriptor[Int] =
      int("port").describe("Running port of the application").default(8080)

    val descriptor: ConfigDescriptor[Server] = port.to[Server]
  }

  val server: ConfigDescriptor[Server] =
    nested("server")(Server.descriptor)

  val descriptor: ConfigDescriptor[Config] = server.to[Config]

  val live: ZLayer[Any, ReadError[String], Config] =
    ZLayer(read(descriptor.from(TypesafeConfigSource.fromResourcePath)))
}
