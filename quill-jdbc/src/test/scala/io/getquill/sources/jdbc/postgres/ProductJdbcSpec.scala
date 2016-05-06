package io.getquill.sources.jdbc.postgres

import io.getquill._
import io.getquill.sources.sql.ProductSpec

class ProductJdbcSpec extends ProductSpec {

  override def beforeAll = {
    testPostgresDB.run(quote(query[Product].delete))
    ()
  }

  "Product" - {
    "Insert multiple products" in {
      val inserted = testPostgresDB.run(productInsert)(productEntries)
      val product = testPostgresDB.run(productById(inserted(2))).head
      product.description mustEqual productEntries(2).description
      product.id mustEqual inserted(2)
    }

    "Single insert product" in {
      val inserted = testPostgresDB.run(productSingleInsert)
      val product = testPostgresDB.run(productById(inserted)).head
      product.description mustEqual "Window"
      product.id mustEqual inserted
    }

    "Single insert with inlined free variable" in {
      val prd = Product(0L, "test1", 1L)
      val inserted = testPostgresDB.run(product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)))
      val returnedProduct = testPostgresDB.run(productById(inserted)).head
      returnedProduct.description mustEqual "test1"
      returnedProduct.sku mustEqual 1L
      returnedProduct.id mustEqual inserted
    }

    "Single insert with free variable and explicit quotation" in {
      val prd = Product(0L, "test2", 2L)
      val q1 = quote { product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)) }
      val inserted = testPostgresDB.run(q1)
      val returnedProduct = testPostgresDB.run(productById(inserted)).head
      returnedProduct.description mustEqual "test2"
      returnedProduct.sku mustEqual 2L
      returnedProduct.id mustEqual inserted
    }

    "Single product insert with a method quotation" in {
      val prd = Product(0L, "test3", 3L)
      val inserted = testPostgresDB.run(productInsert(prd))
      val returnedProduct = testPostgresDB.run(productById(inserted)).head
      returnedProduct.description mustEqual "test3"
      returnedProduct.sku mustEqual 3L
      returnedProduct.id mustEqual inserted
    }
  }
}
