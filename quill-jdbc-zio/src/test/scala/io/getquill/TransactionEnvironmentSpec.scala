package io.getquill

class TransactionEnvironmentSpec extends Spec {
  val ctx = new PostgresZioJdbcUnderlyingContext(Literal)

  "transaction with mixed environments" - {
    "compiles with just Has[Connection]" in {
      """|import zio.{ Has, ZIO }
         |import java.sql.Connection
         |
         |def service: ZIO[Has[Connection], Throwable, Unit] = ???
         |
         |ctx.transaction(service)""".stripMargin must compile
    }

    "compiles with env requiring more than just Has[Connection]" in {
      """|import zio.{ Has, ZIO }
         |import java.sql.Connection
         |
         |trait Something
         |trait SomeOtherThing
         |def serviceA(): ZIO[Has[Something] with Has[Connection], Throwable, Unit] = ???
         |def serviceB(): ZIO[Has[Connection] with Has[SomeOtherThing], Throwable, Unit] = ???
         |
         |ctx.transaction {
         |  for {
         |    _ <- serviceA() //   ZIO[Has[Something]      with Has[Connection], Throwable, A]
         |    _ <- serviceB() //   ZIO[Has[SomeOtherThing] with Has[Connection], Throwable, A]
         |  } yield ()
         |}""".stripMargin must compile
    }
  }
}
