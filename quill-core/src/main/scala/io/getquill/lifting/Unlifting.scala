package io.getquill.lifting

import scala.reflect.macros.whitebox.Context
import io.getquill.Queryable
import io.getquill.ast._
import io.getquill.norm.BetaReduction
import io.getquill.attach.TypeAttachment

trait Unlifting extends TypeAttachment {
  val c: Context
  import c.universe.{ Function => _, Expr => _, Ident => _, Constant => _, _ }

  implicit val queryUnlift: Unliftable[Query] = Unliftable[Query] {
    case q"io.getquill.ast.Table.apply(${ name: String })" =>
      Table(name)
    case q"io.getquill.ast.Map.apply(${ source: Query }, ${ alias: Ident }, ${ body: Expr })" =>
      Map(source, alias, body)
    case q"io.getquill.ast.FlatMap.apply(${ source: Query }, ${ alias: Ident }, ${ body: Query })" =>
      FlatMap(source, alias, body)
    case q"io.getquill.ast.Filter.apply(${ source: Query }, ${ alias: Ident }, ${ body: Expr })" =>
      Filter(source, alias, body)
  }

  private def query(t: Tree) =
    t match {
      case q"${ query: Query }" => query
    }

  implicit val exprUnlift: Unliftable[Expr] = Unliftable[Expr] {

    case q"io.getquill.ast.Subtract.apply(${ a: Expr }, ${ b: Expr })" =>
      Subtract(a, b)
    case q"${ a: Expr } - ${ b: Expr }" =>
      Subtract(a, b)

    case q"io.getquill.ast.Add.apply(${ a: Expr }, ${ b: Expr })" =>
      Add(a, b)
    case q"${ a: Expr } + ${ b: Expr }" =>
      Add(a, b)

    case q"io.getquill.ast.FunctionApply.apply(${ ident: Ident }, ${ value: Expr })" =>
      FunctionApply(ident, value)
    case q"${ ident: Ident }.apply(${ value: Expr })" =>
      FunctionApply(ident, value)

    case q"io.getquill.ast.FunctionDef.apply(${ ident: Ident }, ${ value: Expr })" =>
      FunctionDef(ident, value)
    case q"(${ ident: Ident }) => ${ value: Expr }" =>
      FunctionDef(ident, value)

    case q"io.getquill.ast.Equals.apply(${ a: Expr }, ${ b: Expr })" =>
      Equals(a, b)
    case q"${ a: Expr } == ${ b: Expr }" =>
      Equals(a, b)

    case q"io.getquill.ast.And.apply(${ a: Expr }, ${ b: Expr })" =>
      And(a, b)
    case q"${ a: Expr } && ${ b: Expr }" =>
      And(a, b)

    case q"io.getquill.ast.GreaterThanOrEqual.apply(${ a: Expr }, ${ b: Expr })" =>
      GreaterThanOrEqual(a, b)
    case q"${ a: Expr } >= ${ b: Expr }" =>
      GreaterThanOrEqual(a, b)

    case q"io.getquill.ast.GreaterThan.apply(${ a: Expr }, ${ b: Expr })" =>
      GreaterThan(a, b)
    case q"${ a: Expr } > ${ b: Expr }" =>
      GreaterThan(a, b)

    case q"io.getquill.ast.LessThanOrEqual.apply(${ a: Expr }, ${ b: Expr })" =>
      LessThanOrEqual(a, b)
    case q"${ a: Expr } <= ${ b: Expr }" =>
      LessThanOrEqual(a, b)

    case q"io.getquill.ast.LessThan.apply(${ a: Expr }, ${ b: Expr })" =>
      LessThan(a, b)
    case q"${ a: Expr } < ${ b: Expr }" =>
      LessThan(a, b)

    case q"io.getquill.ast.Division.apply(${ a: Expr }, ${ b: Expr })" =>
      Division(a, b)
    case q"${ a: Expr } / ${ b: Expr }" =>
      Division(a, b)

    case q"io.getquill.ast.Remainder.apply(${ a: Expr }, ${ b: Expr })" =>
      Remainder(a, b)
    case q"${ a: Expr } % ${ b: Expr }" =>
      Remainder(a, b)

    case q"${ ref: Ref }" =>
      ref
  }

  implicit val refUnlift: Unliftable[Ref] = Unliftable[Ref] {
    case q"io.getquill.ast.Property.apply(${ expr: Expr }, ${ name: String })" =>
      Property(expr, name)
    case q"${ value: Value }" =>
      value
    case q"${ ident: Ident }" =>
      ident
    case q"${ expr: Expr }.$property" =>
      Property(expr, property.decodedName.toString)
  }

  implicit val valueUnlift: Unliftable[Value] = Unliftable[Value] {
    case q"io.getquill.ast.Tuple.apply(immutable.this.List.apply[$t](..$v))" =>
      val values =
        v.map {
          case q"${ expr: Expr }" => expr
        }
      Tuple(values)
    case q"io.getquill.ast.Constant.apply(${ Literal(c.universe.Constant(v)) })" =>
      Constant(v)
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
    case q"io.getquill.ast.Ident.apply(${ name: String })" =>
      Ident(name)
    case t: ValDef =>
      Ident(t.name.decodedName.toString)
    case c.universe.Ident(TermName(name)) =>
      Ident(name)
    case q"${ name: Ident }: $typ" =>
      name
    case c.universe.Bind(TermName(name), c.universe.Ident(termNames.WILDCARD)) =>
      Ident(name)
  }

  implicit val parametrizedUnlift: Unliftable[Parametrized] = Unliftable[Parametrized] {
    case q"${ p: ParametrizedQuery }" => p
    case q"${ p: ParametrizedExpr }"  => p
  }

  implicit val parametrizedQueryUnlift: Unliftable[ParametrizedQuery] = Unliftable[ParametrizedQuery] {
    case q"io.getquill.ast.ParametrizedQuery.apply(immutable.this.List.apply[$t](..$p), ${ query: Query })" =>
      val params =
        p.map {
          case q"${ ident: Ident }" => ident
        }
      ParametrizedQuery(params, query)
  }

  implicit val parametrizedExprUnlift: Unliftable[ParametrizedExpr] = Unliftable[ParametrizedExpr] {
    case q"io.getquill.ast.ParametrizedExpr.apply(immutable.this.List.apply[$t](..$p), ${ expr: Expr })" =>
      val params =
        p.map {
          case q"${ ident: Ident }" => ident
        }
      ParametrizedExpr(params, expr)
  }
}
