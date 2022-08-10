package io.getquill.context.jdbc.h2

import io.getquill.context.sql.base.BatchValuesSpec
import io.getquill._

class BatchValuesJdbcSpec extends BatchValuesSpec {

  val context = testContext
  import testContext._

  override def beforeEach(): Unit = {
    testContext.run(sql"TRUNCATE TABLE Product; ALTER TABLE Product ALTER COLUMN id RESTART WITH 1".as[Delete[Product]])
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
}
