package io.getquill.context.cassandra.zio.examples

import io.getquill.CassandraZioContext._
import io.getquill.{ CassandraZioContext, _ }
import zio.App
import zio.console.putStrLn

object ExampleApp extends App {

  object MyPostgresContext extends CassandraZioContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  val zioSession =
    Layers.sessionFromPrefix("testStreamDB")

  override def run(args: List[String]) = {
    val people = quote {
      query[Person]
    }
    MyPostgresContext.run(people)
      .tap(result => putStrLn(result.toString))
      .provideCustomLayer(zioSession).exitCode
  }
}
