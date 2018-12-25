package io.getquill.quotation

import scala.reflect.ClassTag
import io.getquill.ast._
import io.getquill.Embedded
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages.RichContext
import io.getquill.util.Interleave
import io.getquill.dsl.CoreDsl
import scala.collection.immutable.StringOps
import scala.reflect.macros.TypecheckException

trait Parsing {
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
    case q"$i: $typ"                         => astParser(i)
    case `queryParser`(value)                => value
    case `liftParser`(value)                 => value
    case `valParser`(value)                  => value
    case `patMatchValParser`(value)          => value
    case `valueParser`(value)                => value
    case `quotedAstParser`(value)            => value
    case `functionParser`(value)             => value
    case `actionParser`(value)               => value
    case `conflictParser`(value)             => value
    case `infixParser`(value)                => value
    case `orderingParser`(value)             => value
    case `operationParser`(value)            => value
    case `identParser`(value)                => value
    case `optionOperationParser`(value)      => value
    case `propertyParser`(value)             => value
    case `stringInterpolationParser`(value)  => value
    case `traversableOperationParser`(value) => value
    case `boxingParser`(value)               => value
    case `ifParser`(value)                   => value
    case `patMatchParser`(value)             => value
    case `blockParser`(block)                => block
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

  val liftParser: Parser[Lift] = Parser[Lift] {

    case q"$pack.liftScalar[$t]($value)($encoder)"          => ScalarValueLift(value.toString, value, encoder)
    case q"$pack.liftCaseClass[$t]($value)"                 => CaseClassValueLift(value.toString, value)

    case q"$pack.liftQueryScalar[$t, $u]($value)($encoder)" => ScalarQueryLift(value.toString, value, encoder)
    case q"$pack.liftQueryCaseClass[$t, $u]($value)"        => CaseClassQueryLift(value.toString, value)

    // Unused, it's here only to make eclipse's presentation compiler happy :(
    case q"$pack.lift[$t]($value)"                          => ScalarValueLift(value.toString, value, q"null")
    case q"$pack.liftQuery[$t, $u]($value)"                 => ScalarQueryLift(value.toString, value, q"null")
  }

