package io.getquill.context.cassandra.zio.examples

import io.getquill.{ CassandraZioContext, _ }
import zio.{ Runtime, Unsafe }
import zio.Console.printLine

object PlainApp {

  object MyZioPostgresContext extends CassandraZioContext(Literal)
  import MyZioPostgresContext._

  case class Person(name: String, age: Int)

  val zioSession =
    CassandraZioSession.fromPrefix("testStreamDB")

  def main(args: Array[String]): Unit = {
    val people = quote {
      query[Person]
    }
    val czio =
      MyZioPostgresContext.run(people)
        .tap(result => printLine(result.toString))
        .provide(zioSession)

    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(czio).getOrThrow()
    }
    ()
  }
}
