package io.getquill.sources.jdbc.mysql

import io.getquill._
import io.getquill.sources.sql.ProductSpec

class ProductJdbcSpec extends ProductSpec {

  override def beforeAll = {
    testMysqlDB.run(quote(query[Product].delete))
    ()
  }

  "Product" - {
    "Insert multiple products" in {
      val inserted = testMysqlDB.run(productInsert)(productEntries)
      val product = testMysqlDB.run(productById)(inserted(2)).head
      product.description mustEqual productEntries(2).description
      product.id mustEqual inserted(2)
    }
    "Single insert product" in {
      val inserted = testMysqlDB.run(productSingleInsert)
      val product = testMysqlDB.run(productById)(inserted).head
      product.description mustEqual "Window"
      product.id mustEqual inserted
    }
  }

}
