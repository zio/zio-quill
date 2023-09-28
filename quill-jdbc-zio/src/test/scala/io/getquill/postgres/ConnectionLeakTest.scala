package io.getquill.postgres

import java.util.UUID
import io.getquill.{JdbcContextConfig, Literal, PostgresZioJdbcContext, ZioProxySpec}
import io.getquill.context.sql.ProductSpec
import io.getquill.util.LoadConfig
import io.getquill.context.ZioJdbc._
import io.getquill.context.qzio.ImplicitSyntax.Implicit
import io.getquill.jdbczio.Quill
import zio.{Runtime, Unsafe}

import scala.util.Random
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import zio.ZLayer

class ConnectionLeakTest extends ProductSpec with ZioProxySpec {

  implicit val pool: Implicit[ZLayer[Any, Throwable, DataSource]] = Implicit(
    Quill.DataSource.fromPrefix("testPostgresDB")
  )

  val dataSource: HikariDataSource = JdbcContextConfig(LoadConfig("testPostgresLeakDB")).dataSource
  val context                      = new PostgresZioJdbcContext(Literal)
  import context._

  override def beforeAll: Unit = {
    super.beforeAll()
    context.run(quote(query[Product].delete)).provide(pool.env).runSyncUnsafe()
    ()
  }

  "insert and select without leaking" in {
    val result =
      Unsafe.unsafe { implicit u =>
        Runtime.default.unsafe
          .run(
            context.underlying.transaction {
              import context.underlying._
              for {
                _ <- context.underlying.run {
                       quote {
                         query[Product].insertValue(
                           lift(Product(1, UUID.randomUUID().toString, Random.nextLong()))
                         )
                       }
                     }
                result <- context.underlying.run {
                            query[Product].filter(p => query[Product].map(_.id).max.exists(_ == p.id))
                          }
              } yield (result)
            }
              .map(_.headOption.map(_.id))
              .onDataSource
              .provide(pool.env)
          )
          .getOrThrow()
      }

    Thread.sleep(2000)

    result mustEqual Option(1)
    dataSource.getHikariPoolMXBean.getActiveConnections mustEqual 0

    context.close()
  }

}
