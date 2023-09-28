package io.getquill.context.cassandra

import io.getquill.TestEntities
import io.getquill.{ EntityQuery, Quoted }

trait CassandraTestEntities extends TestEntities {
  this: CassandraContext[_] =>

  case class MapFrozen(id: Map[Int, Boolean])
  val mapFroz: Quoted[EntityQuery[MapFrozen]] = quote(query[MapFrozen])

  case class SetFrozen(id: Set[Int])
  val setFroz: Quoted[EntityQuery[SetFrozen]] = quote(query[SetFrozen])

  case class ListFrozen(id: List[Int])
  val listFroz: Quoted[EntityQuery[ListFrozen]] = quote(query[ListFrozen])
}
