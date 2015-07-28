package io.getquill.lifting

import scala.reflect.macros.whitebox.Context
import io.getquill.Queryable
import io.getquill.ast._
import io.getquill.norm.BetaReduction

trait Unlifting {
  val c: Context
  import c.universe.{ Function => _, Expr => _, Ident => _, Constant => _, _ }

  implicit val queryUnlift: Unliftable[Query] = Unliftable[Query] {

    case q"$pack.from[${ t: Type }]" =>
      Table(t.typeSymbol.name.decodedName.toString)
    case q"$source.filter((${ alias: Ident }) => ${ body: Expr })" =>
      Filter(query(source), alias, body)
    case q"$source.withFilter((${ alias: Ident }) => $body)" if (alias.name.toString.contains("ifrefutable")) =>
      query(source)
    case q"$source.withFilter((${ alias: Ident }) => ${ body: Expr })" =>
      Filter(query(source), alias, body)
    case q"$source.map[$t]((${ alias: Ident }) => ${ body: Expr })" =>
      Map(query(source), alias, body)
    case q"$source.flatMap[$t]((${ alias: Ident }) => ${ matchAlias: Ident } match { case (..$a) => ${ body: Query } })" if (alias == matchAlias) =>
      val aliases =
        a.map {
          case Bind(name, _) =>
            Ident(name.decodedName.toString)
        }
      val reduction =
        for ((a, i) <- aliases.zipWithIndex) yield {
          a -> Property(alias, s"_${i + 1}")
        }
      FlatMap(query(source), alias, BetaReduction(body)(reduction.toMap))
    case q"$source.flatMap[$t]((${ alias: Ident }) => ${ body: Query })" =>
      FlatMap(query(source), alias, body)
  }

  private def query(t: Tree) =
    t match {
      case q"${ query: Query }" => query
    }

  implicit val exprUnlift: Unliftable[Expr] = Unliftable[Expr] {
    case q"${ a: Expr } - ${ b: Expr }" =>
      Subtract(a, b)
    case q"${ a: Expr } + ${ b: Expr }" =>
      Add(a, b)
    case q"${ ident: Ident }.apply(${ value: Expr })" =>
      FunctionApply(ident, value)
    case q"(${ ident: Ident }) => ${ value: Expr }" =>
      FunctionDef(ident, value)
    case q"${ a: Expr } == ${ b: Expr }" =>
      Equals(a, b)
    case q"${ a: Expr } && ${ b: Expr }" =>
      And(a, b)
    case q"${ a: Expr } >= ${ b: Expr }" =>
      GreaterThanOrEqual(a, b)
    case q"${ a: Expr } > ${ b: Expr }" =>
      GreaterThan(a, b)
    case q"${ a: Expr } <= ${ b: Expr }" =>
      LessThanOrEqual(a, b)
    case q"${ a: Expr } < ${ b: Expr }" =>
      LessThan(a, b)
    case q"${ a: Expr } / ${ b: Expr }" =>
      Division(a, b)
    case q"${ a: Expr } % ${ b: Expr }" =>
      Remainder(a, b)

    case q"${ ref: Ref }" =>
      ref
  }

  implicit val refUnlift: Unliftable[Ref] = Unliftable[Ref] {
    case q"${ value: Value }" =>
      value
    case q"${ ident: Ident }" =>
      ident
    case q"${ expr: Expr }.$property" =>
      Property(expr, property.decodedName.toString)
  }

  implicit val valueUnlift: Unliftable[Value] = Unliftable[Value] {
    case q"io.getquill.ast.NullValue" =>
      NullValue
    case q"null" =>
      NullValue
    case Literal(c.universe.Constant(v)) =>
      Constant(v)
    case q"((..$v))" if (v.size > 1) =>
      val values =
        v.map {
          case q"${ expr: Expr }" => expr
        }
      Tuple(values)
  }

  implicit val identUnift: Unliftable[Ident] = Unliftable[Ident] {
    case t: ValDef =>
      Ident(t.name.decodedName.toString)
    case c.universe.Ident(TermName(name)) =>
      Ident(name)
    case q"${ name: Ident }: $typ" =>
      name
    case c.universe.Bind(TermName(name), c.universe.Ident(termNames.WILDCARD)) =>
      Ident(name)
  }
}
