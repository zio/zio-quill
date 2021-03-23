package io.getquill.context.cassandra.zio.examples

import io.getquill.{ CassandraZioContext, _ }
import zio.App
import zio.console.putStrLn

object MyZioPostgresContext extends CassandraZioContext(Literal)

case class Person(name: String, age: Int)

object PersonOps {
  import MyZioPostgresContext._

  def byName = quote {
    (name: String, people: Query[Person]) => people.filter(p => p.name == name)
  }
}

object PersonOps2 {
  import MyZioPostgresContext._

  def byAge = quote {
    (age: Int, people: Query[Person]) => people.filter(p => p.age > age)
  }
}

object ExampleApp extends App {
  import MyZioPostgresContext._
  import PersonOps2._
  import PersonOps._

  val zioSession =
    CassandraZioSession.fromPrefix("testStreamDB")

  override def run(args: List[String]) = {
    val people = quote {
      byAge(18, byName("Joe", query[Person]))
    }
    MyZioPostgresContext.run(people)
      .tap(result => putStrLn(result.toString))
      .provideCustomLayer(zioSession).exitCode
  }
}
