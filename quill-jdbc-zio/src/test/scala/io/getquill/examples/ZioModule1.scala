package io.getquill.examples

import java.sql.{ Connection, SQLException }

import io.getquill._
import io.getquill.context.ZioJdbc._
import zio.{ Has, IO, URLayer, ZIO, ZLayer }
import zio.blocking.Blocking

object ziomodule1 {
  case class Person(name: String, age: Int)

  type ZioModule1 = Has[ZioModule1.Service]
  object ZioModule1 {
    trait Service {
      def getAlexes: IO[SQLException, Seq[Person]]
    }

    val live: URLayer[QConnection, ZioModule1] =
      ZLayer.fromServices[Blocking.Service, Connection, ZioModule1.Service] { (blocking, connection) =>
        new Service {
          private val env = Has.allOf(blocking, connection)
          private object MyPostgresContext extends PostgresZioJdbcContext(Literal)
          import MyPostgresContext._

          override def getAlexes: IO[SQLException, Seq[Person]] =
            MyPostgresContext.run(query[Person].filter(p => p.name == "Alex")).provide(env)
        }
      }

    def getAlexes: ZIO[ZioModule1, SQLException, Seq[Person]] =
      ZIO.accessM(_.get.getAlexes)
  }
}
