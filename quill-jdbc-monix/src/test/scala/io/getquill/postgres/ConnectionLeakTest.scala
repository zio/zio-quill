package io.getquill.postgres

import java.util.UUID

import io.getquill.context.monix.Runner
import io.getquill.{ JdbcContextConfig, Literal, PostgresMonixJdbcContext }
import io.getquill.context.sql.ProductSpec
import io.getquill.util.LoadConfig
import monix.execution.Scheduler

import scala.util.Random

class ConnectionLeakTest extends ProductSpec {

  implicit val scheduler = Scheduler.global

  val dataSource = JdbcContextConfig(LoadConfig("testPostgresLeakDB")).dataSource

  val context = new PostgresMonixJdbcContext(Literal, dataSource, Runner.default)
  import context._

  override def beforeAll = {
    context.run(quote(query[Product].delete)).runSyncUnsafe()
    ()
  }

  "insert and select without leaking" in {
    val result =
      context.transaction {
        for {
          _ <- context.run {
            quote {
              query[Product].insert(
                lift(Product(1, UUID.randomUUID().toString, Random.nextLong()))
              )
            }
          }
          result <- context.run {
            query[Product].filter(p => query[Product].map(_.id).max.exists(_ == p.id))
          }
        } yield (result)
      }
        .map(_.headOption.map(_.id))
        .runSyncUnsafe()

    Thread.sleep(2000)

    result mustEqual Option(1)
    dataSource.getHikariPoolMXBean.getActiveConnections mustEqual 0

    context.close()
  }

}
