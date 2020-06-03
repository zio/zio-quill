package io.getquill.context.sql

import io.getquill.Ord

trait QueryResultTypeSpec extends ProductSpec {

  import context._

  val deleteAll = quote(query[Product].delete)
  val selectAll = quote(query[Product])
  val map = quote(query[Product].map(_.id))
  val filter = quote(query[Product].filter(_ => true))
  val withFilter = quote(query[Product].withFilter(_ => true))
  val sortBy = quote(query[Product].sortBy(_.id)(Ord.asc))
  val take = quote(query[Product].take(10))
  val drop = quote(query[Product].drop(1))
  val `++` = quote(query[Product] ++ query[Product])
  val unionAll = quote(query[Product].unionAll(query[Product]))
  val union = quote(query[Product].union(query[Product]))

  val minExists = quote(query[Product].map(_.sku).min)
  val minNonExists = quote(query[Product].filter(_.id > 1000).map(_.sku).min)
  val maxExists = quote(query[Product].map(_.sku).max)
  val maxNonExists = quote(query[Product].filter(_.id > 1000).map(_.sku).max)
  val avgExists = quote(query[Product].map(_.sku).avg)
  val avgNonExists = quote(query[Product].filter(_.id > 1000).map(_.sku).avg)
  val productSize = quote(query[Product].size)
  val parametrizedSize = quote { (id: Long) =>
    query[Product].filter(_.id == id).size
  }

  val join = quote(query[Product].join(query[Product]).on(_.id == _.id))

  val nonEmpty = quote(query[Product].nonEmpty)
  val isEmpty = quote(query[Product].isEmpty)
  val distinct = quote(query[Product].map(_.id).distinct)

}
