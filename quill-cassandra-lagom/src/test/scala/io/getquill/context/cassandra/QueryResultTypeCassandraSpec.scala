package io.getquill.context.cassandra

import io.getquill.Spec
import io.getquill.context.cassandra.encoding.Encoders
import io.getquill.context.cassandra.encoding.Decoders
import io.getquill.Ord

trait QueryResultTypeCassandraSpec extends Spec {

  val context: CassandraContext[_] with Encoders with Decoders
  import context._

  case class OrderTestEntity(id: Int, i: Int)

  val entries = List(
    OrderTestEntity(1, 1),
    OrderTestEntity(2, 2),
    OrderTestEntity(3, 3)
  )

  val insert = quote((e: OrderTestEntity) => query[OrderTestEntity].insert(e))
  val deleteAll = quote(query[OrderTestEntity].delete)
  val selectAll = quote(query[OrderTestEntity])
  val map = quote(query[OrderTestEntity].map(_.id))
  val filter = quote(query[OrderTestEntity].filter(_.id == 1))
  val withFilter = quote(query[OrderTestEntity].withFilter(_.id == 1))
  val sortBy = quote(query[OrderTestEntity].filter(_.id == 1).sortBy(_.i)(Ord.asc))
  val take = quote(query[OrderTestEntity].take(10))
  val entitySize = quote(query[OrderTestEntity].size)
  val parametrizedSize = quote { (id: Int) =>
    query[OrderTestEntity].filter(_.id == id).size
  }
  val distinct = quote(query[OrderTestEntity].map(_.id).distinct)
}
