package io.getquill.sources.sql.norm

import io.getquill.ast._

case class MergeGeneratedWithEntity(state: Option[String]) extends StatefulTransformer[Option[String]] {

  override def apply(e: Query): (Query, StatefulTransformer[Option[String]]) =
    e match {
      case Generated(a: Entity, _, c) =>
        val (at, att) = apply(a)
        (at, MergeGeneratedWithEntity(singleProperty(c)))
      case other => super.apply(other)
    }

  private def singleProperty(ast: Ast) = ast match {
    case Property(Ident(a), b) => Some(b)
    case _                     => None
  }

}

case class SetGeneratedEntity(generated: Option[String]) extends StatelessTransformer {
  override def apply(e: Query): Query =
    e match {
      case e: Entity => e.copy(generated = generated)
      case other => super.apply(other)
    }
}

object MergeGeneratedWithEntity {
  def apply(q: Ast) =
    new MergeGeneratedWithEntity(None)(q) match {
      case (q, s) =>
        SetGeneratedEntity(s.state)(q)
    }
}
