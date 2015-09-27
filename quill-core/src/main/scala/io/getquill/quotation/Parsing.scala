package io.getquill.quotation

import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context

import io.getquill.{ Action => _, Query => _, _ }
import io.getquill.ast._
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages.RichContext

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
        case q"$pack.unquote[$t]($quoted)" =>
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

  private val assignmentParser: Parser[Assignment] = Parser[Assignment] {
    case q"(($x1) => scala.this.Predef.ArrowAssoc[$t]($x2.$prop).->[$v]($value))" =>
      Assignment(prop.decodedName.toString, astParser(value))
  }

  val astParser: Parser[Ast] = Parser[Ast] {
    case `queryParser`(query) => query
    case `functionParser`(function) => function
    case `actionParser`(action) => action
    case `infixParser`(value) => value
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

    case q"$pack.query[${ t: Type }]" =>
      Entity(t.typeSymbol.name.decodedName.toString)

    case q"$source.filter(($alias) => $body)" =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.withFilter(($alias) => $body)" =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.map[$t](($alias) => $body)" =>
      Map(astParser(source), identParser(alias), astParser(body))

    case q"$source.flatMap[$t](($alias) => $body)" =>
      FlatMap(astParser(source), identParser(alias), astParser(body))

    case q"$source.sortBy[$t](($alias) => $body)($ord)" =>
      SortBy(astParser(source), identParser(alias), astParser(body))

    case q"$source.reverse" =>
      Reverse(astParser(source))

    case q"$source.take($n)" =>
      Take(astParser(source), astParser(n))

    case q"$source.drop($n)" =>
      Drop(astParser(source), astParser(n))

    case q"$source.union[$t]($n)" =>
      Union(astParser(source), astParser(n))

    case q"$source.unionAll[$t]($n)" =>
      UnionAll(astParser(source), astParser(n))

    case q"$source.++[$t]($n)" =>
      UnionAll(astParser(source), astParser(n))
  }

  private def binaryOperator(name: TermName) =
    name.decodedName.toString match {
      case "-"  => ast.`-`
      case "+"  => ast.`+`
      case "*"  => ast.`*`
      case "==" => ast.`==`
      case "!=" => ast.`!=`
      case "&&" => ast.`&&`
      case "||" => ast.`||`
      case ">"  => ast.`>`
      case ">=" => ast.`>=`
      case "<"  => ast.`<`
      case "<=" => ast.`<=`
      case "/"  => ast.`/`
      case "%"  => ast.`%`
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

  val infixParser: Parser[Infix] = Parser[Infix] {
    case q"$infix.as[$t]" =>
      infixParser(infix)
    case q"$pack.InfixInterpolator(scala.StringContext.apply(..${ parts: List[String] })).infix(..$params)" =>
      Infix(parts, params.map(astParser(_)))
  }
}
