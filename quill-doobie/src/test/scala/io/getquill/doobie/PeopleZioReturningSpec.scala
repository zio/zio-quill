package io.getquill.doobie

import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.getquill.Literal
import io.getquill.context.sql.base.PeopleReturningSpec

class PeopleZioReturningSpec extends PeopleReturningSpec {

  // Logging should appear in test output
  sys.props.put("quill.binds.log", "true")
  sys.props.put("org.slf4j.simpleLogger.defaultLogLevel", "debug")

  import cats.effect.unsafe.implicits.global

  // A transactor that always rolls back.
  lazy val xa: Transactor[IO[A]] = Transactor.after
    .set(
      Transactor.fromDriverManager[IO](
        "org.postgresql.Driver",
        s"jdbc:postgresql://${System.getenv("POSTGRES_HOST")}:${System.getenv("POSTGRES_PORT")}/quill_test",
        "postgres",
        System.getenv("POSTGRES_PASSWORD")
      ),
      HC.commit
    )

  val testContext               = new DoobieContext.Postgres(Literal)
  val context: testContext.type = testContext
  import testContext._

  override def beforeEach(): Unit = {
    (testContext.run(query[Contact].delete) *>
      testContext.run(query[Product].delete) *>
      testContext.run(liftQuery(people).foreach(p => peopleInsert(p)))).transact(xa).unsafeRunSync()
    super.beforeEach()
  }

  "Ex 0 insert.returning(_.generatedColumn) mod" in {
    import `Ex 0 insert.returning(_.generatedColumn) mod`._
    val (id, output) =
      (for {
        id     <- testContext.run(op)
        output <- testContext.run(get)
      } yield (id, output)).transact(xa).unsafeRunSync()

    output.toSet mustEqual result(id).toSet
  }

  "Ex 0.5 insert.returning(wholeRecord) mod" in {
    import `Ex 0.5 insert.returning(wholeRecord) mod`._
    val (product, output) =
      (for {
        product <- testContext.run(op)
        output  <- testContext.run(get)
      } yield (product, output)).transact(xa).unsafeRunSync()

    output mustEqual result(product)
  }

  "Ex 1 insert.returningMany(_.generatedColumn) mod" in {
    import `Ex 1 insert.returningMany(_.generatedColumn) mod`._
    val (id, output) =
      (for {
        id     <- testContext.run(op)
        output <- testContext.run(get)
      } yield (id, output)).transact(xa).unsafeRunSync()

    output mustEqual result(id.head)
  }

  "Ex 2 update.returningMany(_.singleColumn) mod" in {
    import `Ex 2 update.returningMany(_.singleColumn) mod`._
    val output =
      (for {
        opResult <- testContext.run(op)
        _         = opResult.toSet mustEqual expect.toSet
        output   <- testContext.run(get)
      } yield output).transact(xa).unsafeRunSync()

    output.toSet mustEqual result.toSet
  }

  "Ex 3 delete.returningMany(wholeRecord)" in {
    import `Ex 3 delete.returningMany(wholeRecord)`._
    val output =
      (for {
        opResult <- testContext.run(op)
        _         = opResult.toSet mustEqual expect.toSet
        output   <- testContext.run(get)
      } yield output).transact(xa).unsafeRunSync()

    output.toSet mustEqual result.toSet
  }

  "Ex 4 update.returningMany(query)" in {
    import `Ex 4 update.returningMany(query)`._
    val output =
      (for {
        opResult <- testContext.run(op)
        _         = opResult.toSet mustEqual expect.toSet
        output   <- testContext.run(get)
      } yield output).transact(xa).unsafeRunSync()

    output.toSet mustEqual result.toSet
  }
}
