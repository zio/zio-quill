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
      val product = testH2DB.run(productById)(inserted(2)).head
      product.description mustEqual productEntries(2).description
      product.id mustEqual inserted(2)
    }
    "Single insert product" in {
      val inserted = testH2DB.run(productSingleInsert)
      val product = testH2DB.run(productById)(inserted).head
      product.description mustEqual "Window"
      product.id mustEqual inserted
    }
  }

}
