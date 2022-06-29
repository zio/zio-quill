package io.getquill.context.cassandra.zio.examples

import io.getquill.{ CassandraZioContext, _ }
import zio.ZIOAppDefault
import zio.Console.printLine

object ExampleApp extends ZIOAppDefault {

  object MyZioPostgresContext extends CassandraZioContext(Literal)
  import MyZioPostgresContext._

  case class Person(name: String, age: Int)

  val zioSessionLayer =
    CassandraZioSession.fromPrefix("testStreamDB")

  override def run = {
    val people = quote {
      query[Person]
    }
    MyZioPostgresContext.run(people)
      .tap(result => printLine(result.toString))
      .provide(zioSessionLayer).exitCode
  }
}
