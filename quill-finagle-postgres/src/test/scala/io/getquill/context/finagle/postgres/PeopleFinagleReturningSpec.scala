package io.getquill.context.finagle.postgres

import com.twitter.util.{ Await, Future }
import io.getquill.context.sql.base.PeopleReturningSpec

class PeopleFinagleReturningSpec extends PeopleReturningSpec {

  val context: testContext.type = testContext
  import testContext._

  def await[T](future: Future[T]) = Await.result(future)

  override def beforeEach(): Unit = {
    await {
      testContext.transaction {
        for {
          _ <- testContext.run(query[Contact].delete)
          _ <- testContext.run(query[Product].delete)
          _ <- testContext.run(liftQuery(people).foreach(p => peopleInsert(p)))
        } yield ()
      }
    }
    super.beforeEach()
  }

  "Ex 0 insert.returning(_.generatedColumn) mod" in {
    import `Ex 0 insert.returning(_.generatedColumn) mod`._
    await(for {
      id <- testContext.run(op)
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result(id).toSet))
  }

  "Ex 0.5 insert.returning(wholeRecord) mod" in {
    import `Ex 0.5 insert.returning(wholeRecord) mod`._
    await(for {
      product <- testContext.run(op)
      output <- testContext.run(get)
    } yield (output mustEqual result(product)))
  }

  "Ex 1 insert.returningMany(_.generatedColumn) mod" in {
    import `Ex 1 insert.returningMany(_.generatedColumn) mod`._
    await(for {
      id <- testContext.run(op)
      output <- testContext.run(get)
    } yield (output mustEqual result(id.head)))
  }

  "Ex 2 update.returningMany(_.singleColumn) mod" in {
    import `Ex 2 update.returningMany(_.singleColumn) mod`._
    await(for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet))
  }

  "Ex 3 delete.returningMany(wholeRecord)" in {
    import `Ex 3 delete.returningMany(wholeRecord)`._
    await(for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet))
  }

  "Ex 4 update.returningMany(query)" in {
    import `Ex 4 update.returningMany(query)`._
    await(for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet))
  }
}
