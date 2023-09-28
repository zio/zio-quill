package io.getquill.context.sql.base

import io.getquill.Ord
import io.getquill.context.sql.ProductSpec
import io.getquill.{Delete, EntityQuery, Query, Quoted}

trait QueryResultTypeSpec extends ProductSpec {

  import context._

  val deleteAll: Quoted[Delete[Product]]       = quote(query[Product].delete)
  val selectAll: Quoted[EntityQuery[Product]]  = quote(query[Product])
  val map: Quoted[EntityQuery[Long]]           = quote(query[Product].map(_.id))
  val filter: Quoted[EntityQuery[Product]]     = quote(query[Product].filter(_ => true))
  val withFilter: Quoted[EntityQuery[Product]] = quote(query[Product].withFilter(_ => true))
  val sortBy: Quoted[Query[Product]]           = quote(query[Product].sortBy(_.id)(Ord.asc))
  val take: Quoted[Query[Product]]             = quote(query[Product].take(10))
  val drop: Quoted[Query[Product]]             = quote(query[Product].drop(1))
  val `++`                                     = quote(query[Product] ++ query[Product])
  val unionAll: Quoted[Query[Product]]         = quote(query[Product].unionAll(query[Product]))
  val union: Quoted[Query[Product]]            = quote(query[Product].union(query[Product]))

  val minExists: Quoted[Option[Long]]          = quote(query[Product].map(_.sku).min)
  val minNonExists: Quoted[Option[Long]]       = quote(query[Product].filter(_.id > 1000).map(_.sku).min)
  val maxExists: Quoted[Option[Long]]          = quote(query[Product].map(_.sku).max)
  val maxNonExists: Quoted[Option[Long]]       = quote(query[Product].filter(_.id > 1000).map(_.sku).max)
  val avgExists: Quoted[Option[BigDecimal]]    = quote(query[Product].map(_.sku).avg)
  val avgNonExists: Quoted[Option[BigDecimal]] = quote(query[Product].filter(_.id > 1000).map(_.sku).avg)
  val productSize: Quoted[Long]                = quote(query[Product].size)
  val parametrizedSize: Quoted[Long => Long] = quote { (id: Long) =>
    query[Product].filter(_.id == id).size
  }

  val join: Quoted[Query[(Product, Product)]] = quote(query[Product].join(query[Product]).on(_.id == _.id))

  val nonEmpty: Quoted[Boolean]     = quote(query[Product].nonEmpty)
  val isEmpty: Quoted[Boolean]      = quote(query[Product].isEmpty)
  val distinct: Quoted[Query[Long]] = quote(query[Product].map(_.id).distinct)

}
