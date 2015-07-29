package io.getquill.lifting

import scala.reflect.macros.whitebox.Context
import io.getquill.Queryable
import io.getquill.ast._
import io.getquill.norm.BetaReduction
import scala.reflect.ClassTag
import io.getquill.util.TreeSubstitution

trait Parser extends TreeSubstitution {

  val c: Context
  import c.universe.{ Function => _, Expr => _, Ident => _, Constant => _, _ }

  case class Extractor[T](p: PartialFunction[Tree, T])(implicit t: ClassTag[T]) {
    def apply(tree: Tree) =
      unapply(tree).getOrElse {
        c.abort(c.enclosingPosition, s"Tree '$tree' can't be parsed to '${t.runtimeClass.getSimpleName}'")
      }
    def unapply(tree: Tree) = p.lift(tree)
  }

  val query: Extractor[Query] = Extractor[Query] {
    case q"((..$params) => $body).apply(..$actuals)" =>
      query(substituteTree(body, params, actuals))
    case q"$pack.from[${ t: Type }]" =>
      Table(t.typeSymbol.name.decodedName.toString)
    case q"$source.filter(($alias) => $body)" =>
      Filter(query(source), ident(alias), expr(body))
    case q"$source.withFilter(($alias) => $body)" if (alias.name.toString.contains("ifrefutable")) =>
      query(source)
    case q"$source.withFilter(($alias) => $body)" =>
      Filter(query(source), ident(alias), expr(body))
    case q"$source.map[$t](($alias) => $body)" =>
      Map(query(source), ident(alias), expr(body))
    case q"$source.flatMap[$t](($alias) => $matchAlias match { case (..$a) => $body })" if (alias == matchAlias) =>
      val aliases =
        a.map {
          case Bind(name, _) =>
            Ident(name.decodedName.toString)
        }
      val reduction =
        for ((a, i) <- aliases.zipWithIndex) yield {
          a -> Property(expr(alias), s"_${i + 1}")
        }
      FlatMap(query(source), ident(alias), BetaReduction(query(body))(reduction.toMap))
    case q"$source.flatMap[$t](($alias) => $body)" =>
      FlatMap(query(source), ident(alias), query(body))
  }

  val expr: Extractor[Expr] = Extractor[Expr] {
    case q"((..$params) => $body).apply(..$actuals)" =>
      expr(substituteTree(body, params, actuals))
    case q"$a - $b" =>
      Subtract(expr(a), expr(b))
    case q"$a + $b" =>
      Add(expr(a), expr(b))
    case q"$a == $b" =>
      Equals(expr(a), expr(b))
    case q"$a && $b" =>
      And(expr(a), expr(b))
    case q"$a >= $b" =>
      GreaterThanOrEqual(expr(a), expr(b))
    case q"$a > $b" =>
      GreaterThan(expr(a), expr(b))
    case q"$a <= $b" =>
      LessThanOrEqual(expr(a), expr(b))
    case q"$a < $b" =>
      LessThan(expr(a), expr(b))
    case q"$a / $b" =>
      Division(expr(a), expr(b))
    case q"$a % $b" =>
      Remainder(expr(a), expr(b))

    case `ref`(ref) =>
      ref
  }

  val ref: Extractor[Ref] = Extractor[Ref] {
    case `value`(value) =>
      value
    case `ident`(ident) =>
      ident
    case q"$e.$property" =>
      Property(expr(e), property.decodedName.toString)
  }

  val value: Extractor[Ref] = Extractor[Ref] {
    case q"null" =>
      NullValue
    case Literal(c.universe.Constant(v)) =>
      Constant(v)
    case q"((..$v))" if (v.size > 1) =>
      Tuple(v.map(expr(_)))
  }

  val ident: Extractor[Ident] = Extractor[Ident] {
    case t: ValDef =>
      Ident(t.name.decodedName.toString)
    case c.universe.Ident(TermName(name)) =>
      Ident(name)
    case q"$i: $typ" =>
      ident(i)
    case c.universe.Bind(TermName(name), c.universe.Ident(termNames.WILDCARD)) =>
      Ident(name)
  }
}
