package io.getquill.postgres

import io.getquill.ZioSpec
import io.getquill.context.sql.PeopleReturningSpec

class PeopleZioReturningSpec extends PeopleReturningSpec with ZioSpec {

  val context: testContext.type = testContext
  import testContext._

  override def beforeEach(): Unit = {
    testContext.transaction {
      testContext.run(query[Contact].delete) *>
        testContext.run(query[Product].delete) *>
        testContext.run(liftQuery(people).foreach(p => peopleInsert(p)))
    }.runSyncUnsafe()
    super.beforeEach()
  }

  "Ex 0 insert.returning(_.generatedColumn) mod" in {
    import `Ex 0 insert.returning(_.generatedColumn) mod`._
    (for {
      id <- testContext.run(op)
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result(id).toSet)).runSyncUnsafe()
  }

  "Ex 0.5 insert.returning(wholeRecord) mod" in {
    import `Ex 0.5 insert.returning(wholeRecord) mod`._
    (for {
      product <- testContext.run(op)
      output <- testContext.run(get)
    } yield (output mustEqual result(product))).runSyncUnsafe()
  }

  "Ex 1 insert.returningMany(_.generatedColumn) mod" in {
    import `Ex 1 insert.returningMany(_.generatedColumn) mod`._
    (for {
      id <- testContext.run(op)
      output <- testContext.run(get)
    } yield (output mustEqual result(id.head))).runSyncUnsafe()
  }

  "Ex 2 update.returningMany(_.singleColumn) mod" in {
    import `Ex 2 update.returningMany(_.singleColumn) mod`._
    (for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet)).runSyncUnsafe()
  }

  "Ex 3 delete.returningMany(wholeRecord)" in {
    import `Ex 3 delete.returningMany(wholeRecord)`._
    (for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet)).runSyncUnsafe()
  }

  "Ex 4 update.returningMany(query)" in {
    import `Ex 4 update.returningMany(query)`._
    (for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet)).runSyncUnsafe()
  }
}
