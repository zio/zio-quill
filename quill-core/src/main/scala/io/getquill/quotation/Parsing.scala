package io.getquill.quotation

import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context

import io.getquill.{ Query => QuillQuery }
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
        case q"$source.withFilter(($alias) => $body)" if (alias.name.toString.contains("ifrefutable")) =>
          unapply(source)
        case q"$pack.unquote[$t]($quoted)" =>
          unquote[T](quoted).orElse(p.lift(tree))
        case other =>
          p.lift(tree)
      }
  }

  val astParser: Parser[Ast] = Parser[Ast] {

    case q"$pack.unquote[$t]($quoted)"  => Dynamic(quoted)

    case `queryParser`(query)           => query
    case `functionParser`(function)     => function
    case `actionParser`(action)         => action
    case `infixParser`(value)           => value
    case `valueParser`(value)           => value
    case `operationParser`(value)       => value
    case `identParser`(ident)           => ident
    case `propertyParser`(value)        => value
    case `optionOperationParser`(value) => value

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

  val queryParser: Parser[Query] = Parser[Query] {

    case q"$pack.query[${ t: Type }].apply(${ alias: String }, ..$propertyAliases)" =>
      Entity(t.typeSymbol.name.decodedName.toString, Some(alias), propertyAliases.map(propertyAliasParser(_)))

    case q"$pack.query[${ t: Type }]" =>
      Entity(t.typeSymbol.name.decodedName.toString, None, List())

    case q"$source.filter(($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.withFilter(($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.map[$t](($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      Map(astParser(source), identParser(alias), astParser(body))

    case q"$source.flatMap[$t](($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      FlatMap(astParser(source), identParser(alias), astParser(body))

    case q"$source.sortBy[$t](($alias) => $body)($ord)" if (is[QuillQuery[Any]](source)) =>
      SortBy(astParser(source), identParser(alias), astParser(body))

    case q"$source.groupBy[$t](($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      GroupBy(astParser(source), identParser(alias), astParser(body))

    case q"$a.min[$t]($n)" if (is[QuillQuery[Any]](a)) => Aggregation(AggregationOperator.`min`, astParser(a))
    case q"$a.max[$t]($n)" if (is[QuillQuery[Any]](a)) => Aggregation(AggregationOperator.`max`, astParser(a))
    case q"$a.avg[$t]($n)" if (is[QuillQuery[Any]](a)) => Aggregation(AggregationOperator.`avg`, astParser(a))
    case q"$a.sum[$t]($n)" if (is[QuillQuery[Any]](a)) => Aggregation(AggregationOperator.`sum`, astParser(a))
    case q"$a.size" if (is[QuillQuery[Any]](a))        => Aggregation(AggregationOperator.`size`, astParser(a))

    case q"$source.reverse" if (is[QuillQuery[Any]](source)) =>
      Reverse(astParser(source))

    case q"$source.take($n)" if (is[QuillQuery[Any]](source)) =>
      Take(astParser(source), astParser(n))

    case q"$source.drop($n)" if (is[QuillQuery[Any]](source)) =>
      Drop(astParser(source), astParser(n))

    case q"$source.union[$t]($n)" if (is[QuillQuery[Any]](source)) =>
      Union(astParser(source), astParser(n))

    case q"$source.unionAll[$t]($n)" if (is[QuillQuery[Any]](source)) =>
      UnionAll(astParser(source), astParser(n))

    case q"$source.++[$t]($n)" if (is[QuillQuery[Any]](source)) =>
      UnionAll(astParser(source), astParser(n))

    case q"${ outerJoinCallParser(typ, a, b) }.on(($aliasA, $aliasB) => $body)" =>
      OuterJoin(typ, a, b, identParser(aliasA), identParser(aliasB), astParser(body))

    case q"${ outerJoinCallParser(typ, a, b) }" =>
      c.fail("An outer join clause must be followed by 'on'.")
  }

  implicit val propertyAliasParser: Parser[PropertyAlias] = Parser[PropertyAlias] {
    case q"(($x1) => scala.this.Predef.ArrowAssoc[$t]($x2.$prop).->[$v](${ alias: String }))" =>
      PropertyAlias(prop.decodedName.toString, alias)
  }

  val outerJoinCallParser: Parser[(OuterJoinType, Ast, Ast)] = Parser[(OuterJoinType, Ast, Ast)] {
    case q"$a.leftJoin[$t, $u]($b)" if (is[QuillQuery[Any]](a))  => (LeftJoin, astParser(a), astParser(b))
    case q"$a.rightJoin[$t, $u]($b)" if (is[QuillQuery[Any]](a)) => (RightJoin, astParser(a), astParser(b))
    case q"$a.fullJoin[$t, $u]($b)" if (is[QuillQuery[Any]](a))  => (FullJoin, astParser(a), astParser(b))
  }

  val infixParser: Parser[Infix] = Parser[Infix] {
    case q"$infix.as[$t]" =>
      infixParser(infix)
    case q"$pack.InfixInterpolator(scala.StringContext.apply(..${ parts: List[String] })).infix(..$params)" =>
      Infix(parts, params.map(astParser(_)))
  }

  val functionParser: Parser[Function] = Parser[Function] {
    case q"new { def apply[..$t1](...$params) = $body }" =>
      Function(params.flatten.map(p => p: Tree).map(identParser(_)), astParser(body))
    case q"(..$params) => $body" =>
      Function(params.map(identParser(_)), astParser(body))
  }

  val identParser: Parser[Ident] = Parser[Ident] {
    case t: ValDef                        => Ident(t.name.decodedName.toString)
    case c.universe.Ident(TermName(name)) => Ident(name)
    case q"$i: $typ"                      => identParser(i)
    case c.universe.Bind(TermName(name), c.universe.Ident(termNames.WILDCARD)) =>
      Ident(name)
  }

  val optionOperationParser: Parser[OptionOperation] = Parser[OptionOperation] {
    case q"$o.map[$t](($alias) => $body)" if (is[Option[Any]](o)) =>
      OptionOperation(OptionMap, astParser(o), identParser(alias), astParser(body))
    case q"$o.forall(($alias) => $body)" if (is[Option[Any]](o)) =>
      OptionOperation(OptionForall, astParser(o), identParser(alias), astParser(body))
  }

  val propertyParser: Parser[Property] = Parser[Property] {
    case q"$e.$property" => Property(astParser(e), property.decodedName.toString)
  }

  val operationParser: Parser[Operation] = Parser[Operation] {
    case `equalityOperationParser`(value) => value
    case `booleanOperationParser`(value)  => value
    case `stringOperationParser`(value)   => value
    case `numericOperationParser`(value)  => value
    case `setOperationParser`(value)      => value
    case `functionApplyParser`(value)     => value
  }

  private def operationParser(cond: Tree => Boolean)(
    f: PartialFunction[String, Operator]): Parser[Operation] = {
    object operator {
      def unapply(t: TermName) =
        f.lift(t.decodedName.toString)
    }
    Parser[Operation] {
      case q"$a.${ operator(op: BinaryOperator) }($b)" if (cond(a) && cond(b)) =>
        BinaryOperation(astParser(a), op, astParser(b))
      case q"$a.${ operator(op: UnaryOperator) }" if (cond(a)) =>
        UnaryOperation(op, astParser(a))
      case q"$a.${ operator(op: UnaryOperator) }()" if (cond(a)) =>
        UnaryOperation(op, astParser(a))
    }
  }

  val functionApplyParser: Parser[Operation] = Parser[Operation] {
    case q"${ astParser(a) }.apply[..$t](...$values)" => FunctionApply(a, values.flatten.map(astParser(_)))
  }

  val equalityOperationParser: Parser[Operation] =
    operationParser(_ => true) {
      case "==" => EqualityOperator.`==`
      case "!=" => EqualityOperator.`!=`
    }

  val booleanOperationParser: Parser[Operation] =
    operationParser(is[Boolean](_)) {
      case "unary_!" => BooleanOperator.`!`
      case "&&"      => BooleanOperator.`&&`
      case "||"      => BooleanOperator.`||`
    }

  val stringOperationParser: Parser[Operation] =
    operationParser(is[String](_)) {
      case "+"           => StringOperator.+
      case "toUpperCase" => StringOperator.`toUpperCase`
      case "toLowerCase" => StringOperator.`toLowerCase`
    }

  val numericOperationParser: Parser[Operation] =
    operationParser(t => isNumeric(c.WeakTypeTag(t.tpe.erasure))) {
      case "unary_-" => NumericOperator.`-`
      case "-"       => NumericOperator.`-`
      case "+"       => NumericOperator.`+`
      case "*"       => NumericOperator.`*`
      case ">"       => NumericOperator.`>`
      case ">="      => NumericOperator.`>=`
      case "<"       => NumericOperator.`<`
      case "<="      => NumericOperator.`<=`
      case "/"       => NumericOperator.`/`
      case "%"       => NumericOperator.`%`
    }

  val setOperationParser: Parser[Operation] =
    operationParser(is[io.getquill.Query[Any]](_)) {
      case "isEmpty"  => SetOperator.`isEmpty`
      case "nonEmpty" => SetOperator.`nonEmpty`
    }

  private def isNumeric[T: WeakTypeTag] =
    c.inferImplicitValue(c.weakTypeOf[Numeric[T]]) != EmptyTree

  private def is[T](tree: Tree)(implicit t: TypeTag[T]) =
    tree.tpe <:< t.tpe

  val valueParser: Parser[Value] = Parser[Value] {
    case q"null"                         => NullValue
    case Literal(c.universe.Constant(v)) => Constant(v)
    case q"((..$v))" if (v.size > 1)     => Tuple(v.map(astParser(_)))
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

}
