package io.getquill.quotation

import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Action
import io.getquill.ast.Assignment
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Constant
import io.getquill.ast.Delete
import io.getquill.ast.Ast
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Insert
import io.getquill.ast.Map
import io.getquill.ast.NullValue
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.Table
import io.getquill.ast.Tuple
import io.getquill.ast.Update
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages._
import io.getquill.ast.UnaryOperation
import io.getquill.ast.UnaryOperator
import io.getquill.ast.PrefixUnaryOperator
import io.getquill.ast.UnaryOperation
import io.getquill.ast.Function
import io.getquill.ast.FunctionApply
import io.getquill.ast.Value

trait Unliftables {
  this: Quotation =>

  val c: Context
  import c.universe.{ Ident => _, Constant => _, Function => _, _ }

  case class Unliftable[T](p: PartialFunction[Tree, T])(implicit t: ClassTag[T]) extends c.universe.Unliftable[T] {

    def apply(tree: Tree) =
      unapply(tree).getOrElse {
        c.fail(s"Tree '$tree' can't be parsed to '${t.runtimeClass.getSimpleName}'")
      }

    def unapply(tree: Tree): Option[T] =
      tree match {
        case q"$source.withFilter(($alias) => $body)" if (alias.name.toString.contains("ifrefutable")) =>
          unapply(source)
        case q"io.getquill.`package`.unquote[$t]($quoted)" =>
          unapply(unquoteTree(quoted))
        case other =>
          p.lift(tree)
      }
  }

  implicit val actionUnliftable: Unliftable[Action] = Unliftable[Action] {
    case q"$query.$method(..$assignments)" if (method.decodedName.toString == "update") =>
      Update(astUnliftable(query), assignments.map(assignmentUnliftable(_)))
    case q"$query.insert(..$assignments)" =>
      Insert(astUnliftable(query), assignments.map(assignmentUnliftable(_)))
    case q"$query.delete" =>
      Delete(astUnliftable(query))
  }

  implicit val assignmentUnliftable: Unliftable[Assignment] = Unliftable[Assignment] {
    case q"(($x) => scala.this.Predef.ArrowAssoc[$t]($ast).->[$v]($value))" =>
      Assignment(propertyUnliftable(ast), astUnliftable(value))
  }

  implicit val astUnliftable: Unliftable[Ast] = Unliftable[Ast] {
    case `queryUnliftable`(query)                    => query
    case `functionExtrator`(function)                => function
    case `actionUnliftable`(action)                  => action
    case q"${ a: Function }.apply[..$t](...$values)" => FunctionApply(a, values.flatten.map(astUnliftable(_)))
    case q"${ a: Ident }.apply[..$t](...$values)"    => FunctionApply(a, values.flatten.map(astUnliftable(_)))
    case q"$a.$op($b)"                               => BinaryOperation(astUnliftable(a), binaryOperator(op), astUnliftable(b))
    case q"!$a"                                      => UnaryOperation(io.getquill.ast.`!`, astUnliftable(a))
    case q"$a.isEmpty"                               => UnaryOperation(io.getquill.ast.`isEmpty`, astUnliftable(a))
    case q"$a.nonEmpty"                              => UnaryOperation(io.getquill.ast.`nonEmpty`, astUnliftable(a))
    case `identUnliftable`(ident)                    => ident
    case `valueUnliftable`(value)                    => value
    case `propertyUnliftable`(value)                 => value

    case q"${ tuple: Ast } match { case (..${ fields: List[Ident] }) => ${ body: Ast } }" =>
      val properties =
        for ((field, i) <- fields.zipWithIndex) yield {
          Property(tuple, s"_${i + 1}")
        }
      BetaReduction(body, fields.zip(properties): _*)
  }

  implicit val functionExtrator: Unliftable[Function] = Unliftable[Function] {
    case q"new { def apply[..$t1](...$params) = $body }" =>
      Function(params.flatten.map(p => p: Tree).map(identUnliftable(_)), astUnliftable(body))
    case q"(..$params) => $body" =>
      Function(params.map(identUnliftable(_)), astUnliftable(body))
  }

  implicit val queryUnliftable: Unliftable[Query] = Unliftable[Query] {

    case q"io.getquill.`package`.queryable[${ t: Type }]" =>
      Table(t.typeSymbol.name.decodedName.toString)

    case q"$source.filter(($alias) => $body)" =>
      Filter(astUnliftable(source), identUnliftable(alias), astUnliftable(body))

    case q"$source.withFilter(($alias) => $body)" =>
      Filter(astUnliftable(source), identUnliftable(alias), astUnliftable(body))

    case q"$source.map[$t](($alias) => $body)" =>
      Map(astUnliftable(source), identUnliftable(alias), astUnliftable(body))

    case q"$source.flatMap[$t](($alias) => $matchAlias match { case (..$a) => $body })" if (alias == matchAlias) =>
      val aliases =
        a.map {
          case Bind(name, _) =>
            Ident(name.decodedName.toString)
        }
      val reduction =
        for ((a, i) <- aliases.zipWithIndex) yield {
          a -> Property(astUnliftable(alias), s"_${i + 1}")
        }
      FlatMap(astUnliftable(source), identUnliftable(alias), BetaReduction(astUnliftable(body), reduction: _*))

    case q"$source.flatMap[$t](($alias) => $body)" =>
      FlatMap(astUnliftable(source), identUnliftable(alias), astUnliftable(body))
  }

  implicit val unaryOperatorUnliftable = PartialFunction[String, UnaryOperator] {
    case "unary_!"  => io.getquill.ast.`!`
    case "nonEmpty" => io.getquill.ast.`nonEmpty`
    case "isEmpty"  => io.getquill.ast.`isEmpty`
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

  implicit val propertyUnliftable: Unliftable[Property] = Unliftable[Property] {
    case q"$e.$property" => Property(astUnliftable(e), property.decodedName.toString)
  }

  implicit val valueUnliftable: Unliftable[Value] = Unliftable[Value] {
    case q"null"                         => NullValue
    case Literal(c.universe.Constant(v)) => Constant(v)
    case q"((..$v))" if (v.size > 1)     => Tuple(v.map(astUnliftable(_)))
  }

  implicit val identUnliftable: Unliftable[Ident] = Unliftable[Ident] {
    case t: ValDef                        => Ident(t.name.decodedName.toString)
    case c.universe.Ident(TermName(name)) => Ident(name)
    case q"$i: $typ"                      => identUnliftable(i)
    case c.universe.Bind(TermName(name), c.universe.Ident(termNames.WILDCARD)) =>
      Ident(name)
  }
}
