package io.getquill.context.spark.norm

import io.getquill.ast.{ Entity, Ident, Property }

sealed trait SearchState {
  def aliases: Seq[EntityId] = Seq()

  def merge(other: SearchState): SearchState = {
    (this, other) match {
      case (NotFound, NotFound)                   => NotFound
      case (NotFound, b: FoundEntityIds)          => b
      case (a: FoundEntityIds, NotFound)          => a
      case (a: FoundEntityIds, b: FoundEntityIds) => FoundEntityIds(a.aliases ++ b.aliases)
    }
  }

  def contains(id: String): Boolean = aliases.map(a => (a.id, a)).toMap.contains(id)
  def apply(id: String): EntityId = aliases.map(a => (a.id, a)).toMap.apply(id)
}

object NotFound extends SearchState
case class EntityId(id: String, fields: List[Property])
case class FoundEntityIds(override val aliases: Seq[EntityId]) extends SearchState

object FoundEntityIds {
  def apply(id: String, e: Entity) =
    new FoundEntityIds(Seq(EntityId(id, e)))

  def apply(id: String, propertyList: List[String]) =
    new FoundEntityIds(Seq(EntityId(id, propertyList.map(p => Property(Ident(id), p)))))
}
object EntityId {
  def apply(id: String, e: Entity) = new EntityId(id, e.properties.map(pa => Property(Ident(id), pa.path.mkString("."))))
}