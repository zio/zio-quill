package io.getquill.quotation

import scala.reflect.ClassTag
import io.getquill.ast._
import io.getquill.Embedded
import io.getquill.context._
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages.RichContext
import io.getquill.dsl.{ CoreDsl, QueryDsl, ValueComputation }
import io.getquill.norm.capture.AvoidAliasConflict
import io.getquill.idiom.Idiom

import scala.annotation.tailrec
import scala.collection.immutable.StringOps
import scala.reflect.macros.TypecheckException
import io.getquill.ast.Implicits._
import io.getquill.ast.Renameable.Fixed
import io.getquill.ast.Visibility.{ Hidden, Visible }
import io.getquill.util.Interleave

trait Parsing extends ValueComputation {
  this: Quotation =>

  import c.universe.{ Ident => _, Constant => _, Function => _, If => _, Block => _, _ }

  // Variables that need to be sanitized out in various places due to internal conflicts with the way
  // macros hard handeled in MetaDsl
  private[getquill] val dangerousVariables = Set("v").map(Ident(_))

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
      Entity.Opinionated(name, properties.map(propertyAliasParser(_)), Fixed)

    case q"$pack.impliedQuerySchema[$t](${ name: String }, ..$properties)" =>
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

    case q"$a.value[$t]" if (is[CoreDsl#Query[Any]](a))   => astParser(a)
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
      io.getquill.ast.Nested(astParser(source))

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
    case q"$infix.pure.as[$t]" =>
      combinedInfixParser(true)(infix)
    case q"$infix.as[$t]" =>
      combinedInfixParser(false)(infix)
    case `impureInfixParser`(value) =>
      value
  }

  val impureInfixParser = combinedInfixParser(false)

  def combinedInfixParser(infixIsPure: Boolean): Parser[Ast] = Parser[Ast] {
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
              override def ast = io.getquill.ast.Infix($newParts, $newParams, $infixIsPure)
            }
          """)
        }
      } else
        Infix(parts, params.map(astParser(_)), infixIsPure)
  }

  val functionParser: Parser[Function] = Parser[Function] {
    case q"new { def apply[..$t1](...$params) = $body }" =>
      c.fail("Anonymous classes aren't supported for function declaration anymore. Use a method with a type parameter instead. " +
        "For instance, replace `val q = quote { new { def apply[T](q: Query[T]) = ... } }` by `def q[T] = quote { (q: Query[T] => ... }`")
    case q"(..$params) => $body" => {
      val subtree = Function(params.map(identParser(_)), astParser(body))
      // If there are actions inside the subtree, we need to do some additional sanitizations
      // of the variables so that their content will not collide with code that we have generated.
      if (CollectAst.byType[Action](subtree).nonEmpty)
        AvoidAliasConflict.sanitizeVariables(subtree, dangerousVariables)
      else
        subtree
    }
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

  /**
   * In order to guarentee consistent behavior across multiple databases, we have begun to explicitly to null-check
   * nullable columns that are wrapped inside of `Option[T]` whenever a `Option.map`, `Option.flatMap`, `Option.forall`, and
   * `Option.exists` are used. However, we would like users to be warned that the behavior of improperly structured queries
   * may change as a result of this modification (see #1302 for more details). This method search the subtree of the
   * respective Option methods and creates a warning if any `If(_, _, _)` AST elements are found inside. Since these
   * can be found deeply nested in the AST (e.g. inside of `BinaryOperation` nodes etc...) it is necessary to recursively
   * traverse into the subtree via a stateful transformer in order to discovery this.
   */
  private def warnConditionalsExist(ast: OptionOperation) = {
    def searchSubtreeAndWarn(subtree: Ast, warning: String) = {
      val results = CollectAst.byType[If](subtree)
      if (results.nonEmpty)
        c.info(warning)
    }

    val messageSuffix =
      s"""\nExpressions like Option(if (v == "foo") else "bar").getOrElse("baz") will now work correctly, """ +
        """but expressions that relied on the broken behavior (where "bar" would be returned instead) need to be modified (see the "orNull / getOrNull" section of the documentation of more detail)."""

    ast match {
      case OptionMap(_, _, body) =>
        searchSubtreeAndWarn(body, s"Conditionals inside of Option.map will create a `CASE` statement in order to properly null-check the sub-query: `${ast}`. " + messageSuffix)
      case OptionFlatMap(_, _, body) =>
        searchSubtreeAndWarn(body, s"Conditionals inside of Option.flatMap will create a `CASE` statement in order to properly null-check the sub-query: `${ast}`." + messageSuffix)
      case OptionForall(_, _, body) =>
        searchSubtreeAndWarn(body, s"Conditionals inside of Option.forall will create a null-check statement in order to properly null-check the sub-query: `${ast}`." + messageSuffix)
      case OptionExists(_, _, body) =>
        searchSubtreeAndWarn(body, s"Conditionals inside of Option.exists will create a null-check statement in order to properly null-check the sub-query: `${ast}`." + messageSuffix)
      case _ =>
    }

    ast
  }

  /**
   * Process scala Option-related DSL into AST constructs.
   * In Option[T] if T is a row type (typically a product or instance of Embedded)
   * the it is impossible to physically null check in SQL (e.g. you cannot do
   * `select p.* from People p where p is not null`). However, when a column (or "leaf-type")
   * is encountered, doing a null check during operations such as `map` or `flatMap` is necessary
   * in order for constructs like case statements to work correctly.
   * <br>For example,
   * the statement:
   *
   * `<pre>query[Person].map(_.name + " S.r.").getOrElse("unknown")</pre>`
   * needs to become:
   *
   * `<pre>select case when p.name is not null then p.name + 'S.r' else 'unknown' end from ...</pre>`
   * Otherwise it will not function correctly. This latter kind of operation is involves null checking
   * versus the former (i.e. the table-select example) which cannot, and is therefore called "Unchecked."
   *
   * The `isOptionRowType` method checks if the type-parameter of an option is a Product. The isOptionEmbedded
   * checks if it an embedded object.
   */
  val optionOperationParser: Parser[OptionOperation] = Parser[OptionOperation] {

    case q"$o.flatMap[$t]({($alias) => $body})" if is[Option[Any]](o) =>
      if (isOptionRowType(o) || isOptionEmbedded(o))
        OptionTableFlatMap(astParser(o), identParser(alias), astParser(body))
      else
        warnConditionalsExist(OptionFlatMap(astParser(o), identParser(alias), astParser(body)))

    case q"$o.map[$t]({($alias) => $body})" if is[Option[Any]](o) =>
      if (isOptionRowType(o) || isOptionEmbedded(o))
        OptionTableMap(astParser(o), identParser(alias), astParser(body))
      else
        warnConditionalsExist(OptionMap(astParser(o), identParser(alias), astParser(body)))

    case q"$o.exists({($alias) => $body})" if is[Option[Any]](o) =>
      if (isOptionRowType(o) || isOptionEmbedded(o))
        OptionTableExists(astParser(o), identParser(alias), astParser(body))
      else
        warnConditionalsExist(OptionExists(astParser(o), identParser(alias), astParser(body)))

    case q"$o.forall({($alias) => $body})" if is[Option[Any]](o) =>
      if (isOptionEmbedded(o)) {
        c.fail("Please use Option.exists() instead of Option.forall() with embedded case classes and other row-objects.")
      } else if (isOptionRowType(o)) {
        OptionTableForall(astParser(o), identParser(alias), astParser(body))
      } else {
        warnConditionalsExist(OptionForall(astParser(o), identParser(alias), astParser(body)))
      }

    // For column values
    case q"$o.flatten[$t]($implicitBody)" if is[Option[Any]](o) =>
      OptionFlatten(astParser(o))
    case q"$o.getOrElse[$t]($body)" if is[Option[Any]](o) =>
      OptionGetOrElse(astParser(o), astParser(body))
    case q"$o.contains[$t]($body)" if is[Option[Any]](o) =>
      OptionContains(astParser(o), astParser(body))
    case q"$o.isEmpty" if is[Option[Any]](o) =>
      OptionIsEmpty(astParser(o))
    case q"$o.nonEmpty" if is[Option[Any]](o) =>
      OptionNonEmpty(astParser(o))
    case q"$o.isDefined" if is[Option[Any]](o) =>
      OptionIsDefined(astParser(o))

    case q"$o.orNull[$t]($v)" if is[Option[Any]](o) =>
      OptionOrNull(astParser(o))
    case q"$prefix.NullableColumnExtensions[$nt]($o).getOrNull" if is[Option[Any]](o) =>
      OptionGetOrNull(astParser(o))
  }

  val traversableOperationParser: Parser[IterableOperation] = Parser[IterableOperation] {
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

      val visibility = {
        val tpe = c.typecheck(q"$e.$property")
        val innerParam = innerOptionParam(q"$tpe".tpe, None)
        if (is[Embedded](q"$innerParam")) Hidden
        else Visible
      }

      Property.Opinionated(astParser(e), property.decodedName.toString,
        Renameable.neutral, //Renameability of the property is determined later in the RenameProperties normalization phase
        visibility // Visibility is decided here.
      )
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
      case q"$a.${ operator(op: BinaryOperator) }($b)" if cond(a) => // test on right tree is not necessary and broken in 2.13
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

  sealed trait EqualityBehavior { def operator: BinaryOperator }
  case object Equal extends EqualityBehavior { def operator: BinaryOperator = EqualityOperator.`==` }
  case object NotEqual extends EqualityBehavior { def operator: BinaryOperator = EqualityOperator.`!=` }

  /** Do equality checking on the database level with the ansi-style truth table (i.e. always false if one side is null) */
  private def equalityWithInnerTypechecksAnsi(left: Tree, right: Tree)(equalityBehavior: EqualityBehavior) = {
    val (leftIsOptional, rightIsOptional) = checkInnerTypes(left, right, AllowInnerCompare)
    val a = astParser(left)
    val b = astParser(right)
    val comparison = BinaryOperation(a, equalityBehavior.operator, b)
    (leftIsOptional, rightIsOptional) match {
      case (true, true)   => OptionIsDefined(a) +&&+ OptionIsDefined(b) +&&+ comparison
      case (true, false)  => OptionIsDefined(a) +&&+ comparison
      case (false, true)  => OptionIsDefined(b) +&&+ comparison
      case (false, false) => comparison
    }
  }

  /** Do equality checking on the database level with the same truth-table as idiomatic scala */
  private def equalityWithInnerTypechecksIdiomatic(left: Tree, right: Tree)(equalityBehavior: EqualityBehavior) = {
    val (leftIsOptional, rightIsOptional) = checkInnerTypes(left, right, ForbidInnerCompare)
    val a = astParser(left)
    val b = astParser(right)
    val comparison = BinaryOperation(a, equalityBehavior.operator, b)
    (leftIsOptional, rightIsOptional, equalityBehavior) match {
      // == two optional things. Either they are both null or they are both defined and the same
      case (true, true, Equal)    => (OptionIsEmpty(a) +&&+ OptionIsEmpty(b)) +||+ (OptionIsDefined(a) +&&+ OptionIsDefined(b) +&&+ comparison)
      // != two optional things. Either one is null and the other isn't. Or they are both defined and have different values
      case (true, true, NotEqual) => (OptionIsDefined(a) +&&+ OptionIsEmpty(b)) +||+ (OptionIsEmpty(a) +&&+ OptionIsDefined(b)) +||+ comparison
      // No additional logic when both sides are defined
      case (false, false, _)      => comparison
      // Comparing an optional object with a non-optional object is not allowed when using scala-idiomatic optional behavior
      case (lop, rop, _) => {
        val lopString = (if (lop) "Optional" else "Non-Optional") + s" ${left}}"
        val ropString = (if (rop) "Optional" else "Non-Optional") + s" ${right}}"
        c.abort(left.pos, s"Cannot compare ${lopString} with ${ropString} using operator ${equalityBehavior.operator}");
      }
    }
  }

  val equalityOperationParser: Parser[Operation] = Parser[Operation] {
    case q"$a.==($b)" =>
      equalityWithInnerTypechecksIdiomatic(a, b)(Equal)
    case q"$a.equals($b)" =>
      equalityWithInnerTypechecksIdiomatic(a, b)(Equal)
    case q"$a.!=($b)" =>
      equalityWithInnerTypechecksIdiomatic(a, b)(NotEqual)

    case q"$pack.extras.NumericOptionOps[$t]($a)($imp).===[$q]($b)($imp2)" =>
      equalityWithInnerTypechecksAnsi(a, b)(Equal)
    case q"$pack.extras.NumericRegOps[$t]($a)($imp).===[$q]($b)($imp2)" =>
      equalityWithInnerTypechecksAnsi(a, b)(Equal)
    case q"$pack.extras.NumericOptionOps[$t]($a)($imp).=!=[$q]($b)($imp2)" =>
      equalityWithInnerTypechecksAnsi(a, b)(NotEqual)
    case q"$pack.extras.NumericRegOps[$t]($a)($imp).=!=[$q]($b)($imp2)" =>
      equalityWithInnerTypechecksAnsi(a, b)(NotEqual)

    case q"$pack.extras.OptionOps[$t]($a).===($b)" =>
      equalityWithInnerTypechecksAnsi(a, b)(Equal)
    case q"$pack.extras.RegOps[$t]($a).===($b)" =>
      equalityWithInnerTypechecksAnsi(a, b)(Equal)
    case q"$pack.extras.OptionOps[$t]($a).=!=($b)" =>
      equalityWithInnerTypechecksAnsi(a, b)(NotEqual)
    case q"$pack.extras.RegOps[$t]($a).=!=($b)" =>
      equalityWithInnerTypechecksAnsi(a, b)(NotEqual)
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

  private def isTypeCaseClass(tpe: Type) = {
    val symbol = tpe.typeSymbol
    symbol.isClass && symbol.asClass.isCaseClass
  }

  private def isTypeTuple(tpe: Type) =
    tpe.typeSymbol.fullName startsWith "scala.Tuple"

  /**
   * Need special handling to check if a type is null since need to check if it's Option, Some or None. Don't want
   * to use `<:<` since that would also match things like `Nothing` and `Null`.
   */
  def isOptionType(tpe: Type) = {
    val era = tpe.erasure
    era =:= typeOf[Option[Any]] || era =:= typeOf[Some[Any]] || era =:= typeOf[None.type]
  }

  object ClassTypeRefMatch {
    def unapply(tpe: Type) = tpe match {
      case TypeRef(_, cls, args) if (cls.isClass) => Some((cls.asClass, args))
      case _                                      => None
    }
  }

  /**
   * Recursively traverse an `Option[T]` or `Option[Option[T]]`, or `Option[Option[Option[T]]]` etc...
   * until we find the `T`. Stop at a specified depth.
   */
  @tailrec
  private def innerOptionParam(tpe: Type, maxDepth: Option[Int]): Type = tpe match {
    // If it's a ref-type and an Option, pull out the argument
    case TypeRef(_, cls, List(arg)) if (cls.isClass && cls.asClass.fullName == "scala.Option") && maxDepth.forall(_ > 0) =>
      innerOptionParam(arg, maxDepth.map(_ - 1))
    // If it's not a ref-type but an Option, convert to a ref-type and reprocess
    // also since Nothing is a subtype of everything need to know to stop searching once Nothing
    // has been reached (since we have not gone inside anything, do not decrement the depth here).
    case _ if (isOptionType(tpe) && !(tpe =:= typeOf[Nothing])) && maxDepth.forall(_ > 0) =>
      innerOptionParam(tpe.baseType(typeOf[Option[Any]].typeSymbol), maxDepth)
    // Otherwise we have gotten to the actual type inside the nesting. Check what it is.
    case other => other
  }

  private def isOptionEmbedded(tree: Tree) = {
    val param = innerOptionParam(tree.tpe, None)
    param <:< typeOf[Embedded]
  }

  private def isOptionRowType(tree: Tree) = {
    val param = innerOptionParam(tree.tpe, None)
    isTypeCaseClass(param) || isTypeTuple(param)
  }

  private def isCaseClass[T: WeakTypeTag] =
    isTypeCaseClass(c.weakTypeTag[T].tpe)

  private def firstConstructorParamList[T: WeakTypeTag] = {
    val tpe = c.weakTypeTag[T].tpe
    val paramLists = tpe.decls.collect {
      case m: MethodSymbol if m.isConstructor => m.paramLists.map(_.map(_.name))
    }
    paramLists.toList(0)(0).map(_.toString)
  }

  val valueParser: Parser[Ast] = Parser[Ast] {
    case q"null"                         => NullValue
    case q"scala.Some.apply[$t]($v)"     => OptionSome(astParser(v))
    case q"scala.Option.apply[$t]($v)"   => OptionApply(astParser(v))
    case q"scala.None"                   => OptionNone
    case q"scala.Option.empty[$t]"       => OptionNone
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

  private[getquill] def currentIdiom: Option[Type] = {
    c.prefix.tree.tpe
      .baseClasses
      .flatMap { baseClass =>
        val baseClassTypeArgs = c.prefix.tree.tpe.baseType(baseClass).typeArgs
        baseClassTypeArgs.find { typeArg =>
          typeArg <:< typeOf[Idiom]
        }
      }
      .headOption
  }

  private[getquill] def idiomReturnCapability: ReturningCapability = {
    val returnAfterInsertType =
      currentIdiom
        .toSeq
        .flatMap(_.members)
        .collect {
          case ms: MethodSymbol if (ms.name.toString == "idiomReturningCapability") => Some(ms.returnType)
        }
        .headOption
        .flatten

    returnAfterInsertType match {
      case Some(returnType) if (returnType =:= typeOf[ReturningClauseSupported]) => ReturningClauseSupported
      case Some(returnType) if (returnType =:= typeOf[OutputClauseSupported]) => OutputClauseSupported
      case Some(returnType) if (returnType =:= typeOf[ReturningSingleFieldSupported]) => ReturningSingleFieldSupported
      case Some(returnType) if (returnType =:= typeOf[ReturningMultipleFieldSupported]) => ReturningMultipleFieldSupported
      case Some(returnType) if (returnType =:= typeOf[ReturningNotSupported]) => ReturningNotSupported
      // Since most SQL Dialects support returning a single field (that is auto-incrementing) allow a return
      // of a single field in the case that a dialect is not actually specified. E.g. when SqlContext[_, _]
      // is used to define `returning` clauses.
      case other => ReturningSingleFieldSupported
    }
  }

  implicit class InsertReturnCapabilityExtension(capability: ReturningCapability) {
    def verifyAst(returnBody: Ast) = capability match {
      case OutputClauseSupported => returnBody match {
        case _: Query =>
          c.fail(s"${currentIdiom.map(n => s"The dialect ${n} does").getOrElse("Unspecified dialects do")} not allow queries in 'returning' clauses.")
        case _ =>
      }
      case ReturningClauseSupported =>
      // Only .returning(r => r.prop) or .returning(r => OneElementCaseClass(r.prop1..., propN)) or .returning(r => (r.prop1..., propN)) (well actually it's prop22) is allowed.
      case ReturningMultipleFieldSupported =>
        returnBody match {
          case CaseClass(list) if (list.forall {
            case (_, Property(_, _)) => true
            case _                   => false
          }) =>
          case Tuple(list) if (list.forall {
            case Property(_, _) => true
            case _              => false
          }) =>
          case Property(_, _) =>
          case other =>
            c.fail(s"${currentIdiom.map(n => s"The dialect ${n} only allows").getOrElse("Unspecified dialects only allow")} single a single property or multiple properties in case classes / tuples in 'returning' clauses ${other}.")
        }
      // Only .returning(r => r.prop) or .returning(r => OneElementCaseClass(r.prop)) is allowed.
      case ReturningSingleFieldSupported =>
        returnBody match {
          case Property(_, _) =>
          case other =>
            c.fail(s"${currentIdiom.map(n => s"The dialect ${n} only allows").getOrElse("Unspecified dialects only allow")} single, auto-incrementing columns in 'returning' clauses.")
        }
      // This is not actually the case for unspecified dialects (e.g. when doing `returning` from `SqlContext[_, _]` but error message
      // says what it would say if either case happened. Otherwise doing currentIdiom.get would be allowed which is bad practice.
      case ReturningNotSupported =>
        c.fail(s"${currentIdiom.map(n => s"The dialect ${n} does").getOrElse("Unspecified dialects do")} not allow 'returning' clauses.")
    }
  }

  val actionParser: Parser[Ast] = Parser[Ast] {
    case q"$query.$method(..$assignments)" if (method.decodedName.toString == "update") =>
      Update(astParser(query), assignments.map(assignmentParser(_)))
    case q"$query.insert(..$assignments)" =>
      Insert(astParser(query), assignments.map(assignmentParser(_)))
    case q"$query.delete" =>
      Delete(astParser(query))
    case q"$action.returning[$r]" =>
      c.fail(s"A 'returning' clause must have arguments.")
    case q"$action.returning[$r](($alias) => $body)" =>
      val ident = identParser(alias)
      val bodyAst = reprocessReturnClause(ident, astParser(body), action)
      // Verify that the idiom supports this type of returning clause
      idiomReturnCapability match {
        case ReturningMultipleFieldSupported | ReturningClauseSupported | OutputClauseSupported =>
        case ReturningSingleFieldSupported =>
          c.fail(s"The 'returning' clause is not supported by the ${currentIdiom.getOrElse("specified")} idiom. Use 'returningGenerated' instead.")
        case ReturningNotSupported =>
          c.fail(s"The 'returning' or 'returningGenerated' clauses are not supported by the ${currentIdiom.getOrElse("specified")} idiom.")
      }
      // Verify that the AST in the returning-body is valid
      idiomReturnCapability.verifyAst(bodyAst)
      Returning(astParser(action), ident, bodyAst)

    case q"$action.returningGenerated[$r](($alias) => $body)" =>
      val ident = identParser(alias)
      val bodyAst = reprocessReturnClause(ident, astParser(body), action)
      // Verify that the idiom supports this type of returning clause
      idiomReturnCapability match {
        case ReturningNotSupported =>
          c.fail(s"The 'returning' or 'returningGenerated' clauses are not supported by the ${currentIdiom.getOrElse("specified")} idiom.")
        case _ =>
      }
      // Verify that the AST in the returning-body is valid
      idiomReturnCapability.verifyAst(bodyAst)
      ReturningGenerated(astParser(action), ident, bodyAst)

    case q"$query.foreach[$t1, $t2](($alias) => $body)($f)" if (is[CoreDsl#Query[Any]](query)) =>
      // If there are actions inside the subtree, we need to do some additional sanitizations
      // of the variables so that their content will not collide with code that we have generated.
      AvoidAliasConflict.sanitizeVariables(Foreach(astParser(query), identParser(alias), astParser(body)), dangerousVariables)
  }

  /**
   * In situations where the a `.returning` clause returns the initial record i.e. `.returning(r => r)`,
   * we need to expand out the record into it's fields i.e. `.returning(r => (r.foo, r.bar))`
   * otherwise the tokenizer would be force to pass `RETURNING *` to the SQL which is a problem
   * because the fields inside of the record could arrive out of order in the result set
   * (e.g. arrive as `r.bar, r.foo`). Use use the value/flatten methods in order to expand
   * the case-class out into fields.
   */
  private def reprocessReturnClause(ident: Ident, originalBody: Ast, action: Tree) = {
    val actionType = typecheckUnquoted(action)

    (ident == originalBody, actionType.tpe) match {
      // Note, tuples are also case classes so this also matches for tuples
      case (true, ClassTypeRefMatch(cls, List(arg))) if (cls == asClass[QueryDsl#Insert[_]] || cls == asClass[QueryDsl#Update[_]]) && isTypeCaseClass(arg) =>

        val elements = flatten(q"${TermName(ident.name)}", value("Decoder", arg))
        if (elements.size == 0) c.fail("Case class in the 'returning' clause has no values")

        // Create an intermediate scala API that can then be parsed into clauses. This needs to be
        // typechecked first in order to function properly.
        val tpe = c.typecheck(
          q"((${TermName(ident.name)}:$arg) => io.getquill.dsl.UnlimitedTuple.apply(..$elements))"
        )
        val newBody =
          tpe match {
            case q"(($newAlias) => $newBody)" => newBody
            case _                            => c.fail("Could not process whole-record 'returning' clause. Consider trying to return individual columns.")
          }
        astParser(newBody)

      case (true, _) =>
        c.fail("Could not process whole-record 'returning' clause. Consider trying to return individual columns.")

      case _ =>
        originalBody
    }
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

  trait OptionCheckBehavior
  /** Allow T == Option[T] comparison **/
  case object AllowInnerCompare extends OptionCheckBehavior
  /** Forbid T == Option[T] comparison **/
  case object ForbidInnerCompare extends OptionCheckBehavior

  /**
   * Type-check two trees, if one of them has optionals, go into the optionals to find the root types
   * in each of them. Then compare the types that are inside. If they are not compareable, abort the build.
   * Otherwise return type of which side (or both) has the optional. In order to do the actual comparison,
   * the 'weak conformance' operator is used and a subclass is allowed on either side of the `==`. Weak
   * conformance is necessary so that Longs can be compared to Ints etc...
   */
  private def checkInnerTypes(lhs: Tree, rhs: Tree, optionCheckBehavior: OptionCheckBehavior): (Boolean, Boolean) = {
    val leftType = typecheckUnquoted(lhs).tpe
    val rightType = typecheckUnquoted(rhs).tpe
    val leftInner = innerOptionParam(leftType, Some(1))
    val rightInner = innerOptionParam(rightType, Some(1))
    val leftIsOptional = isOptionType(leftType) && !is[Nothing](lhs)
    val rightIsOptional = isOptionType(rightType) && !is[Nothing](rhs)
    val typesMatch = matchTypes(rightInner, leftInner)

    optionCheckBehavior match {
      case AllowInnerCompare if typesMatch =>
        (leftIsOptional, rightIsOptional)
      case ForbidInnerCompare if ((leftIsOptional && rightIsOptional) || (!leftIsOptional && !rightIsOptional)) && typesMatch =>
        (leftIsOptional, rightIsOptional)
      case _ =>
        if (leftIsOptional || rightIsOptional)
          c.abort(lhs.pos, s"${leftType.widen} == ${rightType.widen} is not allowed since ${leftInner.widen}, ${rightInner.widen} are different types.")
        else
          c.abort(lhs.pos, s"${leftType.widen} == ${rightType.widen} is not allowed since they are different types.")
    }
  }

  private def matchTypes(rightInner: Type, leftInner: Type): Boolean = {
    (rightInner.`weak_<:<`(leftInner) ||
      rightInner.widen.`weak_<:<`(leftInner.widen) ||
      leftInner.`weak_<:<`(rightInner) ||
      leftInner.widen.`weak_<:<`(rightInner.widen))
  }

  private def typecheckUnquoted(tree: Tree): Tree = {
    def unquoted(maybeQuoted: Tree) =
      is[CoreDsl#Quoted[Any]](maybeQuoted) match {
        case false => maybeQuoted
        case true  => q"unquote($maybeQuoted)"
      }
    val t = TypeName(c.freshName("T"))
    try
      c.typecheck(unquoted(tree), c.TYPEmode)
    catch {
      case t: TypecheckException => c.abort(tree.pos, t.msg)
    }
  }

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
