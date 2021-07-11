package io.getquill.examples

import io.getquill._
import io.getquill.context.ZioJdbc._
import io.getquill.util.LoadConfig
import zio.console.putStrLn
import zio.{ App, ExitCode, Has, URIO }

import java.io.Closeable
import javax.sql.DataSource
import io.getquill.context.qzio.ImplicitSyntax._

object ZioAppImplicitEnv extends App {

  object Ctx extends PostgresZioJdbcContext(Literal)

  case class Person(name: String, age: Int)

  def dataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  case class MyQueryService(ds: DataSource with Closeable) {
    import Ctx._
    implicit val env = Implicit(Has(ds))

    val joes = Ctx.run(query[Person].filter(p => p.name == "Joe")).implicitDS
    val jills = Ctx.run(query[Person].filter(p => p.name == "Jill")).implicitDS
    val alexes = Ctx.run(query[Person].filter(p => p.name == "Alex")).implicitDS
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    MyQueryService(dataSource)
      .joes
      .tap(result => putStrLn(result.toString))
      .exitCode
  }
}
