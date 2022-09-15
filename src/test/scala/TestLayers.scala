package dev.akif.showtime

import config.{Config, Server, Discount as DiscountConfig, Show as ShowConfig}
import dto.InventoryResponse
import model.{Genre, Show}
import repository.{InMemoryOrderRepository, InMemoryShowRepository, OrderRepository, ShowRepository}
import service.InventoryService
import service.csv.{CSVParser, ParsedCSV}

import zio.{IO, Ref, ULayer, ZIO, ZLayer}

import java.time.LocalDate

trait TestLayers {
  val showConfig: ShowConfig =
    ShowConfig(
      durationInDays = 3,
      pricesByGenre = Map(Genre.Comedy -> 100, Genre.Drama -> 50),
      discount = DiscountConfig(afterDays = 2, percentage = 20)
    )

  val config: Config =
    Config(Server(8080), showConfig, 10)

  val configLayer: ULayer[Config] =
    ZLayer.succeed(config)

  def inMemoryShowRepository(shows: Show*): ULayer[ShowRepository] =
    ZLayer.fromZIO(Ref.make(shows.map(s => s.title -> s).toMap).map(InMemoryShowRepository.apply))

  def mockShowRepository(
    mockFindByTitleDateAndDuration: (String, LocalDate, Long) => IO[ShowRepository.Error, Show] = (title, date, _) =>
      ZIO.fail(ShowRepository.Error.ShowNotFound(title, date)),
    mockFindByDateAndDuration: (LocalDate, Long) => IO[ShowRepository.Error, List[Show]] = (_, _) => ZIO.succeed(List.empty),
    mockSaveAll: List[Show] => IO[ShowRepository.Error, Unit] = _ => ZIO.unit
  ): ULayer[ShowRepository] =
    ZLayer.succeed(new ShowRepository {
      override def findByTitleDateAndDuration(title: String, date: LocalDate, durationInDays: Long): IO[ShowRepository.Error, Show] =
        mockFindByTitleDateAndDuration(title, date, durationInDays)

      override def findByDateAndDuration(date: LocalDate, durationInDays: Long): IO[ShowRepository.Error, List[Show]] =
        mockFindByDateAndDuration(date, durationInDays)

      override def saveAll(shows: List[Show]): IO[ShowRepository.Error, Unit] =
        mockSaveAll(shows)
    })

  def inMemoryOrderRepository(orders: (String, LocalDate, Int)*): ULayer[OrderRepository] =
    ZLayer.fromZIO(
      Ref
        .make {
          val showsToData = orders.groupBy(_._1)
          showsToData.map { case (title, _) =>
            val dates = showsToData.get(title).map(_.groupBy(_._2)).getOrElse(Map.empty)
            title -> dates.map { case (date, tuples) =>
              date -> tuples.map(_._3).toList
            }
          }
        }
        .map(InMemoryOrderRepository.apply)
    )

  def mockOrderRepository(
    mockCreate: (String, LocalDate, Int) => IO[OrderRepository.Error, Unit] = (_, _, _) => ZIO.unit,
    mockFindByShowAndDate: (String, LocalDate) => IO[OrderRepository.Error, List[Int]] = (_, _) => ZIO.succeed(List.empty)
  ): ULayer[OrderRepository] =
    ZLayer.succeed(new OrderRepository {
      override def create(title: String, performanceDate: LocalDate, tickets: Int): IO[OrderRepository.Error, Unit] =
        mockCreate(title, performanceDate, tickets)

      override def findByShowTitleAndPerformanceDate(title: String, performanceDate: LocalDate): IO[OrderRepository.Error, List[Int]] =
        mockFindByShowAndDate(title, performanceDate)
    })

  def inMemoryCSVParser(rows: List[String]*): ULayer[CSVParser] =
    ZLayer.succeed(new CSVParser {
      override def parse(name: String): IO[CSVParser.Error, ParsedCSV] =
        ZIO.succeed(ParsedCSV(rows.toList))
    })

  def failingCSVParser(error: CSVParser.Error): ULayer[CSVParser] =
    ZLayer.succeed(new CSVParser {
      override def parse(name: String): IO[CSVParser.Error, ParsedCSV] =
        ZIO.fail(error)
    })

  def mockInventoryService(
    mockImportCSV: IO[InventoryService.Error, Unit] = ZIO.unit,
    mockFindAvailabilitiesFor: LocalDate => IO[InventoryService.Error, InventoryResponse] = _ => ZIO.succeed(InventoryResponse(List.empty)),
    mockPlaceOrder: (String, LocalDate, Int) => IO[InventoryService.Error, Int] = (_, _, _) => ZIO.succeed(0)
  ): ULayer[InventoryService] =
    ZLayer.succeed(new InventoryService {
      override def importCSV: IO[InventoryService.Error, Unit] =
        mockImportCSV

      override def findAvailabilitiesFor(date: LocalDate): IO[InventoryService.Error, InventoryResponse] =
        mockFindAvailabilitiesFor(date)

      override def placeOrder(title: String, performanceDate: LocalDate, tickets: Int): IO[InventoryService.Error, Int] =
        mockPlaceOrder(title, performanceDate, tickets)
    })
}
