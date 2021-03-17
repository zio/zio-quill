package io.getquill.context.cassandra.zio.examples

import io.getquill.{ CassandraZioContext, _ }
import zio.App
import zio.console.putStrLn

object ExampleApp extends App {

  object MyZioPostgresContext extends CassandraZioContext(Literal)
  import MyZioPostgresContext._

  case class Person(name: String, age: Int)

  val zioSession =
    CassandraZioSession.fromPrefix("testStreamDB")

  override def run(args: List[String]) = {
    val people = quote {
      query[Person]
    }
    MyZioPostgresContext.run(people)
      .tap(result => putStrLn(result.toString))
      .provideCustomLayer(zioSession).exitCode
  }
}
