package io.getquill.examples

import com.zaxxer.hikari.HikariDataSource
import io.getquill.context.ZioJdbc.{ QConnection, QDataSource }
import io.getquill.util.LoadConfig
import io.getquill.{ JdbcContextConfig, Literal, PostgresZioJdbcContext }
import zio.console.putStrLn
import zio.Runtime
import zio._
import zio.blocking.Blocking

import java.io.Closeable
import java.sql.{ Connection, SQLException }
import javax.sql.DataSource

object PlainAppDataSource {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  implicit class RunExt[E <: Throwable, T](run: ZIO[Has[Connection] with Blocking, E, T]) {
    def asDao =
      for {
        ds <- ZIO.environment[Has[DataSource with Closeable] with Blocking]
        conn <- QDataSource.toConnection.build.useNow.provide(ds)
        result <- run.provide(conn)
      } yield result

    def asDao(ds: DataSource with Closeable) =
      for {
        block <- ZIO.environment[Blocking]
        result <- run.asDao.provide(Has(ds) ++ block)
      } yield result
  }

  implicit class RunExtDs[E <: Throwable, T](run: ZIO[Has[DataSource with Closeable] with Blocking, E, T]) {
    def provideDataSource(ds: DataSource with Closeable) =
      for {
        block <- ZIO.environment[Blocking]
        result <- run.provide(Has(ds) ++ block)
      } yield result
  }

  def config = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  val zioConn =
    QDataSource.fromDataSource(new HikariDataSource(config)) >>> QDataSource.toConnection

  def main(args: Array[String]): Unit = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    //val qzio =
    //  MyPostgresContext.run(people).asDao.provide(QDataSource.fromDataSource(ds))

    val qzio1 =
      MyPostgresContext.run(people).asInstanceOf[ZIO[QConnection, SQLException, Throwable]]

    val ds: DataSource with Closeable = null
    val qzio2 = qzio1.asDao.provideCustomLayer(QDataSource.fromDataSource(ds))

    val qzio22 = qzio1.asDao.provideDataSource(ds)
    val qzio23 = qzio1.asDao(ds)

    //.tap(result => putStrLn(result.toString))
    //.provideCustomLayer(zioConn)

    Runtime.default.unsafeRun(qzio2)
    ()
  }
}
