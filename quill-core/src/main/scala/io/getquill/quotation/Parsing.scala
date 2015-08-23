package io.getquill.quotation

import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context
import io.getquill.ast
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

trait Parsing {
  this: Quotation =>

  val c: Context
  import c.universe.{ Ident => _, Constant => _, Function => _, _ }

  case class Parser[T](p: PartialFunction[Tree, T])(implicit ct: ClassTag[T]) {

    def apply(tree: Tree) =
      unapply(tree).getOrElse {
        c.fail(s"Tree '$tree' can't be parsed to '${ct.runtimeClass.getSimpleName}'")
      }

    def unapply(tree: Tree): Option[T] =
      tree match {
        case q"io.getquill.`package`.unquote[$t]($quoted)" =>
          unquote[T](quoted)
        case q"$source.withFilter(($alias) => $body)" if (alias.name.toString.contains("ifrefutable")) =>
          unapply(source)
        case other =>
          p.lift(tree)
      }
  }

  val actionParser: Parser[Action] = Parser[Action] {
    case q"$query.$method(..$assignments)" if (method.decodedName.toString == "update") =>
      Update(astParser(query), assignments.map(assignmentParser(_)))
    case q"$query.insert(..$assignments)" =>
      Insert(astParser(query), assignments.map(assignmentParser(_)))
    case q"$query.delete" =>
      Delete(astParser(query))
  }

  val assignmentParser: Parser[Assignment] = Parser[Assignment] {
    case q"(($x) => scala.this.Predef.ArrowAssoc[$t]($ast).->[$v]($value))" =>
      Assignment(propertyParser(ast), astParser(value))
  }

  val astParser: Parser[Ast] = Parser[Ast] {
    case `queryParser`(query) => query
    case `functionParser`(function) => function
    case `actionParser`(action) => action
    case q"${ functionParser(a) }.apply[..$t](...$values)" => FunctionApply(a, values.flatten.map(astParser(_)))
    case q"${ identParser(a) }.apply[..$t](...$values)" => FunctionApply(a, values.flatten.map(astParser(_)))
    case q"$a.$op($b)" => BinaryOperation(astParser(a), binaryOperator(op), astParser(b))
    case q"!$a" => UnaryOperation(io.getquill.ast.`!`, astParser(a))
    case q"$a.isEmpty" => UnaryOperation(io.getquill.ast.`isEmpty`, astParser(a))
    case q"$a.nonEmpty" => UnaryOperation(io.getquill.ast.`nonEmpty`, astParser(a))
    case `identParser`(ident) => ident
    case `valueParser`(value) => value
    case `propertyParser`(value) => value

    case q"$tupleTree match { case (..$fieldsTrees) => $bodyTree }" =>
      val tuple = astParser(tupleTree)
      val fields = fieldsTrees.map(identParser(_))
      val body = astParser(bodyTree)
      val properties =
        for ((field, i) <- fields.zipWithIndex) yield {
          Property(tuple, s"_${i + 1}")
        }
      BetaReduction(body, fields.zip(properties): _*)
  }

  val functionParser: Parser[Function] = Parser[Function] {
    case q"new { def apply[..$t1](...$params) = $body }" =>
      Function(params.flatten.map(p => p: Tree).map(identParser(_)), astParser(body))
    case q"(..$params) => $body" =>
      Function(params.map(identParser(_)), astParser(body))
  }

  val queryParser: Parser[Query] = Parser[Query] {

    case q"io.getquill.`package`.queryable[${ t: Type }]" =>
      Table(t.typeSymbol.name.decodedName.toString)

    case q"$source.filter(($alias) => $body)" =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.withFilter(($alias) => $body)" =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.map[$t](($alias) => $body)" =>
      Map(astParser(source), identParser(alias), astParser(body))

    case q"$source.flatMap[$t](($alias) => $matchAlias match { case (..$a) => $body })" if (alias == matchAlias) =>
      val aliases =
        a.map {
          case Bind(name, _) =>
            Ident(name.decodedName.toString)
        }
      val reduction =
        for ((a, i) <- aliases.zipWithIndex) yield {
          a -> Property(astParser(alias), s"_${i + 1}")
        }
      FlatMap(astParser(source), identParser(alias), BetaReduction(astParser(body), reduction: _*))

    case q"$source.flatMap[$t](($alias) => $body)" =>
      FlatMap(astParser(source), identParser(alias), astParser(body))
  }

  val unaryOperatorParser = PartialFunction[String, UnaryOperator] {
    case "unary_!"  => ast.`!`
    case "nonEmpty" => ast.`nonEmpty`
    case "isEmpty"  => ast.`isEmpty`
  }

  private def binaryOperator(name: TermName) =
    name.decodedName.toString match {
      case "-"    => ast.`-`
      case "+"    => ast.`+`
      case "=="   => ast.`==`
      case "!="   => ast.`!=`
      case "&&"   => ast.`&&`
      case "||"   => ast.`||`
      case ">"    => ast.`>`
      case ">="   => ast.`>=`
      case "<"    => ast.`<`
      case "<="   => ast.`<=`
      case "/"    => ast.`/`
      case "%"    => ast.`%`
      case "like" => ast.`like`
    }

  val propertyParser: Parser[Property] = Parser[Property] {
    case q"$e.$property" => Property(astParser(e), property.decodedName.toString)
  }

  val valueParser: Parser[Value] = Parser[Value] {
    case q"null"                         => NullValue
    case Literal(c.universe.Constant(v)) => Constant(v)
    case q"((..$v))" if (v.size > 1)     => Tuple(v.map(astParser(_)))
  }

  val identParser: Parser[Ident] = Parser[Ident] {
    case t: ValDef                        => Ident(t.name.decodedName.toString)
    case c.universe.Ident(TermName(name)) => Ident(name)
    case q"$i: $typ"                      => identParser(i)
    case c.universe.Bind(TermName(name), c.universe.Ident(termNames.WILDCARD)) =>
      Ident(name)
  }
}
