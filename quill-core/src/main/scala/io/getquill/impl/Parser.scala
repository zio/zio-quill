package io.getquill.impl

import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Add
import io.getquill.ast.And
import io.getquill.ast.Constant
import io.getquill.ast.Division
import io.getquill.ast.Equals
import io.getquill.ast.Expr
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.GreaterThan
import io.getquill.ast.GreaterThanOrEqual
import io.getquill.ast.Ident
import io.getquill.ast.LessThan
import io.getquill.ast.LessThanOrEqual
import io.getquill.ast.Map
import io.getquill.ast.NullValue
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.Ref
import io.getquill.ast.Remainder
import io.getquill.ast.Subtract
import io.getquill.ast.Table
import io.getquill.ast.Tuple
import io.getquill.norm.BetaReduction
import io.getquill.util.TreeSubstitution
import io.getquill.util.Messages

trait Parser extends TreeSubstitution with Quotation with Messages {

  val c: Context
  import c.universe.{ Expr => _, Ident => _, Constant => _, _ }

  case class Extractor[T](p: PartialFunction[Tree, T])(implicit t: ClassTag[T]) {

    def apply(tree: Tree) =
      unapply(tree).getOrElse {
        fail(s"Tree '$tree' can't be parsed to '${t.runtimeClass.getSimpleName}'")
      }

    def unapply(tree: Tree): Option[T] =
      tree match {
        case tree if (tree.tpe.erasure =:= c.weakTypeOf[Quoted[_]]) =>
          unapply(unquoteTree(tree))
        case q"((..$params) => $body).apply(..$actuals)" =>
          unapply(substituteTree(body, params, actuals))
        case other =>
          p.lift(tree)
      }
  }

  val query: Extractor[Query] = Extractor[Query] {

    case q"io.getquill.`package`.from[${ t: Type }]" =>
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

    case q"$a - $b"  => Subtract(expr(a), expr(b))
    case q"$a + $b"  => Add(expr(a), expr(b))
    case q"$a == $b" => Equals(expr(a), expr(b))
    case q"$a && $b" => And(expr(a), expr(b))
    case q"$a >= $b" => GreaterThanOrEqual(expr(a), expr(b))
    case q"$a > $b"  => GreaterThan(expr(a), expr(b))
    case q"$a <= $b" => LessThanOrEqual(expr(a), expr(b))
    case q"$a < $b"  => LessThan(expr(a), expr(b))
    case q"$a / $b"  => Division(expr(a), expr(b))
    case q"$a % $b"  => Remainder(expr(a), expr(b))

    case `ref`(ref)  => ref
  }

  val ref: Extractor[Ref] = Extractor[Ref] {
    case `value`(value)  => value
    case `ident`(ident)  => ident
    case q"$e.$property" => Property(expr(e), property.decodedName.toString)
  }

  val value: Extractor[Ref] = Extractor[Ref] {
    case q"null"                         => NullValue
    case Literal(c.universe.Constant(v)) => Constant(v)
    case q"((..$v))" if (v.size > 1)     => Tuple(v.map(expr(_)))
  }

  val ident: Extractor[Ident] = Extractor[Ident] {
    case t: ValDef                        => Ident(t.name.decodedName.toString)
    case c.universe.Ident(TermName(name)) => Ident(name)
    case q"$i: $typ"                      => ident(i)
    case c.universe.Bind(TermName(name), c.universe.Ident(termNames.WILDCARD)) =>
      Ident(name)
  }
}
