package io.getquill.context.async.mysql

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import io.getquill.context.sql.ProductSpec
import io.getquill.context.sql.Id

class ProductMysqlAsyncSpec extends ProductSpec {

  val context = testContext
  import testContext._

  override def beforeAll = {
    await(testContext.run(quote(query[Product].delete)))
    ()
  }

  "Product" - {
    "Insert multiple products" in {
      val inserted =
        await(Future.sequence(productEntries.map(product => testContext.run(productInsert(lift(product))))))
      val product = await(testContext.run(productById(lift(inserted(2))))).head
      product.description mustEqual productEntries(2).description
      product.id mustEqual inserted(2)
    }
    "Single insert product" in {
      val inserted = await(testContext.run(productSingleInsert))
      val product = await(testContext.run(productById(lift(inserted)))).head
      product.description mustEqual "Window"
      product.id mustEqual inserted
    }

    "Single insert with inlined free variable" in {
      val prd = Product(0L, "test1", 1L)
      val inserted = await {
        testContext.run {
          product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)).returningGenerated(_.id)
        }
      }
      val returnedProduct = await(testContext.run(productById(lift(inserted)))).head
      returnedProduct.description mustEqual "test1"
      returnedProduct.sku mustEqual 1L
      returnedProduct.id mustEqual inserted
    }

    "Single insert with free variable and explicit quotation" in {
      val prd = Product(0L, "test2", 2L)
      val q1 = quote {
        product.insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)).returningGenerated(_.id)
      }
      val inserted = await(testContext.run(q1))
      val returnedProduct = await(testContext.run(productById(lift(inserted)))).head
      returnedProduct.description mustEqual "test2"
      returnedProduct.sku mustEqual 2L
      returnedProduct.id mustEqual inserted
    }

    "Single insert with value class" in {
      case class Product(id: Id, description: String, sku: Long)
      val prd = Product(Id(0L), "test2", 2L)
      val q1 = quote {
        query[Product].insert(_.sku -> lift(prd.sku), _.description -> lift(prd.description)).returningGenerated(_.id)
      }
      await(testContext.run(q1)) mustBe a[Id]
    }

    "Single product insert with a method quotation" in {
      val prd = Product(0L, "test3", 3L)
      val inserted = await(testContext.run(productInsert(lift(prd))))
      val returnedProduct = await(testContext.run(productById(lift(inserted)))).head
      returnedProduct.description mustEqual "test3"
      returnedProduct.sku mustEqual 3L
      returnedProduct.id mustEqual inserted
    }

    "supports casts from string to number" - {
      "toInt" in {
        case class Product(id: Long, description: String, sku: Int)
        val queried = await {
          testContext.run {
            query[Product].filter(_.sku == lift("1004").toInt)
          }
        }.head
        queried.sku mustEqual 1004L
      }
      "toLong" in {
        val queried = await {
          testContext.run {
            query[Product].filter(_.sku == lift("1004").toLong)
          }
        }.head
        queried.sku mustEqual 1004L
      }
    }
  }
}
