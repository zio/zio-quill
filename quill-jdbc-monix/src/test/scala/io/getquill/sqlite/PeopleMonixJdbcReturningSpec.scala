package io.getquill.sqlite

import io.getquill.MonixSpec
import io.getquill.context.sql.base.PeopleReturningSpec

class PeopleMonixJdbcReturningSpec extends PeopleReturningSpec with MonixSpec {

  val context: testContext.type = testContext
  import testContext._

  override def beforeEach(): Unit = {
    testContext.transaction {
      for {
        _ <- testContext.run(query[Contact].delete)
        _ <- testContext.run(query[Product].delete)
        _ <- testContext.run(liftQuery(people).foreach(p => peopleInsert(p)))
      } yield ()
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

  // ignored because not supported
  "Ex 0.5 insert.returning(wholeRecord) mod" ignore {
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

  // ignored because not supported
  "Ex 2 update.returningMany(_.singleColumn) mod" ignore {
    import `Ex 2 update.returningMany(_.singleColumn) mod`._
    (for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet)).runSyncUnsafe()
  }

  // ignored because not supported
  "Ex 3 delete.returningMany(wholeRecord)" ignore {
    import `Ex 3 delete.returningMany(wholeRecord)`._
    (for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet)).runSyncUnsafe()
  }

  // ignored because not supported
  "Ex 4 update.returningMany(query)" ignore {
    import `Ex 4 update.returningMany(query)`._
    (for {
      opResult <- testContext.run(op)
      _ = opResult.toSet mustEqual expect.toSet
      output <- testContext.run(get)
    } yield (output.toSet mustEqual result.toSet)).runSyncUnsafe()
  }
}
