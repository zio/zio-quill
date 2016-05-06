package io.getquill.sources.jdbc.h2

import io.getquill._
import io.getquill.sources.sql.ProductSpec

class ProductJdbcSpec extends ProductSpec {

  override def beforeAll = {
    testH2DB.run(quote(query[Product].delete))
    ()
  }

  "Product" - {
    "Insert multiple products" in {
      /*
      H2 does not support returning generated keys for batch insert.
      So we have to insert one entry at a time in order to get the generated values.
     */
      val inserted = productEntries.map(product => testH2DB.run(productInsert)(product))
      val product = testH2DB.run(productById(inserted(2))).head
      product.description mustEqual productEntries(2).description
      product.id mustEqual inserted(2)
    }
    "Single insert product" in {
      val inserted = testH2DB.run(productSingleInsert)
      val product = testH2DB.run(productById(inserted)).head
      product.description mustEqual "Window"
      product.id mustEqual inserted
    }

    "Single insert with inlined free variable" in {
      val prd = Product(0L, "test1", 1L)
      val inserted = testH2DB.run(product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)))
      val returnedProduct = testH2DB.run(productById(inserted)).head
      returnedProduct.description mustEqual "test1"
      returnedProduct.sku mustEqual 1L
      returnedProduct.id mustEqual inserted
    }

    "Single insert with free variable and explicit quotation" in {
      val prd = Product(0L, "test2", 2L)
      val q1 = quote { product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)) }
      val inserted = testH2DB.run(q1)
      val returnedProduct = testH2DB.run(productById(inserted)).head
      returnedProduct.description mustEqual "test2"
      returnedProduct.sku mustEqual 2L
      returnedProduct.id mustEqual inserted
    }

    "Single product insert with a method quotation" in {
      val prd = Product(0L, "test3", 3L)
      val inserted = testH2DB.run(productInsert(prd))
      val returnedProduct = testH2DB.run(productById(inserted)).head
      returnedProduct.description mustEqual "test3"
      returnedProduct.sku mustEqual 3L
      returnedProduct.id mustEqual inserted
    }
  }

}
