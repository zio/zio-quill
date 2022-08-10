package io.getquill.context.sql.base

import io.getquill._
import io.getquill.base.Spec
import io.getquill.context.sql.SqlContext
import org.scalatest.BeforeAndAfterEach

trait BatchValuesSpec extends Spec with BeforeAndAfterEach {

  val context: SqlContext[_, _]
  import context._

  case class Product(id: Int, description: String, sku: Long)

  def insertProduct =
    quote((p: Product) => query[Product].insertValue(p))

  def makeProducts(maxRows: Int = 22) =
    (1 to maxRows).map(i => Product(i, s"Product-${i}", i * 100))

  object `Ex 1 - Batch Insert Normal` {
    implicit val meta = insertMeta[Product](_.id)
    val products = makeProducts(22)
    val batchSize = 5
    def opExt = quote {
      (transform: Insert[Product] => Insert[Product]) =>
        liftQuery(products).foreach(p => transform(query[Product].insertValue(p)))
    }
    def op = quote {
      liftQuery(products).foreach(p => query[Product].insertValue(p))
    }
    def get = quote { query[Product] }
    def result = products
  }

  object `Ex 2 - Batch Insert Returning` {
    val productsOriginal = makeProducts(22)
    // want to populate them from DB
    val products = productsOriginal.map(p => p.copy(id = 0))
    val expectedIds = productsOriginal.map(_.id)
    val batchSize = 10
    def op = quote {
      liftQuery(products).foreach(p => query[Product].insertValue(p).returningGenerated(p => p.id))
    }
    def get = quote { query[Product] }
    def result = productsOriginal
  }
}
