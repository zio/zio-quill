package io.getquill.quotation

import scala.reflect.ClassTag
import io.getquill.{ Query => QuillQuery }
import io.getquill.ast._
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages.RichContext
import io.getquill.util.Interleave

trait Parsing extends SchemaConfigParsing {
  this: Quotation =>

  import c.universe.{ Ident => _, Constant => _, Function => _, If => _, Block => _, _ }

  case class Parser[T](p: PartialFunction[Tree, T])(implicit ct: ClassTag[T]) {

    def apply(tree: Tree) =
      unapply(tree).getOrElse {
        c.fail(s"Tree '$tree' can't be parsed to '${ct.runtimeClass.getSimpleName}'")
      }

    def unapply(tree: Tree): Option[T] =
      tree match {
        case q"$source.withFilter(($alias) => $body)" if (alias.name.toString.contains("ifrefutable")) =>
          unapply(source)
        case other =>
          p.lift(tree)
      }
  }

  val astParser: Parser[Ast] = Parser[Ast] {
    case `bindingParser`(value)             => value
    case `valParser`(value)                 => value
    case `patMatchValParser`(value)         => value
    case `valueParser`(value)               => value
    case `quotedAstParser`(value)           => value
    case `queryParser`(query)               => query
    case `functionParser`(function)         => function
    case `actionParser`(action)             => action
    case `infixParser`(value)               => value
    case `orderingParser`(value)            => value
    case `operationParser`(value)           => value
    case `identParser`(ident)               => ident
    case `propertyParser`(value)            => value
    case `stringInterpolationParser`(value) => value
    case `optionOperationParser`(value)     => value
    case `boxingParser`(value)              => value
    case `ifParser`(value)                  => value

    case q"$i: $typ"                        => astParser(i)

    case `patMatchParser`(value)            => value
    case `blockParser`(block)               => block
  }

  val blockParser: Parser[Block] = Parser[Block] {
    case q"{..$exprs}" if exprs.size > 1 => Block(exprs.map(astParser(_)))
  }

  val valParser: Parser[Val] = Parser[Val] {
    case q"val $name: $typ = $body" => Val(ident(name), astParser(body))
  }

  val patMatchValParser: Parser[Val] = Parser[Val] {
    case q"$mods val $name: $typ = ${ patMatchParser(value) }" =>
      Val(ident(name), value)
  }

  val patMatchParser: Parser[Ast] = Parser[Ast] {
    case q"$expr match { case ($fields) => $body }" =>
      patMatchParser(expr, fields, body)
  }

  private def patMatchParser(tupleTree: Tree, fieldsTree: Tree, bodyTree: Tree) = {
    val tuple = astParser(tupleTree)
    val fields = astParser(fieldsTree)
    val body = astParser(bodyTree)
    def property(path: List[Int]) =
      path.foldLeft(tuple) {
        case (t, i) => Property(t, s"_${i + 1}")
      }
    def reductions(ast: Ast, path: List[Int] = List()): List[(Ident, Ast)] = {
      ast match {
        case ident: Ident => List(ident -> property(path))
        case Tuple(elems) =>
          elems.zipWithIndex.flatMap {
            case (elem, idx) => reductions(elem, path :+ idx)
          }
        case other =>
          c.fail(s"Please report a bug. Expected tuple, val, or ident, got '$other'")
      }
    }
    BetaReduction(body, reductions(fields): _*)
  }

  val ifParser: Parser[If] = Parser[If] {
    case q"if($a) $b else $c" => If(astParser(a), astParser(b), astParser(c))
  }

  val bindingParser: Parser[Binding] = Parser[Binding] {
    case q"$pack.lift[$t]($value)" => CompileTimeBinding(value)
  }

  val quotedAstParser: Parser[Ast] = Parser[Ast] {
    case q"$pack.unquote[$t]($quoted)" => astParser(quoted)
    case t if (t.tpe <:< c.weakTypeOf[Quoted[Any]]) =>
      unquote[Ast](t) match {
        case Some(ast) if (!IsDynamic(ast)) => QuotedReference(t, Rebind(c)(t, ast, astParser(_)))
        case other                          => Dynamic(t)
      }
  }

  val boxingParser: Parser[Ast] = Parser[Ast] {
    // BigDecimal
    case q"$pack.int2bigDecimal(${ astParser(v) })"            => v
    case q"$pack.long2bigDecimal(${ astParser(v) })"           => v
    case q"$pack.double2bigDecimal(${ astParser(v) })"         => v
    case q"$pack.javaBigDecimal2bigDecimal(${ astParser(v) })" => v

    // Predef autoboxing
    case q"$pack.byte2Byte(${ astParser(v) })"                 => v
    case q"$pack.short2Short(${ astParser(v) })"               => v
    case q"$pack.char2Character(${ astParser(v) })"            => v
    case q"$pack.int2Integer(${ astParser(v) })"               => v
    case q"$pack.long2Long(${ astParser(v) })"                 => v
    case q"$pack.float2Float(${ astParser(v) })"               => v
    case q"$pack.double2Double(${ astParser(v) })"             => v
    case q"$pack.boolean2Boolean(${ astParser(v) })"           => v

    case q"$pack.Byte2byte(${ astParser(v) })"                 => v
    case q"$pack.Short2short(${ astParser(v) })"               => v
    case q"$pack.Character2char(${ astParser(v) })"            => v
    case q"$pack.Integer2int(${ astParser(v) })"               => v
    case q"$pack.Long2long(${ astParser(v) })"                 => v
    case q"$pack.Float2float(${ astParser(v) })"               => v
    case q"$pack.Double2double(${ astParser(v) })"             => v
    case q"$pack.Boolean2boolean(${ astParser(v) })"           => v
  }

