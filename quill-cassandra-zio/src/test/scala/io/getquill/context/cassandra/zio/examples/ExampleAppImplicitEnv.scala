package io.getquill.context.cassandra.zio.examples

import io.getquill.{ CassandraZioContext, _ }
import zio.ZIOAppDefault
import zio.Console.printLine
import io.getquill.context.qzio.ImplicitSyntax._

object ExampleAppImplicitEnv extends ZIOAppDefault {

  object Ctx extends CassandraZioContext(Literal)

  case class Person(name: String, age: Int)

  val zioSessionLayer =
    CassandraZioSession.fromPrefix("testStreamDB")

  case class MyQueryService(cs: CassandraZioSession) {
    import Ctx._
    implicit val env = Implicit(cs)

    def joes = Ctx.run { query[Person].filter(p => p.name == "Joe") }.implicitly
    def jills = Ctx.run { query[Person].filter(p => p.name == "Jill") }.implicitly
    def alexes = Ctx.run { query[Person].filter(p => p.name == "Alex") }.implicitly
  }

  override def run = {
    val result =
      for {
        csession <- zioSessionLayer.build.useNow
        joes <- MyQueryService(csession.get).joes
      } yield joes

    result
      .tap(result => printLine(result.toString))
      .exitCode
  }
}
