package io.getquill.context.async.postgres

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import io.getquill.context.sql.ProductSpec

class ProductAsyncSpec extends ProductSpec {

  val context = testContext
  import testContext._

  def await[T](future: Future[T]) = Await.result(future, Duration.Inf)

  override def beforeAll = {
    await(testContext.run(quote(query[Product].delete)))
    ()
  }

  "Product" - {
    "Insert multiple products" in {
      val inserted = await(testContext.run(productInsert)(productEntries))
      val product = await(testContext.run(productById(inserted(2)))).head
      product.description mustEqual productEntries(2).description
      product.id mustEqual inserted(2)
    }
    "Single insert product" in {
      val inserted = await(testContext.run(productSingleInsert))
      val product = await(testContext.run(productById(inserted))).head
      product.description mustEqual "Window"
      product.id mustEqual inserted
    }
  }

}
