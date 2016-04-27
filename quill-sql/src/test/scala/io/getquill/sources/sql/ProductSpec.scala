package io.getquill.sources.sql

import io.getquill._

trait ProductSpec extends Spec {

  case class Product(id: Long, description: String, sku: Long)

  val product = quote {
    query[Product](_.generated(_.id))
  }

  val productInsert = quote {
    query[Product](_.generated(_.id)).insert
  }

  def productById(id: Long) = quote {
    product.filter(_.id == lift(id))
  }

  val productEntries = List(
    Product(0L, "Notebook", 1001L),
    Product(0L, "Soap", 1002L),
    Product(0L, "Pencil", 1003L)
  )

  val productSingleInsert = quote {
    product.insert(_.id -> 0, _.description -> "Window", _.sku -> 1004L)
  }

  def productInsert(prd: Product) = quote {
    product.insert(_.description -> lift(prd.description), _.sku -> lift(prd.sku))
  }

}
