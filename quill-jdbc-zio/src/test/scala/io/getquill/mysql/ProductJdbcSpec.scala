package io.getquill.mysql

import io.getquill.ZioSpec
import io.getquill.context.ZioJdbc.Prefix
import io.getquill.context.sql.ProductSpec

class ProductJdbcSpec extends ProductSpec with ZioSpec {

  def prefix = Prefix("testMysqlDB")
  val context = testContext
  import testContext._

  override def beforeAll = {
    super.beforeAll()
    testContext.run(quote(query[Product].delete)).runSyncUnsafe()
    ()
  }

  "Product" - {
    "Insert multiple products" in {
      val (inserted, product) =
        (for {
          i <- testContext.run(liftQuery(productEntries).foreach(e => productInsert(e)))
          ps <- testContext.run(productById(lift(i(2))))
        } yield (i, ps.head)).runSyncUnsafe()

      product.description mustEqual productEntries(2).description
      product.id mustEqual inserted(2)
    }

    "Single insert product" in {
      val (inserted, product) =
        (for {
          i <- testContext.run(productSingleInsert)
          ps <- testContext.run(productById(lift(i)))
        } yield (i, ps.head)).runSyncUnsafe()
      product.description mustEqual "Window"
      product.id mustEqual inserted
    }

    "Single insert with inlined free variable" in {
      val prd = Product(0L, "test1", 1L)
      val (inserted, returnedProduct) =
        (for {
          i <- testContext.run {
            product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)).returningGenerated(_.id)
          }
          rps <- testContext.run(productById(lift(i)))
        } yield (i, rps.head)).runSyncUnsafe()

      returnedProduct.description mustEqual "test1"
      returnedProduct.sku mustEqual 1L
      returnedProduct.id mustEqual inserted
    }

    "Single insert with free variable and explicit quotation" in {
      val prd = Product(0L, "test2", 2L)
      val q1 = quote {
        product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)).returningGenerated(_.id)
      }
      val (inserted, returnedProduct) =
        (for {
          i <- testContext.run(q1)
          rps <- testContext.run(productById(lift(i)))
        } yield (i, rps.head)).runSyncUnsafe()

      returnedProduct.description mustEqual "test2"
      returnedProduct.sku mustEqual 2L
      returnedProduct.id mustEqual inserted
    }

    "Single product insert with a method quotation" in {
      val prd = Product(0L, "test3", 3L)
      val (inserted, returnedProduct) =
        (for {
          i <- testContext.run(productInsert(lift(prd)))
          rps <- testContext.run(productById(lift(i)))
        } yield (i, rps.head)).runSyncUnsafe()

      returnedProduct.description mustEqual "test3"
      returnedProduct.sku mustEqual 3L
      returnedProduct.id mustEqual inserted
    }
  }
}
