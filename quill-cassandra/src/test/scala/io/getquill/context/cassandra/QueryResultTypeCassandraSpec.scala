package io.getquill.context.cassandra

import io.getquill.context.cassandra.encoding.Encoders
import io.getquill.context.cassandra.encoding.Decoders
import io.getquill.Ord
import io.getquill.base.Spec
import io.getquill.{Delete, EntityQuery, Query, Quoted}

trait QueryResultTypeCassandraSpec extends Spec {

  val context: CassandraContext[_] with Encoders with Decoders
  import context._

  case class OrderTestEntity(id: Int, i: Int)

  val entries: List[OrderTestEntity] = List(
    OrderTestEntity(1, 1),
    OrderTestEntity(2, 2),
    OrderTestEntity(3, 3)
  )

  val insert                                           = quote((e: OrderTestEntity) => query[OrderTestEntity].insertValue(e))
  val deleteAll: Quoted[Delete[OrderTestEntity]]       = quote(query[OrderTestEntity].delete)
  val selectAll: Quoted[EntityQuery[OrderTestEntity]]  = quote(query[OrderTestEntity])
  val map: Quoted[EntityQuery[Int]]                    = quote(query[OrderTestEntity].map(_.id))
  val filter: Quoted[EntityQuery[OrderTestEntity]]     = quote(query[OrderTestEntity].filter(_.id == 1))
  val withFilter: Quoted[EntityQuery[OrderTestEntity]] = quote(query[OrderTestEntity].withFilter(_.id == 1))
  val sortBy: Quoted[Query[OrderTestEntity]]           = quote(query[OrderTestEntity].filter(_.id == 1).sortBy(_.i)(Ord.asc))
  val take: Quoted[Query[OrderTestEntity]]             = quote(query[OrderTestEntity].take(10))
  val entitySize: Quoted[Long]                         = quote(query[OrderTestEntity].size)
  val parametrizedSize: Quoted[Int => Long] = quote { (id: Int) =>
    query[OrderTestEntity].filter(_.id == id).size
  }
  val distinct: Quoted[Query[Int]] = quote(query[OrderTestEntity].map(_.id).distinct)
}
