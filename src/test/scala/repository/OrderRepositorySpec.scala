package dev.akif.showtime
package repository

import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

import java.time.LocalDate

object OrderRepositorySpec extends ZIOSpecDefault with TestLayers {
  val createSpec: Spec[Any, Any] =
    test("creating a new order inserts new item in ordered tickets") {
      val test = for {
        dbBefore <- ZIO.serviceWithZIO[OrderRepository](_.asInstanceOf[InMemoryOrderRepository].dbRef.get)
        _        <- ZIO.serviceWithZIO[OrderRepository](_.create("Test Show", LocalDate.of(2022, 9, 15), 1))
        _        <- ZIO.serviceWithZIO[OrderRepository](_.create("Test Show", LocalDate.of(2022, 9, 15), 2))
        dbAfter  <- ZIO.serviceWithZIO[OrderRepository](_.asInstanceOf[InMemoryOrderRepository].dbRef.get)
      } yield {
        assertTrue(dbBefore.isEmpty)
        && assertTrue(dbAfter == Map("Test Show" -> Map(LocalDate.of(2022, 9, 15) -> List(1, 2))))
      }

      test.provide(inMemoryOrderRepository())
    }

  val findByShowTitleAndPerformanceDateSuite: Spec[Any, Any] =
    suite("finding orders by show title and performance date")(
      test("returns empty list when there are no orders for given show title") {
        ZIO
          .serviceWithZIO[OrderRepository](_.findByShowTitleAndPerformanceDate("Test Show 2", LocalDate.of(2022, 9, 15)))
          .map(orders => assertTrue(orders.isEmpty))
      },
      test("returns empty list when there are no orders for given performance date") {
        ZIO
          .serviceWithZIO[OrderRepository](_.findByShowTitleAndPerformanceDate("Test Show", LocalDate.of(2022, 9, 17)))
          .map(orders => assertTrue(orders.isEmpty))
      },
      test("returns orders for given show title and performance date") {
        ZIO
          .serviceWithZIO[OrderRepository](_.findByShowTitleAndPerformanceDate("Test Show", LocalDate.of(2022, 9, 15)))
          .map(orders => assertTrue(orders == List(1, 2)))
      }
    ).provide(
      inMemoryOrderRepository(
        ("Test Show", LocalDate.of(2022, 9, 15), 1),
        ("Test Show", LocalDate.of(2022, 9, 15), 2),
        ("Test Show", LocalDate.of(2022, 9, 16), 3)
      )
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("OrderRepositorySpec")(createSpec, findByShowTitleAndPerformanceDateSuite)
}
