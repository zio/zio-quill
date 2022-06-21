package io.getquill.sql.norm

import io.getquill.ast._

object ExpandJoin extends StatelessTransformer {

  object ExcludedFromNesting {
    def unapply(ast: Ast) =
      ast match {
        case _: Entity   => true
        case _: Infix    => true
        case _: FlatJoin => true
        case _: Nested   => true
        case _           => false
      }
  }

  override def apply(e: Query): Query = {
    e match {
      // Need to nest any map/flatMap clauses found in the `query` slot of a FlatMap or
      // Verifier will cause Found an `ON` table reference of a table that is not available when they are
      // found in this position.
      case fm: FlatMap =>
        super.apply(fm) match {
          case fm @ FlatMap(ExcludedFromNesting(), _, _) => fm
          case FlatMap(ent, alias, body) =>
            FlatMap(Nested(ent), alias, body)
          case other =>
            throw new IllegalArgumentException(s"Result of a flatMap reduction $fm needs to be a flatMap. It was $other.")
        }

      // Note that quats in iA, iB shuold not need to change since this is a just a re-wrapping
      case Join(typ, a, b, iA, iB, on) =>
        val a1 =
          apply(a) match {
            case v @ ExcludedFromNesting() => v
            case other                     => Nested(other)
          }
        val b1 = apply(b)
        FlatMap(a1, iA, Map(FlatJoin(typ, b1, iB, on), iB, Tuple(List(iA, iB))))

      case Filter(query: Join, alias, body)    => Filter(Nested(apply(query)), alias, body)
      case Map(query: Join, alias, body)       => Map(Nested(apply(query)), alias, body)
      case ConcatMap(query: Join, alias, body) => ConcatMap(Nested(apply(query)), alias, body)

      case _                                   => super.apply(e)
    }
  }
}
