package io.getquill.context.cassandra

import io.getquill.TestEntities

trait CassandraTestEntities extends TestEntities {
  this: CassandraContext[_] =>

  case class MapFrozen(id: Map[Int, Boolean])
  val mapFroz = quote(query[MapFrozen])

  case class SetFrozen(id: Set[Int])
  val setFroz = quote(query[SetFrozen])

  case class ListFrozen(id: List[Int])
  val listFroz = quote(query[ListFrozen])
}
