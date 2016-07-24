package io.getquill.context.sql

import io.getquill.Spec

trait ProductSpec extends Spec {

  val context: SqlContext[_, _, _, _]

  import context._

  case class Product(id: Long, description: String, sku: Long)

  val product = quote {
    query[Product]
  }

  val productInsert = quote {
    query[Product].insert.returning(_.id)
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
    product.insert(_.id -> 0, _.description -> "Window", _.sku -> 1004L).returning(_.id)
  }

  def productInsert(prd: Product) = quote {
    product.insert(_.description -> lift(prd.description), _.sku -> lift(prd.sku)).returning(_.id)
  }

}
