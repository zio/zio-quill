package io.getquill.context.ndbc.postgres

import io.getquill.context.sql.PeopleReturningSpec

class PeopleNdbcReturningSpec extends PeopleReturningSpec {

  val context = testContext
  import context.{ get => runSyncUnsafe, _ }

  override def beforeEach(): Unit = {
    runSyncUnsafe {
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
    runSyncUnsafe(for {
      id <- testContext.run(op)
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result(id).toSet))
  }

  "Ex 0.5 insert.returning(wholeRecord) mod" in {
    import `Ex 0.5 insert.returning(wholeRecord) mod`._
    runSyncUnsafe(for {
      product <- testContext.run(op)
      output <- testContext.run(get)
    } yield (output mustEqual result(product)))
  }

  "Ex 1 insert.returningMany(_.generatedColumn) mod" in {
    import `Ex 1 insert.returningMany(_.generatedColumn) mod`._
    runSyncUnsafe(for {
      id <- testContext.run(op)
      output <- testContext.run(get)
    } yield (output mustEqual result(id.head)))
  }

  "Ex 2 update.returningMany(_.singleColumn) mod" in {
    import `Ex 2 update.returningMany(_.singleColumn) mod`._
    runSyncUnsafe(for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet))
  }

  "Ex 3 delete.returningMany(wholeRecord)" in {
    import `Ex 3 delete.returningMany(wholeRecord)`._
    runSyncUnsafe(for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet))
  }

  "Ex 4 update.returningMany(query)" in {
    import `Ex 4 update.returningMany(query)`._
    runSyncUnsafe(for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet))
  }
}
