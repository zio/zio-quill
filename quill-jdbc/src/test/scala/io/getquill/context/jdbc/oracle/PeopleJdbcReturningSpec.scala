package io.getquill.context.jdbc.oracle

import io.getquill.context.sql.PeopleReturningSpec

class PeopleJdbcReturningSpec extends PeopleReturningSpec {

  val context = testContext
  import testContext._

  override def beforeEach(): Unit = {
    testContext.transaction {
      testContext.run(query[Contact].delete)
      testContext.run(query[Product].delete)
      testContext.run(liftQuery(people).foreach(p => peopleInsert(p))) // //
    }
    super.beforeEach()
  }

  "Ex 0 insert.returning(_.generatedColumn) mod" in {
    import `Ex 0 insert.returning(_.generatedColumn) mod`._
    val id = testContext.run(op)
    testContext.run(get).toSet mustEqual result(id).toSet
  }

  "Ex 0.5 insert.returning(wholeRecord) mod" in {
    import `Ex 0.5 insert.returning(wholeRecord) mod`._
    val product = testContext.run(op)
    testContext.run(get) mustEqual result(product)
  }

  "Ex 1 insert.returningMany(_.generatedColumn) mod" in {
    import `Ex 1 insert.returningMany(_.generatedColumn) mod`._
    val id = testContext.run(op)
    testContext.run(get).toSet mustEqual result(id.head).toSet
  }

  // Oracle doesn't return anything from getGeneratedKeys of Update
  "Ex 2 update.returningMany(_.singleColumn) mod" ignore {
    import `Ex 2 update.returningMany(_.singleColumn) mod`._
    testContext.run(op).toSet mustEqual expect.toSet
    testContext.run(get).toSet mustEqual result.toSet
  }

  // Oracle doesn't return anything from getGeneratedKeys of Delete
  "Ex 3 delete.returningMany(wholeRecord)" ignore {
    import `Ex 3 delete.returningMany(wholeRecord)`._
    testContext.run(op).toSet mustEqual expect.toSet
    testContext.run(get).toSet mustEqual result.toSet
  }

  // Oracle doesn't return anything from getGeneratedKeys of Update
  "Ex 4 update.returningMany(query)" ignore {
    import `Ex 4 update.returningMany(query)`._
    testContext.run(op).toSet mustEqual expect.toSet
    testContext.run(get).toSet mustEqual result.toSet
  }
}