  val quotedAstParser: Parser[Ast] = Parser[Ast] {
    case q"$pack.unquote[$t]($quoted)" => astParser(quoted)
    case t if (t.tpe <:< c.weakTypeOf[CoreDsl#Quoted[Any]]) =>
      unquote[Ast](t) match {
        case Some(ast) if (!IsDynamic(ast)) =>
          t match {
            case t: c.universe.Block => ast // expand quote(quote(body)) locally
            case t =>
              Rebind(c)(t, ast, astParser(_)) match {
                case Some(ast) => ast
                case None      => QuotedReference(t, ast)
              }
          }
        case other => Dynamic(t)
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
    case q"$pack.augmentString(${ astParser(v) })"             => v
    case q"$pack.unaugmentString(${ astParser(v) })"           => v

    case q"$pack.Byte2byte(${ astParser(v) })"                 => v
    case q"$pack.Short2short(${ astParser(v) })"               => v
    case q"$pack.Character2char(${ astParser(v) })"            => v
    case q"$pack.Integer2int(${ astParser(v) })"               => v
    case q"$pack.Long2long(${ astParser(v) })"                 => v
    case q"$pack.Float2float(${ astParser(v) })"               => v
    case q"$pack.Double2double(${ astParser(v) })"             => v
    case q"$pack.Boolean2boolean(${ astParser(v) })"           => v
  }

  val queryParser: Parser[Ast] = Parser[Ast] {

    case q"$pack.query[$t]" =>
      // Unused, it's here only to make eclipse's presentation compiler happy
      Entity("unused", Nil)

    case q"$pack.querySchema[$t](${ name: String }, ..$properties)" =>
      Entity(name, properties.map(propertyAliasParser(_)))

    case q"$source.filter(($alias) => $body)" if (is[CoreDsl#Query[Any]](source)) =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.withFilter(($alias) => $body)" if (is[CoreDsl#Query[Any]](source)) =>
      Filter(astParser(source), identParser(alias), astParser(body))

    case q"$source.map[$t](($alias) => $body)" if (is[CoreDsl#Query[Any]](source)) =>
      Map(astParser(source), identParser(alias), astParser(body))

    case q"$source.flatMap[$t](($alias) => $body)" if (is[CoreDsl#Query[Any]](source)) =>
      FlatMap(astParser(source), identParser(alias), astParser(body))

    case q"$source.concatMap[$t, $u](($alias) => $body)($ev)" if (is[CoreDsl#Query[Any]](source)) =>
      ConcatMap(astParser(source), identParser(alias), astParser(body))

    case q"$source.sortBy[$t](($alias) => $body)($ord)" if (is[CoreDsl#Query[Any]](source)) =>
      SortBy(astParser(source), identParser(alias), astParser(body), astParser(ord))

    case q"$source.groupBy[$t](($alias) => $body)" if (is[CoreDsl#Query[Any]](source)) =>
      GroupBy(astParser(source), identParser(alias), astParser(body))

    case q"$a.min[$t]" if (is[CoreDsl#Query[Any]](a))     => Aggregation(AggregationOperator.`min`, astParser(a))
    case q"$a.max[$t]" if (is[CoreDsl#Query[Any]](a))     => Aggregation(AggregationOperator.`max`, astParser(a))
    case q"$a.avg[$t]($n)" if (is[CoreDsl#Query[Any]](a)) => Aggregation(AggregationOperator.`avg`, astParser(a))
    case q"$a.sum[$t]($n)" if (is[CoreDsl#Query[Any]](a)) => Aggregation(AggregationOperator.`sum`, astParser(a))
    case q"$a.size" if (is[CoreDsl#Query[Any]](a))        => Aggregation(AggregationOperator.`size`, astParser(a))

    case q"$source.take($n)" if (is[CoreDsl#Query[Any]](source)) =>
      Take(astParser(source), astParser(n))

    case q"$source.drop($n)" if (is[CoreDsl#Query[Any]](source)) =>
      Drop(astParser(source), astParser(n))

    case q"$source.union[$t]($n)" if (is[CoreDsl#Query[Any]](source)) =>
      Union(astParser(source), astParser(n))

    case q"$source.unionAll[$t]($n)" if (is[CoreDsl#Query[Any]](source)) =>
      UnionAll(astParser(source), astParser(n))

    case q"$source.++[$t]($n)" if (is[CoreDsl#Query[Any]](source)) =>
      UnionAll(astParser(source), astParser(n))

    case q"${ joinCallParser(typ, a, Some(b)) }.on(($aliasA, $aliasB) => $body)" =>
      Join(typ, a, b, identParser(aliasA), identParser(aliasB), astParser(body))

    case q"${ joinCallParser(typ, a, None) }($alias => $body)" =>
      FlatJoin(typ, a, identParser(alias), astParser(body))

    case q"${ joinCallParser(typ, a, b) }" =>
      c.fail("a join clause must be followed by 'on'.")

    case q"$source.distinct" if (is[CoreDsl#Query[Any]](source)) =>
      Distinct(astParser(source))

    case q"$source.nested" if (is[CoreDsl#Query[Any]](source)) =>
      Nested(astParser(source))

  }

  implicit val propertyAliasParser: Parser[PropertyAlias] = Parser[PropertyAlias] {
    case q"(($x1) => $pack.Predef.ArrowAssoc[$t]($prop).$arrow[$v](${ alias: String }))" =>
      def path(tree: Tree): List[String] =
        tree match {
          case q"$a.$b" =>
            path(a) :+ b.decodedName.toString
          case q"$a.$b.map[$tpe]((..$args) => $tr)" =>
            path(a) ++ (b.decodedName.toString :: path(tr))
          case _ =>
            Nil
        }
      PropertyAlias(path(prop), alias)
  }

  implicit val orderingParser: Parser[Ordering] = Parser[Ordering] {
    case q"$pack.implicitOrd[$t]"           => AscNullsFirst
    case q"$pack.Ord.apply[..$t](..$elems)" => TupleOrdering(elems.map(orderingParser(_)))
    case q"$pack.Ord.asc[$t]"               => Asc
    case q"$pack.Ord.desc[$t]"              => Desc
    case q"$pack.Ord.ascNullsFirst[$t]"     => AscNullsFirst
    case q"$pack.Ord.descNullsFirst[$t]"    => DescNullsFirst
    case q"$pack.Ord.ascNullsLast[$t]"      => AscNullsLast
    case q"$pack.Ord.descNullsLast[$t]"     => DescNullsLast
  }

  val joinCallParser: Parser[(JoinType, Ast, Option[Ast])] = Parser[(JoinType, Ast, Option[Ast])] {
    case q"$a.join[$t, $u]($b)" if (is[CoreDsl#Query[Any]](a))      => (InnerJoin, astParser(a), Some(astParser(b)))
    case q"$a.leftJoin[$t, $u]($b)" if (is[CoreDsl#Query[Any]](a))  => (LeftJoin, astParser(a), Some(astParser(b)))
    case q"$a.rightJoin[$t, $u]($b)" if (is[CoreDsl#Query[Any]](a)) => (RightJoin, astParser(a), Some(astParser(b)))
    case q"$a.fullJoin[$t, $u]($b)" if (is[CoreDsl#Query[Any]](a))  => (FullJoin, astParser(a), Some(astParser(b)))

    case q"$a.join[$t]" if (is[CoreDsl#Query[Any]](a))              => (InnerJoin, astParser(a), None)
    case q"$a.leftJoin[$t]" if (is[CoreDsl#Query[Any]](a))          => (LeftJoin, astParser(a), None)
    case q"$a.rightJoin[$t]" if (is[CoreDsl#Query[Any]](a))         => (RightJoin, astParser(a), None)
  }

  val infixParser: Parser[Ast] = Parser[Ast] {
    case q"$infix.as[$t]" =>
      infixParser(infix)
    case q"$pack.InfixInterpolator(scala.StringContext.apply(..${ parts: List[String] })).infix(..$params)" =>

      if (parts.find(_.endsWith("#")).isDefined) {

        val elements =
          parts.zipWithIndex.flatMap {
            case (part, idx) if idx < params.length =>
              if (part.endsWith("#")) {
                Left(q"${part.dropRight(1)} + String.valueOf(${params(idx)})") :: Nil
              } else {
                Left(q"$part") :: Right(params(idx)) :: Nil
              }
            case (part, idx) =>
              Left(q"$part") :: Nil
          }

        val fused =
          (elements.foldLeft(List[Either[Tree, Tree]]()) {
            case (Left(a) :: tail, Left(b)) =>
              Left(q"$a + $b") :: tail
            case (list, b) =>
              b :: list
          }).reverse

        val newParts =
          fused.collect {
            case Left(a) => a
          }

        val newParams =
          fused.collect {
            case Right(a) => astParser(a)
          }
        Dynamic {
          c.typecheck(q"""
            new ${c.prefix}.Quoted[Any] {
              override def ast = io.getquill.ast.Infix($newParts, $newParams)
            }
          """)
        }
      } else
        Infix(parts, params.map(astParser(_)))
  }

  val functionParser: Parser[Function] = Parser[Function] {
    case q"new { def apply[..$t1](...$params) = $body }" =>
      c.fail("Anonymous classes aren't supported for function declaration anymore. Use a method with a type parameter instead. " +
        "For instance, replace `val q = quote { new { def apply[T](q: Query[T]) = ... } }` by `def q[T] = quote { (q: Query[T] => ... }`")
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
    case q"$o.flatten[$t]($implicitBody)" if is[Option[Any]](o) =>
      OptionFlatten(astParser(o))
    case q"$o.getOrElse[$t]($body)" if is[Option[Any]](o) =>
      OptionGetOrElse(astParser(o), astParser(body))
    case q"$o.flatMap[$t]({($alias) => $body})" if is[Option[Any]](o) =>
      OptionFlatMap(astParser(o), identParser(alias), astParser(body))
    case q"$o.map[$t]({($alias) => $body})" if is[Option[Any]](o) =>
      OptionMap(astParser(o), identParser(alias), astParser(body))
    case q"$o.forall({($alias) => $body})" if is[Option[Any]](o) =>
      if (is[Option[Embedded]](o)) {
        c.fail("Please use Option.exists() instead of Option.forall() with embedded case classes.")
      } else {
        OptionForall(astParser(o), identParser(alias), astParser(body))
      }
    case q"$o.exists({($alias) => $body})" if is[Option[Any]](o) =>
      OptionExists(astParser(o), identParser(alias), astParser(body))
    case q"$o.contains[$t]($body)" if is[Option[Any]](o) =>
      OptionContains(astParser(o), astParser(body))
    case q"$o.isEmpty" if is[Option[Any]](o) =>
      OptionIsEmpty(astParser(o))
    case q"$o.nonEmpty" if is[Option[Any]](o) =>
      OptionNonEmpty(astParser(o))
    case q"$o.isDefined" if is[Option[Any]](o) =>
      OptionIsDefined(astParser(o))
  }

  val traversableOperationParser: Parser[TraversableOperation] = Parser[TraversableOperation] {
    case q"$col.contains($body)" if isBaseType[collection.Map[Any, Any]](col) =>
      MapContains(astParser(col), astParser(body))
    case q"$col.contains($body)" if isBaseType[Set[Any]](col) =>
      SetContains(astParser(col), astParser(body))
    case q"$col.contains[$t]($body)" if is[Seq[Any]](col) =>
      ListContains(astParser(col), astParser(body))
  }

  val propertyParser: Parser[Property] = Parser[Property] {
    case q"$e.get" if is[Option[Any]](e) =>
      c.fail("Option.get is not supported since it's an unsafe operation. Use `forall` or `exists` instead.")
    case q"$e.$property" =>

      val caseAccessors =
        e.tpe.members.collect {
          case m: MethodSymbol if m.isCaseAccessor => m.name
        }.toSet

      if (caseAccessors.nonEmpty && !caseAccessors.contains(property))
        c.fail(s"Can't find case class property: ${property.decodedName.toString}")

      Property(astParser(e), property.decodedName.toString)
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

  private def rejectOptions(a: Tree, b: Tree) = {
    if ((!is[Null](a) && is[Option[_]](a)) || (!is[Null](b) && is[Option[_]](b)))
      c.abort(a.pos, "Can't compare `Option` values since databases have different behavior for null comparison. Use `Option` methods like `forall` and `exists` instead.")
  }

  val equalityOperationParser: Parser[Operation] = Parser[Operation] {
    case q"$a.==($b)" =>
      checkTypes(a, b)
      rejectOptions(a, b)
      BinaryOperation(astParser(a), EqualityOperator.`==`, astParser(b))
    case q"$a.equals($b)" =>
      checkTypes(a, b)
      rejectOptions(a, b)
      BinaryOperation(astParser(a), EqualityOperator.`==`, astParser(b))
    case q"$a.!=($b)" =>
      checkTypes(a, b)
      rejectOptions(a, b)
      BinaryOperation(astParser(a), EqualityOperator.`!=`, astParser(b))
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
    operationParser(t => is[String](t) || is[StringOps](t)) {
      case "+"           => StringOperator.`+`
      case "toUpperCase" => StringOperator.`toUpperCase`
      case "toLowerCase" => StringOperator.`toLowerCase`
      case "toLong"      => StringOperator.`toLong`
      case "toInt"       => StringOperator.`toInt`
      case "startsWith"  => StringOperator.`startsWith`
      case "split"       => StringOperator.`split`
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
      operationParser(is[CoreDsl#Query[Any]](_)) {
        case "isEmpty"  => SetOperator.`isEmpty`
        case "nonEmpty" => SetOperator.`nonEmpty`
      }
    Parser[Operation] {
      case q"$a.contains[$t]($b)" if (is[CoreDsl#Query[Any]])(a) =>
        BinaryOperation(astParser(a), SetOperator.`contains`, astParser(b))
      case unary(op) => op
    }
  }

  private def isBaseType[T](col: Tree)(implicit t: TypeTag[T]) =
    !(col.tpe.baseType(t.tpe.typeSymbol) =:= NoType)

  private def isNumeric[T: WeakTypeTag] =
    c.inferImplicitValue(c.weakTypeOf[Numeric[T]]) != EmptyTree

  private def is[T](tree: Tree)(implicit t: TypeTag[T]) =
    tree.tpe <:< t.tpe

  private def isCaseClass[T: WeakTypeTag] = {
    val symbol = c.weakTypeTag[T].tpe.typeSymbol
    symbol.isClass && symbol.asClass.isCaseClass
  }

  private def firstConstructorParamList[T: WeakTypeTag] = {
    val tpe = c.weakTypeTag[T].tpe
    val paramLists = tpe.decls.collect {
      case m: MethodSymbol if m.isConstructor => m.paramLists.map(_.map(_.name))
    }
    paramLists.toList(0)(0).map(_.toString)
  }

  val valueParser: Parser[Ast] = Parser[Ast] {
    case q"null"                         => NullValue
    case q"scala.None"                   => NullValue
    case q"scala.Option.empty[$t]"       => NullValue
    case q"scala.Some.apply[$t]($v)"     => astParser(v)
    case q"scala.Option.apply[$t]($v)"   => astParser(v)
    case Literal(c.universe.Constant(v)) => Constant(v)
    case q"((..$v))" if (v.size > 1)     => Tuple(v.map(astParser(_)))
    case q"new $ccTerm(..$v)" if (isCaseClass(c.WeakTypeTag(ccTerm.tpe.erasure))) => {
      val values = v.map(astParser(_))
      val params = firstConstructorParamList(c.WeakTypeTag(ccTerm.tpe.erasure))
      CaseClass(params.zip(values))
    }
    case q"(($pack.Predef.ArrowAssoc[$t1]($v1).$arrow[$t2]($v2)))" => Tuple(List(astParser(v1), astParser(v2)))
    case q"io.getquill.dsl.UnlimitedTuple.apply($v)"               => astParser(v)
    case q"io.getquill.dsl.UnlimitedTuple.apply(..$v)"             => Tuple(v.map(astParser(_)))
    case q"$ccCompanion(..$v)" if (
      ccCompanion.tpe != null &&
      ccCompanion.children.length > 0 &&
      isCaseClassCompanion(ccCompanion)
    ) => {
      val values = v.map(astParser(_))
      val params = firstParamList(c.WeakTypeTag(ccCompanion.tpe.erasure))
      CaseClass(params.zip(values))
    }
  }

  private def ifThenSome[T](cond: => Boolean, output: => T): Option[T] =
    if (cond) Some(output) else None

  private def isCaseClassCompanion(ccCompanion: Tree): Boolean = {
    val output = for {
      resultType <- ifThenSome(
        isTypeConstructor(c.WeakTypeTag(ccCompanion.tpe.erasure)),
        resultType(c.WeakTypeTag(ccCompanion.tpe.erasure))
      )
      firstChild <- ifThenSome(
        isCaseClass(c.WeakTypeTag(resultType)) && ccCompanion.children.length > 0,
        ccCompanion.children(0)
      )
      moduleClass <- ifThenSome(
        isModuleClass(c.WeakTypeTag(firstChild.tpe.erasure)),
        asClass(c.WeakTypeTag(firstChild.tpe.erasure))
      )
      // Fix for SI-7567 Ideally this should be
      // moduleClass.companion == resultType.typeSymbol but .companion
      // returns NoSymbol where in a local context (e.g. inside a method).
    } yield (resultType.typeSymbol.name.toTypeName == moduleClass.name.toTypeName)
    output.getOrElse(false)
  }

  private def isTypeConstructor[T: WeakTypeTag] =
    c.weakTypeTag[T].tpe != null && c.weakTypeTag[T].tpe.typeConstructor != NoType

  private def isModuleClass[T: WeakTypeTag] = {
    val typeSymbol = c.weakTypeTag[T].tpe.typeSymbol
    typeSymbol.isClass && typeSymbol.isModuleClass
  }

  private def resultType[T: WeakTypeTag] =
    c.weakTypeTag[T].tpe.resultType

  private def asClass[T: WeakTypeTag] =
    c.weakTypeTag[T].tpe.typeSymbol.asClass

  private def firstParamList[T: WeakTypeTag] = {
    val tpe = c.weakTypeTag[T].tpe
    tpe.paramLists(0).map(_.name.toString)
  }

  val actionParser: Parser[Ast] = Parser[Ast] {
    case q"$query.$method(..$assignments)" if (method.decodedName.toString == "update") =>
      Update(astParser(query), assignments.map(assignmentParser(_)))
    case q"$query.insert(..$assignments)" =>
      Insert(astParser(query), assignments.map(assignmentParser(_)))
    case q"$query.delete" =>
      Delete(astParser(query))
    case q"$action.returning[$r](($alias) => $body)" =>
      Returning(astParser(action), identParser(alias), astParser(body))
    case q"$query.foreach[$t1, $t2](($alias) => $body)($f)" if (is[CoreDsl#Query[Any]](query)) =>
      Foreach(astParser(query), identParser(alias), astParser(body))
  }

  private val assignmentParser: Parser[Assignment] = Parser[Assignment] {
    case q"((${ identParser(i1) }) => $pack.Predef.ArrowAssoc[$t]($prop).$arrow[$v]($value))" =>
      checkTypes(prop, value)
      Assignment(i1, astParser(prop), astParser(value))

    case q"((${ identParser(i1) }, ${ identParser(i2) }) => $pack.Predef.ArrowAssoc[$t]($prop).$arrow[$v]($value))" =>
      checkTypes(prop, value)
      val valueAst = Transform(astParser(value)) {
        case `i1` => OnConflict.Existing(i1)
        case `i2` => OnConflict.Excluded(i2)
      }
      Assignment(i1, astParser(prop), valueAst)
    // Unused, it's here only to make eclipse's presentation compiler happy
    case astParser(ast) => Assignment(Ident("unused"), Ident("unused"), Constant("unused"))
  }

  val conflictParser: Parser[Ast] = Parser[Ast] {
    case q"$query.onConflictIgnore" =>
      OnConflict(astParser(query), OnConflict.NoTarget, OnConflict.Ignore)
    case q"$query.onConflictIgnore(..$targets)" =>
      OnConflict(astParser(query), parseConflictProps(targets), OnConflict.Ignore)

    case q"$query.onConflictUpdate(..$assigns)" =>
      OnConflict(astParser(query), OnConflict.NoTarget, parseConflictAssigns(assigns))
    case q"$query.onConflictUpdate(..$targets)(..$assigns)" =>
      OnConflict(astParser(query), parseConflictProps(targets), parseConflictAssigns(assigns))
  }

  private def parseConflictProps(targets: List[Tree]) = OnConflict.Properties {
    targets.map {
      case q"($e) => $prop" => propertyParser(prop)
      case tree             => c.fail(s"Tree '$tree' can't be parsed as conflict target")
    }
  }

  private def parseConflictAssigns(targets: List[Tree]) =
    OnConflict.Update(targets.map(assignmentParser(_)))

  private def checkTypes(lhs: Tree, rhs: Tree): Unit = {
    def unquoted(tree: Tree) =
      is[CoreDsl#Quoted[Any]](tree) match {
        case false => tree
        case true  => q"unquote($tree)"
      }
    val t = TypeName(c.freshName("T"))
    try c.typecheck(
      q"""
        def apply[$t](lhs: $t)(rhs: $t) = ()
        apply(${unquoted(lhs)})(${unquoted(rhs)})
      """,
      c.TYPEmode
    ) catch {
      case t: TypecheckException => c.error(lhs.pos, t.msg)
    }
    ()
  }
}