  val queryParser: Parser[Query] = Parser[Query] {

    case q"$pack.query[${ t: Type }].apply(($alias) => $body)" =>
      val config = parseEntityConfig(body)
      Entity(t.typeSymbol.name.decodedName.toString, config.alias, config.properties, config.generated)

    case q"$pack.query[${ t: Type }]" =>
      Entity(t.typeSymbol.name.decodedName.toString, None, List(), None)

    case q"$source.filter(($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.withFilter(($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.map[$t](($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      Map(astParser(source), identParser(alias), astParser(body))

    case q"$source.flatMap[$t](($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      FlatMap(astParser(source), identParser(alias), astParser(body))

    case q"$source.sortBy[$t](($alias) => $body)($ord)" if (is[QuillQuery[Any]](source)) =>
      SortBy(astParser(source), identParser(alias), astParser(body), astParser(ord))

    case q"$source.groupBy[$t](($alias) => $body)" if (is[QuillQuery[Any]](source)) =>
      GroupBy(astParser(source), identParser(alias), astParser(body))

    case q"$a.min[$t]($o)" if (is[QuillQuery[Any]](a)) => Aggregation(AggregationOperator.`min`, astParser(a))
    case q"$a.max[$t]($o)" if (is[QuillQuery[Any]](a)) => Aggregation(AggregationOperator.`max`, astParser(a))
    case q"$a.avg[$t]($n)" if (is[QuillQuery[Any]](a)) => Aggregation(AggregationOperator.`avg`, astParser(a))
    case q"$a.sum[$t]($n)" if (is[QuillQuery[Any]](a)) => Aggregation(AggregationOperator.`sum`, astParser(a))
    case q"$a.size" if (is[QuillQuery[Any]](a))        => Aggregation(AggregationOperator.`size`, astParser(a))

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

    case q"${ joinCallParser(typ, a, Some(b)) }.on(($aliasA, $aliasB) => $body)" =>
      Join(typ, a, b, identParser(aliasA), identParser(aliasB), astParser(body))

    case q"${ joinCallParser(typ, a, None) }($aliasA => $body)" =>
      val alias = identParser(aliasA)
      Join(typ, a, a, alias, alias, astParser(body))

    case q"${ joinCallParser(typ, a, b) }" =>
      c.fail("a join clause must be followed by 'on'.")

    case q"$source.distinct" if (is[QuillQuery[Any]](source)) =>
      Distinct(astParser(source))

  }

  implicit val orderingParser: Parser[Ordering] = Parser[Ordering] {
    case q"$pack.orderingToOrd[$t]($o)"      => AscNullsFirst
    case q"$pack.Ord.apply[..$t](..$elems)"  => TupleOrdering(elems.map(orderingParser(_)))
    case q"$pack.Ord.asc[$t]($o)"            => Asc
    case q"$pack.Ord.desc[$t]($o)"           => Desc
    case q"$pack.Ord.ascNullsFirst[$t]($o)"  => AscNullsFirst
    case q"$pack.Ord.descNullsFirst[$t]($o)" => DescNullsFirst
    case q"$pack.Ord.ascNullsLast[$t]($o)"   => AscNullsLast
    case q"$pack.Ord.descNullsLast[$t]($o)"  => DescNullsLast
  }

  implicit val propertyAliasParser: Parser[PropertyAlias] = Parser[PropertyAlias] {
    case q"(($x1) => scala.this.Predef.ArrowAssoc[$t]($x2.$prop).->[$v](${ alias: String }))" =>
      PropertyAlias(prop.decodedName.toString, alias)
  }

