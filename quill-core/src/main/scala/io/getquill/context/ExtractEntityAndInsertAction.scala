package io.getquill.context

import io.getquill.ast._

case class EntityAndInsertAction(entity: Option[Entity], insert: Option[Insert])

case class ExtractEntityAndInsertAction(state: EntityAndInsertAction) extends StatefulTransformer[EntityAndInsertAction] {

  override def apply(e: Query): (Query, StatefulTransformer[EntityAndInsertAction]) =
    e match {
      case e: Entity =>
        (e, ExtractEntityAndInsertAction(state.copy(entity = Some(e))))
      case other => super.apply(other)
    }

  override def apply(e: Action): (Action, StatefulTransformer[EntityAndInsertAction]) =
    e match {
      case e @ Insert(a) =>
        val (at, att) = apply(a)
        (Insert(at), ExtractEntityAndInsertAction(att.state.copy(insert = Some(e))))
      case other => super.apply(other)
    }
}

object ExtractEntityAndInsertAction {
  def apply(ast: Ast) = new ExtractEntityAndInsertAction(EntityAndInsertAction(None, None))(ast) match {
    case (_, s) => (s.state.entity, s.state.insert)
  }
}
