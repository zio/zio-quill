package io.getquill.impl

import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Action
import io.getquill.ast.Assignment
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Constant
import io.getquill.ast.Delete
import io.getquill.ast.Expr
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Insert
import io.getquill.ast.Map
import io.getquill.ast.NullValue
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.Ref
import io.getquill.ast.Table
import io.getquill.ast.Tuple
import io.getquill.ast.Update
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages.RichContext
import io.getquill.util.SubstituteTrees

trait Parser extends Quotation {

  val c: Context
  import c.universe.{ Expr => _, Ident => _, Constant => _, _ }

  case class Extractor[T](p: PartialFunction[Tree, T])(implicit t: ClassTag[T]) {

    def apply(tree: Tree) =
      unapply(tree).getOrElse {
        c.fail(s"Tree '$tree' can't be parsed to '${t.runtimeClass.getSimpleName}'")
      }

    def unapply(tree: Tree): Option[T] =
      tree match {
        case q"((..$params) => $body).apply(..$actuals)" =>
          unapply(SubstituteTrees(c)(body, params, actuals))
        case q"io.getquill.`package`.unquote[$t]($quoted)" =>
          unapply(unquoteTree(quoted))
        case tree if (tree.tpe <:< c.weakTypeOf[Quoted[Any]] && !(tree.tpe <:< c.weakTypeOf[Null])) =>
          unapply(unquoteTree(tree))
        case other =>
          p.lift(tree)
      }
  }

  val actionExtractor: Extractor[Action] = Extractor[Action] {
    case q"$query.$method(..$assignments)" if (method.decodedName.toString == "update") =>
      Update(queryExtractor(query), assignments.map(assignmentExtractor(_)))
    case q"$query.insert(..$assignments)" =>
      Insert(queryExtractor(query), assignments.map(assignmentExtractor(_)))
    case q"$query.delete" =>
      Delete(queryExtractor(query))
  }

  val assignmentExtractor: Extractor[Assignment] = Extractor[Assignment] {
    case q"(($x) => scala.this.Predef.ArrowAssoc[$t]($expr).->[$v]($value))" =>
      Assignment(propertyExtractor(expr), exprExtractor(value))
  }

  val queryExtractor: Extractor[Query] = Extractor[Query] {

    case q"io.getquill.`package`.queryable[${ t: Type }]" =>
      Table(t.typeSymbol.name.decodedName.toString)

    case q"$source.filter(($alias) => $body)" =>
      Filter(queryExtractor(source), identExtractor(alias), exprExtractor(body))

    case q"$source.withFilter(($alias) => $body)" if (alias.name.toString.contains("ifrefutable")) =>
      queryExtractor(source)

    case q"$source.withFilter(($alias) => $body)" =>
      Filter(queryExtractor(source), identExtractor(alias), exprExtractor(body))

    case q"$source.map[$t](($alias) => $body)" =>
      Map(queryExtractor(source), identExtractor(alias), exprExtractor(body))

    case q"$source.flatMap[$t](($alias) => $matchAlias match { case (..$a) => $body })" if (alias == matchAlias) =>
      val aliases =
        a.map {
          case Bind(name, _) =>
            Ident(name.decodedName.toString)
        }
      val reduction =
        for ((a, i) <- aliases.zipWithIndex) yield {
          a -> Property(exprExtractor(alias), s"_${i + 1}")
        }
      FlatMap(queryExtractor(source), identExtractor(alias), BetaReduction(queryExtractor(body))(reduction.toMap))

    case q"$source.flatMap[$t](($alias) => $body)" =>
      FlatMap(queryExtractor(source), identExtractor(alias), queryExtractor(body))
  }

  val exprExtractor: Extractor[Expr] = Extractor[Expr] {
    case q"$a.$op($b)"       => BinaryOperation(exprExtractor(a), binaryOperator(op), exprExtractor(b))
    case `refExtractor`(ref) => ref
  }

  private def binaryOperator(name: TermName) =
    name.decodedName.toString match {
      case "-"    => io.getquill.ast.`-`
      case "+"    => io.getquill.ast.`+`
      case "=="   => io.getquill.ast.`==`
      case "!="   => io.getquill.ast.`!=`
      case "&&"   => io.getquill.ast.`&&`
      case "||"   => io.getquill.ast.`||`
      case ">"    => io.getquill.ast.`>`
      case ">="   => io.getquill.ast.`>=`
      case "<"    => io.getquill.ast.`<`
      case "<="   => io.getquill.ast.`<=`
      case "/"    => io.getquill.ast.`/`
      case "%"    => io.getquill.ast.`%`
      case "like" => io.getquill.ast.`like`
    }

  val refExtractor: Extractor[Ref] = Extractor[Ref] {
    case `valueExtractor`(value)   => value
    case `identExtractor`(ident)   => ident
    case `propertyExtractor`(prop) => prop
  }

  val propertyExtractor: Extractor[Property] = Extractor[Property] {
    case q"$e.$property" => Property(exprExtractor(e), property.decodedName.toString)
  }

  val valueExtractor: Extractor[Ref] = Extractor[Ref] {
    case q"null"                         => NullValue
    case Literal(c.universe.Constant(v)) => Constant(v)
    case q"((..$v))" if (v.size > 1)     => Tuple(v.map(exprExtractor(_)))
  }

  val identExtractor: Extractor[Ident] = Extractor[Ident] {
    case t: ValDef                        => Ident(t.name.decodedName.toString)
    case c.universe.Ident(TermName(name)) => Ident(name)
    case q"$i: $typ"                      => identExtractor(i)
    case c.universe.Bind(TermName(name), c.universe.Ident(termNames.WILDCARD)) =>
      Ident(name)
  }
}
