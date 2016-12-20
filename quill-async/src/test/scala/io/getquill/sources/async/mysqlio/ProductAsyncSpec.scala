package io.getquill.sources.async.mysqlio

import io.getquill._
import io.getquill.sources.sql.ProductSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

class ProductAsyncSpec extends ProductSpec {

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  override def beforeAll = {
    await(testMysqlIO.run(quote(query[Product].delete)).unsafePerformIO)
    ()
  }

  "Product" - {
    "Insert multiple products" in {
      val inserted = await(testMysqlIO.run(productInsert)(productEntries).unsafePerformIO)
      val product = await(testMysqlIO.run(productById(inserted(2))).unsafePerformIO).head
      product.description mustEqual productEntries(2).description
      product.id mustEqual inserted(2)
    }
    "Single insert product" in {
      val inserted = await(testMysqlIO.run(productSingleInsert).unsafePerformIO)
      val product = await(testMysqlIO.run(productById(inserted)).unsafePerformIO).head
      product.description mustEqual "Window"
      product.id mustEqual inserted
    }
  }

}
