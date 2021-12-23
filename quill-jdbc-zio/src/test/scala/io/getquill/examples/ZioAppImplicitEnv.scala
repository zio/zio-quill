package io.getquill.examples

import io.getquill._
import io.getquill.context.ZioJdbc.DataSource
import io.getquill.context.qzio.ImplicitSyntax._
import io.getquill.util.LoadConfig
import zio.console.putStrLn
import zio.{App, ExitCode, Has, IO, URIO}

import java.io.Closeable
import java.sql.SQLException

object ZioAppImplicitEnv extends App {

  object Ctx extends PostgresZioJdbcContext(Literal)

  final case class Person(name: String, age: Int)

  def dataSource: DataSource with Closeable = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  final case class MyQueryService(ds: DataSource) {
    import Ctx._
    implicit val env: Implicit[Has[DataSource]] = Implicit(Has(ds))

    val joes: IO[SQLException, List[Person]] = Ctx.run(query[Person].filter(p => p.name == "Joe")).implicitly
    val jills: IO[SQLException, List[Person]] = Ctx.run(query[Person].filter(p => p.name == "Jill")).implicitly
    val alexes: IO[SQLException, List[Person]] = Ctx.run(query[Person].filter(p => p.name == "Alex")).implicitly
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    MyQueryService(dataSource)
      .joes
      .tap(result => putStrLn(result.toString))
      .exitCode
  }
}
