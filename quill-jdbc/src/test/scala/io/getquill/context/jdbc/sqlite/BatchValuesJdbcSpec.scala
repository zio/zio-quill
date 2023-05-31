package io.getquill.context.jdbc.sqlite

import io.getquill.context.sql.base.BatchValuesSpec
import io.getquill._

class BatchValuesJdbcSpec extends BatchValuesSpec { //

  val context = testContext
  import testContext._

  override def beforeEach(): Unit = {
    testContext.run(query[Product].delete)
    // For the Ex 2 test to actually work, the ids of the inserted entities need to start
    // testContext.run(sql"DELETE FROM sqlite_sequence WHERE name='Product';".as[Delete[Product]])
    super.beforeEach()
  }

  "Ex 1 - Batch Insert Normal" in {
    import `Ex 1 - Batch Insert Normal`._
    testContext.run(op, batchSize)
    testContext.run(get).toSet mustEqual result.toSet
  }

  "Ex 2 - Batch Insert Returning" in {
    import `Ex 2 - Batch Insert Returning`._
    val ids = testContext.run(op, batchSize)
    ids mustEqual productsOriginal.map(_.id)
    testContext.run(get) mustEqual productsOriginal
  }

  "Ex 2.5 - Batch Insert Returning" in {
    import `Ex 2 - Batch Insert Returning`._
    val ids = testContext.run(op, batchSize)
    ids mustEqual productsOriginal.map(_.id)
    testContext.run(get) mustEqual productsOriginal
  }

  "Ex 3 - Batch Insert Mixed" in {
    import `Ex 3 - Batch Insert Mixed`._
    def op = quote {
      liftQuery(products).foreach(p => query[Product].insert(_.description -> lift("BlahBlah"), _.sku -> p.sku))
    }
    testContext.run(op, batchSize)
    testContext.run(get).toSet mustEqual result.toSet
  }
}
