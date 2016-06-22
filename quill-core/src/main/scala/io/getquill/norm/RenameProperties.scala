package io.getquill.norm

import io.getquill.ast.Ast
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Join
import io.getquill.ast.Map
import io.getquill.ast.OptionOperation
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.StatefulTransformer

case class RenameProperties(state: collection.Map[Ident, collection.Map[String, String]])
  extends StatefulTransformer[collection.Map[Ident, collection.Map[String, String]]] {

  override def apply(q: Query): (Query, StatefulTransformer[collection.Map[Ident, collection.Map[String, String]]]) =
    q match {
      case FlatMap(q: Entity, x, p)   => apply(q, x, p)(FlatMap)

      case Map(q: Entity, x, p)       => apply(q, x, p)(Map)

      case Filter(q: Entity, x, p)    => apply(q, x, p)(Filter)

      case SortBy(q: Entity, x, p, o) => apply(q, x, p)(SortBy(_, _, _, o))

      case q @ Join(t, a: Entity, b: Entity, iA, iB, o) =>
        val ((_, _, or), ort) = apply(a, iA, o)((_, _, _))
        val ((_, _, orr), orrt) = apply(b, iB, or)((_, _, _))
        (Join(t, a, b, iA, iB, orr), RenameProperties(state ++ ort.state ++ orrt.state))

      case q @ Join(t, a: Entity, b, iA, iB, o) =>
        val ((_, _, or), ort) = apply(a, iA, o)((_, _, _))
        val (br, brt) = apply(b)
        (Join(t, a, br, iA, iB, or), RenameProperties(state ++ ort.state ++ brt.state))

      case q @ Join(t, a: Query, b: Entity, iA, iB, o) =>
        val (ar, art) = apply(a)
        val ((_, _, or), ort) = apply(b, iB, o)((_, _, _))
        (Join(t, ar, b, iA, iB, or), RenameProperties(state ++ art.state ++ ort.state))

      case other => super.apply(q)
    }

  override def apply(q: Ast): (Ast, StatefulTransformer[collection.Map[Ident, collection.Map[String, String]]]) =
    q match {
      case Property(ident: Ident, prop) if (state.contains(ident)) =>
        (Property(ident, state(ident).getOrElse(prop, prop)), this)
      case OptionOperation(t, q: Ident, x, p) if (state.contains(q)) =>
        val (pr, _) = RenameProperties(state + (x -> state(q)))(p)
        (OptionOperation(t, q, x, pr), this)
      case other =>
        super.apply(other)
    }

  private def apply[T](q: Entity, x: Ident, p: Ast)(f: (Ast, Ident, Ast) => T): (T, StatefulTransformer[collection.Map[Ident, collection.Map[String, String]]]) = {
    val (pr, prt) = RenameProperties(state + (x -> q.properties.map(p => p.property -> p.alias).toMap))(p)
    (f(q, x, pr), prt)
  }
}

object RenameProperties {
  def apply(q: Ast) =
    new RenameProperties(collection.Map())(q) match {
      case (q, _) => q
    }
}
