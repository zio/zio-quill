package io.getquill.examples

import io.getquill.{ Literal, PostgresZioJdbcContext }
import io.getquill.context.ZioJdbc._
import zio.Runtime

object PlainApp {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  val zioDS = DataSourceLayer.fromPrefix("testPostgresDB")

  def main(args: Array[String]): Unit = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    val qzio =
      MyPostgresContext.run(people)
        .tap(result => zio.Task(println(result.toString)))
        .provideLayer(zioDS)

    Runtime.default.unsafeRun(qzio)
    ()
  }
}
