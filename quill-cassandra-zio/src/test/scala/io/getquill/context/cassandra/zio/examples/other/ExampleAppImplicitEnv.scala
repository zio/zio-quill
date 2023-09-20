package io.getquill.context.cassandra.zio.examples.other

import io.getquill._
import io.getquill.context.qzio.ImplicitSyntax.Implicit
import zio.{ZIO, ZIOAppDefault}
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

    def joes   = Ctx.run(query[Person].filter(p => p.name == "Joe")).implicitly
    def jills  = Ctx.run(query[Person].filter(p => p.name == "Jill")).implicitly
    def alexes = Ctx.run(query[Person].filter(p => p.name == "Alex")).implicitly
  }

  override def run = {
    val result =
      for {
        csession <- ZIO.scoped(zioSessionLayer.build)
        joes     <- MyQueryService(csession.get).joes
      } yield joes

    result
      .tap(result => printLine(result.toString))
      .exitCode
  }
}
