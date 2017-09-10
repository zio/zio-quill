package io.getquill.norm

import io.getquill.ast._

object RenameProperties extends StatelessTransformer {

  override def apply(q: Query): Query =
    applySchema(q) match {
      case (q, schema) => q
    }

  override def apply(q: Action): Action =
    applySchema(q) match {
      case (q, schema) => q
    }

  private def applySchema(q: Ast): (Ast, Ast) =
    q match {
      case q: Action => applySchema(q)
      case q: Query  => applySchema(q)
      case q =>
        CollectAst.byType[Entity](q) match {
          case schema :: Nil => (q, schema)
          case _             => (q, Tuple(List.empty))
        }
    }

  private def applySchema(q: Action): (Action, Ast) =
    q match {
      case Insert(q, assignments) => applySchema(q, assignments, Insert)
      case Update(q, assignments) => applySchema(q, assignments, Update)
      case Delete(q) =>
        applySchema(q) match {
          case (q, schema) => (Delete(q), schema)
        }
      case Returning(action, alias, body) =>
        applySchema(action) match {
          case (action, schema) =>
            val replace = replacements(alias, schema)
            val bodyr = BetaReduction(body, replace: _*)
            (Returning(action, alias, bodyr), schema)
        }
      case Foreach(q, alias, body) =>
        applySchema(q) match {
          case (q, schema) =>
            val replace = replacements(alias, schema)
            val bodyr = BetaReduction(body, replace: _*)
            (Foreach(q, alias, bodyr), schema)
        }
    }

  private def applySchema(q: Ast, a: List[Assignment], f: (Ast, List[Assignment]) => Action): (Action, Ast) =
    applySchema(q) match {
      case (q, schema) =>
        val ar = a.map {
          case Assignment(alias, prop, value) =>
            val replace = replacements(alias, schema)
            val propr = BetaReduction(prop, replace: _*)
            val valuer = BetaReduction(value, replace: _*)
            Assignment(alias, propr, valuer)
        }
        (f(q, ar), schema)
    }

  private def applySchema(q: Query): (Query, Ast) =
    q match {
      case e: Entity          => (e, e)
      case Map(q, x, p)       => applySchema(q, x, p, Map)
      case Filter(q, x, p)    => applySchema(q, x, p, Filter)
      case SortBy(q, x, p, o) => applySchema(q, x, p, SortBy(_, _, _, o))
      case GroupBy(q, x, p)   => applySchema(q, x, p, GroupBy)
      case Aggregation(op, q) => applySchema(q, Aggregation(op, _))
      case Take(q, n)         => applySchema(q, Take(_, n))
      case Drop(q, n)         => applySchema(q, Drop(_, n))
      case Distinct(q)        => applySchema(q, Distinct)

      case FlatMap(q, x, p) =>
        applySchema(q, x, p, FlatMap) match {
          case (FlatMap(q, x, p: Query), oldSchema) =>
            val (pr, newSchema) = applySchema(p)
            (FlatMap(q, x, pr), newSchema)
          case (flatMap, oldSchema) =>
            (flatMap, Tuple(List.empty))
        }

      case Join(typ, a, b, iA, iB, on) =>
        (applySchema(a), applySchema(b)) match {
          case ((a, schemaA), (b, schemaB)) =>
            val replaceA = replacements(iA, schemaA)
            val replaceB = replacements(iB, schemaB)
            val onr = BetaReduction(on, replaceA ++ replaceB: _*)
            (Join(typ, a, b, iA, iB, onr), Tuple(List(schemaA, schemaB)))
        }

      case FlatJoin(typ, a, iA, on) =>
        applySchema(a) match {
          case (a, schemaA) =>
            val replaceA = replacements(iA, schemaA)
            val onr = BetaReduction(on, replaceA: _*)
            (FlatJoin(typ, a, iA, onr), schemaA)
        }

      case Nested(q) =>
        val (qr, schema) = applySchema(q)
        (Nested(qr), schema)

      case _: Union | _: UnionAll =>
        (q, Tuple(List.empty))
    }

  private def applySchema(ast: Ast, f: Ast => Query): (Query, Ast) =
    applySchema(ast) match {
      case (ast, schema) =>
        (f(ast), schema)
    }

  private def applySchema[T](q: Ast, x: Ident, p: Ast, f: (Ast, Ident, Ast) => T): (T, Ast) =
    applySchema(q) match {
      case (q, schema) =>
        val replace = replacements(x, schema)
        val pr = BetaReduction(p, replace: _*)
        (f(q, x, pr), schema)
    }

  private def replacements(base: Ast, schema: Ast): List[(Ast, Ast)] =
    (schema: @unchecked) match {
      case Entity(entity, properties) =>
        properties.map {
          case PropertyAlias(path, alias) =>
            def apply(base: Ast, path: List[String]): Ast =
              path match {
                case Nil          => base
                case head :: tail => apply(Property(base, head), tail)
              }
            apply(base, path) -> Property(base, alias)
        }
      case Tuple(values) =>
        values.zipWithIndex.map {
          case (value, idx) =>
            replacements(Property(base, s"_${idx + 1}"), value)
        }.flatten
    }
}