  val joinCallParser: Parser[(JoinType, Ast, Option[Ast])] = Parser[(JoinType, Ast, Option[Ast])] {
    case q"$a.join[$t, $u]($b)" if (is[QuillQuery[Any]](a))      => (InnerJoin, astParser(a), Some(astParser(b)))
    case q"$a.leftJoin[$t, $u]($b)" if (is[QuillQuery[Any]](a))  => (LeftJoin, astParser(a), Some(astParser(b)))
    case q"$a.rightJoin[$t, $u]($b)" if (is[QuillQuery[Any]](a)) => (RightJoin, astParser(a), Some(astParser(b)))
    case q"$a.fullJoin[$t, $u]($b)" if (is[QuillQuery[Any]](a))  => (FullJoin, astParser(a), Some(astParser(b)))

    case q"$a.join[$t]" if (is[QuillQuery[Any]](a))              => (InnerJoin, astParser(a), None)
    case q"$a.leftJoin[$t]" if (is[QuillQuery[Any]](a))          => (LeftJoin, astParser(a), None)
    case q"$a.rightJoin[$t]" if (is[QuillQuery[Any]](a))         => (RightJoin, astParser(a), None)
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
    case t: ValDef                        => identClean(Ident(t.name.decodedName.toString))
    case c.universe.Ident(TermName(name)) => identClean(Ident(name))
    case q"$cls.this.$i"                  => identClean(Ident(i.decodedName.toString))
    case c.universe.Bind(TermName(name), c.universe.Ident(termNames.WILDCARD)) =>
      identClean(Ident(name))
  }
  private def identClean(x: Ident): Ident = x.copy(name = x.name.replace("$", ""))
  private def ident(x: TermName): Ident = identClean(Ident(x.decodedName.toString))

  val optionOperationParser: Parser[OptionOperation] = Parser[OptionOperation] {
    case q"$o.map[$t]({($alias) => $body})" if (is[Option[Any]](o)) =>
      OptionOperation(OptionMap, astParser(o), identParser(alias), astParser(body))
    case q"$o.forall({($alias) => $body})" if (is[Option[Any]](o)) =>
      OptionOperation(OptionForall, astParser(o), identParser(alias), astParser(body))
    case q"$o.exists({($alias) => $body})" if (is[Option[Any]](o)) =>
      OptionOperation(OptionExists, astParser(o), identParser(alias), astParser(body))
  }

  val propertyParser: Parser[Ast] = Parser[Ast] {
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
    f: PartialFunction[String, Operator]
  ): Parser[Operation] = {
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
      case "==" | "equals" => EqualityOperator.`==`
      case "!="            => EqualityOperator.`!=`
    }

  val booleanOperationParser: Parser[Operation] =
    operationParser(is[Boolean](_)) {
      case "unary_!" => BooleanOperator.`!`
      case "&&"      => BooleanOperator.`&&`
      case "||"      => BooleanOperator.`||`
    }

  val stringInterpolationParser: Parser[Ast] = Parser[Ast] {
    case q"scala.StringContext.apply(..$parts).s(..$params)" =>
      val asts =
        Interleave(parts.map(astParser(_)), params.map(astParser(_)))
          .filter(_ != Constant(""))
      asts.tail.foldLeft(asts.head) {
        case (a, b) =>
          BinaryOperation(a, StringOperator.`+`, b)
      }
  }

  val stringOperationParser: Parser[Operation] =
    operationParser(is[String](_)) {
      case "+"           => StringOperator.`+`
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

  val setOperationParser: Parser[Operation] = {
    val unary =
      operationParser(is[io.getquill.Query[Any]](_)) {
        case "isEmpty"  => SetOperator.`isEmpty`
        case "nonEmpty" => SetOperator.`nonEmpty`
      }
    Parser[Operation] {
      case q"$a.contains[..$t]($b)" => BinaryOperation(astParser(a), SetOperator.`contains`, astParser(b))
      case unary(op)                => op
    }
  }

  private def isNumeric[T: WeakTypeTag] =
    c.inferImplicitValue(c.weakTypeOf[Numeric[T]]) != EmptyTree

  private def is[T](tree: Tree)(implicit t: TypeTag[T]) =
    tree.tpe <:< t.tpe

  private def isTraversable(tree: Tree) = {
    tree.tpe <:< typeOf[Traversable[_]]
  }

  val valueParser: Parser[Value] = Parser[Value] {
    case q"null" => NullValue
    case Literal(c.universe.Constant(v)) => Constant(v)
    case q"((..$v))" if (v.size > 1) => Tuple(v.map(astParser(_)))
    case tree @ q"$pack.$coll.apply[..$t](..$v)" if isTraversable(tree) => Collection(v.map(astParser(_)))
    case q"((scala.this.Predef.ArrowAssoc[$t1]($v1).$arrow[$t2]($v2)))" => Tuple(List(astParser(v1), astParser(v2)))
  }

  val actionParser: Parser[Ast] = Parser[Ast] {
    case q"$query.$method(..$assignments)" if (method.decodedName.toString == "update") =>
      AssignedAction(Update(astParser(query)), assignments.map(assignmentParser(_)))
    case q"$query.insert(..$assignments)" =>
      AssignedAction(Insert(astParser(query)), assignments.map(assignmentParser(_)))
    case q"$query.$method" if (method.decodedName.toString == "update") =>
      Function(List(Ident("x1")), Update(astParser(query)))
    case q"$query.insert" =>
      Function(List(Ident("x1")), Insert(astParser(query)))
    case q"$query.delete" =>
      Delete(astParser(query))
  }

  private val assignmentParser: Parser[Assignment] = Parser[Assignment] {
    case q"((${ identParser(i1) }) => scala.this.Predef.ArrowAssoc[$t](${ identParser(i2) }.$prop).$arrow[$v]($value))" if (i1 == i2) =>
      Assignment(i1, prop.decodedName.toString, astParser(value))
  }

}
