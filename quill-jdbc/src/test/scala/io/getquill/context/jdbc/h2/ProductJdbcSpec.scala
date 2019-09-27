package io.getquill.context.jdbc.h2

import io.getquill.context.sql.ProductSpec

class ProductJdbcSpec extends ProductSpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    testContext.run(quote(query[Product].delete))
    ()
  }

  "Product" - {
    "Insert multiple products" in {
      /*
      H2 does not support returning generated keys for batch insert.
      So we have to insert one entry at a time in order to get the generated values.
     */
      val inserted = productEntries.map(product => testContext.run(productInsert(lift(product))))
      val id: Long = inserted(2)
      val product = testContext.run(productById(lift(id))).head
      product.description mustEqual productEntries(2).description
      product.id mustEqual inserted(2)
    }
    "Single insert product" in {
      val inserted = testContext.run(productSingleInsert)
      val product = testContext.run(productById(lift(inserted))).head
      product.description mustEqual "Window"
      product.id mustEqual inserted
    }

    "Single insert with inlined free variable" in {
      val prd = Product(0L, "test1", 1L)
      val inserted = testContext.run {
        product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)).returningGenerated(_.id)
      }
      val returnedProduct = testContext.run(productById(lift(inserted))).head
      returnedProduct.description mustEqual "test1"
      returnedProduct.sku mustEqual 1L
      returnedProduct.id mustEqual inserted
    }

    "Single insert with free variable and explicit quotation" in {
      val prd = Product(0L, "test2", 2L)
      val q1 = quote {
        product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)).returningGenerated(_.id)
      }
      val inserted = testContext.run(q1)
      val returnedProduct = testContext.run(productById(lift(inserted))).head
      returnedProduct.description mustEqual "test2"
      returnedProduct.sku mustEqual 2L
      returnedProduct.id mustEqual inserted
    }

    "Single product insert with a method quotation" in {
      val prd = Product(0L, "test3", 3L)
      val inserted = testContext.run(productInsert(lift(prd)))
      val returnedProduct = testContext.run(productById(lift(inserted))).head
      returnedProduct.description mustEqual "test3"
      returnedProduct.sku mustEqual 3L
      returnedProduct.id mustEqual inserted
    }

    "supports casts from string to number" - {
      "toInt" in {
        case class Product(id: Long, description: String, sku: Int)
        val queried = testContext.run {
          query[Product].filter(_.sku == lift("1004").toInt)
        }.head
        queried.sku mustEqual 1004L
      }
      "toLong" in {
        val queried = testContext.run {
          query[Product].filter(_.sku == lift("1004").toLong)
        }.head
        queried.sku mustEqual 1004L
      }
    }
  }
}
