package io.getquill.doobie.issue

import cats.effect._
import doobie._
import doobie.implicits._
import io.getquill._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import io.getquill.doobie.DoobieContext

// https://github.com/tpolecat/doobie/issues/1067
class Issue1067 extends AnyFreeSpec with Matchers {

  import cats.effect.unsafe.implicits.global

  lazy val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    s"jdbc:postgresql://${System.getenv("POSTGRES_HOST")}:${System.getenv("POSTGRES_PORT")}/doobie_test",
    "postgres",
    System.getenv("POSTGRES_PASSWORD")
  )

  val dc = new DoobieContext.Postgres(Literal)
  import dc._

  case class Country(name: String, indepYear: Option[Short])

  "Issue1067 - correctly select many countries, with a null in last position" in {
    val stmt = quote(query[Country])
    val actual = dc.run(stmt).transact(xa).unsafeRunSync()
    actual.count(_.indepYear.isDefined) mustEqual 3
    actual.count(_.indepYear.isEmpty) mustEqual 1
  }

}