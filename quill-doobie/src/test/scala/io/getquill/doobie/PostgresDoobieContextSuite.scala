package io.getquill.doobie

import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.getquill._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class PostgresDoobieContextSuite extends AnyFreeSpec with Matchers {

  // Logging should appear in test output
  sys.props.put("quill.binds.log", "true")
  sys.props.put("org.slf4j.simpleLogger.defaultLogLevel", "debug")

  import cats.effect.unsafe.implicits.global

  // A transactor that always rolls back.
  lazy val xa: Transactor[IO[A]] = Transactor.after
    .set(
      Transactor.fromDriverManager[IO](
        "org.postgresql.Driver",
        s"jdbc:postgresql://${System.getenv("POSTGRES_HOST")}:${System.getenv("POSTGRES_PORT")}/doobie_test",
        "postgres",
        System.getenv("POSTGRES_PASSWORD")
      ),
      HC.rollback
    )

  val dc = new DoobieContext.Postgres(Literal)

  import dc.{SqlInfixInterpolator => _, _}
  import dc.compat._

  case class Country(code: String, name: String, population: Int)

  "executeQuery should correctly select a country" in {
    val stmt     = quote(query[Country].filter(_.code == "GBR"))
    val actual   = dc.run(stmt).transact(xa).unsafeRunSync()
    val expected = List(Country("GBR", "United Kingdom", 59623400))
    actual mustEqual expected
  }

  "executeQuerySingle should correctly select a constant" in {
    val stmt     = quote(42)
    val actual   = dc.run(stmt).transact(xa).unsafeRunSync()
    val expected = 42
    actual mustEqual expected
  }

  "streamQuery should correctly stream a bunch of countries" in {
    val stmt     = quote(query[Country])
    val actual   = dc.stream(stmt, 2).transact(xa).as(1).compile.foldMonoid.unsafeRunSync()
    val expected = 4 // this many countries total
    actual mustEqual expected
  }

  "executeAction should correctly update a bunch of countries" in {
    val stmt     = quote(query[Country].filter(_.name like "U%").update(_.name -> "foo"))
    val actual   = dc.run(stmt).transact(xa).unsafeRunSync()
    val expected = 2L // this many countries start with 'U'
    actual mustEqual expected
  }

  "executeBatchAction should correctly do multiple updates" in {
    val stmt = quote {
      liftQuery(List("U%", "I%")).foreach { pat =>
        query[Country].filter(_.name like pat).update(_.name -> "foo")
      }
    }
    val actual   = dc.run(stmt).transact(xa).unsafeRunSync()
    val expected = List(2L, 1L)
    actual mustEqual expected
  }

  // For these last two we need a new table with an auto-generated id, so we'll do a temp table.
  val create: ConnectionIO[Long] = {
    val q = quote {
      qsql"""
        CREATE TEMPORARY TABLE QuillTest (
          id    SERIAL,
          value VARCHAR(42)
        ) ON COMMIT DROP
      """.as[Action[Int]]
    }
    dc.run(q)
  }

  case class QuillTest(id: Int, value: String)

  "executeActionReturning should correctly retrieve a generated key" in {
    val stmt     = quote(query[QuillTest].insertValue(lift(QuillTest(0, "Joe"))).returningGenerated(_.id))
    val actual   = (create *> dc.run(stmt)).transact(xa).unsafeRunSync()
    val expected = 1
    actual mustEqual expected
  }

  "executeBatchActionReturning should correctly retrieve a list of generated keys" in {
    val values = List(QuillTest(0, "Foo"), QuillTest(0, "Bar"), QuillTest(0, "Baz"))
    val stmt = quote {
      liftQuery(values).foreach(a => query[QuillTest].insertValue(a).returningGenerated(_.id))
    }
    val actual   = (create *> dc.run(stmt)).transact(xa).unsafeRunSync()
    val expected = List(1, 2, 3)
    actual mustEqual expected
  }

}
