package io.getquill.context.cassandra.zio.examples

import io.getquill.{ CassandraZioContext, _ }
import zio.Runtime
import zio.console.putStrLn

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
        .tap(result => putStrLn(result.toString))
        .provideCustomLayer(zioSession)

    Runtime.default.unsafeRun(czio)
    ()
  }
}
