package io.getquill.examples.other

import io.getquill._
import io.getquill.context.qzio.ImplicitSyntax._
import io.getquill.util.LoadConfig
import zio.Console.printLine
import zio.ZIOAppDefault

import javax.sql.DataSource
import com.zaxxer.hikari.HikariDataSource
import java.sql.SQLException
import zio.{ExitCode, IO, URIO}

object ZioAppImplicitEnv extends ZIOAppDefault {

  object Ctx extends PostgresZioJdbcContext(Literal)

  final case class Person(name: String, age: Int)

  def dataSource: HikariDataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  final case class MyQueryService(ds: DataSource) {
    import Ctx._
    implicit val env: Implicit[DataSource] = Implicit(ds)

    val joes: IO[SQLException, List[Person]]   = Ctx.run(query[Person].filter(p => p.name == "Joe")).implicitly
    val jills: IO[SQLException, List[Person]]  = Ctx.run(query[Person].filter(p => p.name == "Jill")).implicitly
    val alexes: IO[SQLException, List[Person]] = Ctx.run(query[Person].filter(p => p.name == "Alex")).implicitly
  }

  override def run: URIO[Any, ExitCode] =
    MyQueryService(dataSource).joes
      .tap(result => printLine(result.toString))
      .exitCode
}
