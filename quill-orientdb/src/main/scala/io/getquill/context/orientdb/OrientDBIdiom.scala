package io.getquill.context.orientdb

import io.getquill.norm.{NormalizeCaching, TranspileConfig}
import io.getquill.idiom.StatementInterpolator._
import io.getquill.context.sql.norm._
import io.getquill.ast.{AggregationOperator, External, _}
import io.getquill.context.sql._
import io.getquill.{IdiomContext, NamingStrategy}
import io.getquill.context.{CannotReturn, ExecutionType}
import io.getquill.util.Messages.{fail, trace}
import io.getquill.idiom._
import io.getquill.context.sql.norm.SqlNormalize
import io.getquill.util.{Interleave, Messages}
import io.getquill.context.sql.idiom.VerifySqlQuery
import io.getquill.quat.Quat
import io.getquill.sql.norm.{RemoveExtraAlias, RemoveUnusedSelects}

object OrientDBIdiom extends OrientDBIdiom with CannotReturn

trait OrientDBIdiom extends Idiom {

  override def liftingPlaceholder(index: Int): String = "?"

  override def emptySetContainsToken(field: Token): Token = StringToken(s"$field IS NULL")

  override def prepareForProbing(string: String): String = string

  override def translate(ast: Ast, topLevelQuat: Quat, executionType: ExecutionType, idiomContext: IdiomContext)(
    implicit naming: NamingStrategy
  ): (Ast, Statement, ExecutionType) =
    doTranslate(ast, false, executionType, idiomContext)

  override def translateCached(ast: Ast, topLevelQuat: Quat, executionType: ExecutionType, idiomContext: IdiomContext)(
    implicit naming: NamingStrategy
  ): (Ast, Statement, ExecutionType) =
    doTranslate(ast, true, executionType, idiomContext)

  private def doTranslate(ast: Ast, cached: Boolean, executionType: ExecutionType, idiomContext: IdiomContext)(implicit
    naming: NamingStrategy
  ): (Ast, Statement, ExecutionType) = {
    implicit val implcitIdiomContext: IdiomContext = idiomContext
    val normalizedAst =
      if (cached)
        NormalizeCaching { ast: Ast => SqlNormalize(ast, idiomContext.config) }(ast)
      else SqlNormalize(ast, TranspileConfig.Empty)
    val token =
      normalizedAst match {
        case q: Query =>
          val sql = new SqlQueryApply(idiomContext.traceConfig)(q)
          VerifySqlQuery(sql).map(fail)
          val expanded = ExpandNestedQueries(sql)
          trace("expanded sql")(expanded)
          val refined = if (Messages.pruneColumns) RemoveUnusedSelects(expanded) else expanded
          trace("filtered sql (only used selects)")(refined)
          val cleaned = if (!Messages.alwaysAlias) RemoveExtraAlias(naming)(refined) else refined
          trace("cleaned sql")(cleaned)
          val tokenized = cleaned.token
          trace("tokenized sql")(tokenized)
          tokenized
        case other =>
          other.token
      }

    (normalizedAst, stmt"$token", executionType)
  }

  implicit def astTokenizer(implicit
    strategy: NamingStrategy,
    queryTokenizer: Tokenizer[Query],
    idiomContext: IdiomContext
  ): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case a: Query =>
        new SqlQueryApply(idiomContext.traceConfig)(a).token
      case a: Operation =>
        a.token
      case a: Infix =>
        a.token
      case a: Action =>
        a.token
      case a: Ident =>
        a.token
      case a: ExternalIdent =>
        a.token
      case a: Property =>
        a.token
      case a: Value =>
        a.token
      case a: If =>
        a.token
      case a: External =>
        a.token
      case a: Assignment =>
        a.token
      case a: AssignmentDual =>
        a.token
      case a @ (
            _: Function | _: FunctionApply | _: Dynamic | _: OptionOperation | _: Block | _: Val | _: Ordering |
            _: QuotedReference | _: IterableOperation | _: OnConflict.Excluded | _: OnConflict.Existing
          ) =>
        fail(s"Malformed or unsupported construct: $a.")
    }

  implicit def ifTokenizer(implicit strategy: NamingStrategy, idiomContext: IdiomContext): Tokenizer[If] =
    Tokenizer[If] { case ast: If =>
      def flatten(ast: Ast): (List[(Ast, Ast)], Ast) =
        ast match {
          case If(cond, a, b) =>
            val (l, e) = flatten(b)
            ((cond, a) +: l, e)
          case other =>
            (List(), other)
        }

      val (l, e) = flatten(ast)
      val conditions =
        for ((cond, body) <- l) yield {
          stmt"if(${cond.token}, ${body.token}, ${e.token})"
        }
      conditions.head
    }

  implicit def queryTokenizer(implicit strategy: NamingStrategy, idiomContext: IdiomContext): Tokenizer[Query] =
    Tokenizer[Query] { case q =>
      new SqlQueryApply(idiomContext.traceConfig)(q).token
    }

  implicit def orientDBQueryTokenizer(implicit
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[SqlQuery] = Tokenizer[SqlQuery] {
    case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
      val distinctTokenizer = (if (distinct == DistinctKind.Distinct) "DISTINCT" else "").token

      val selectClause =
        select match {
          case Nil =>
            if (!distinct.isDistinct) stmt"SELECT *"
            else fail("OrientDB DISTINCT with multiple columns is not supported")
          case _ =>
            if (!distinct.isDistinct) stmt"SELECT ${select.token}"
            else if (select.size == 1) stmt"SELECT $distinctTokenizer(${select.token})"
            else fail("OrientDB DISTINCT with multiple columns is not supported")
        }

      val withFrom =
        from match {
          case Nil => selectClause
          case head :: tail =>
            val t = tail.foldLeft(stmt"${head.token}") {
              case (a, b: FlatJoinContext) =>
                stmt"$a ${(b: FromContext).token}"
              case (a, b) =>
                stmt"$a"
            }
            stmt"$selectClause FROM $t"
        }

      val withWhere =
        where match {
          case None => withFrom
          case Some(where) =>
            stmt"$withFrom WHERE ${where.token}"
        }

      val withGroupBy =
        groupBy match {
          case None          => withWhere
          case Some(groupBy) => stmt"$withWhere GROUP BY ${groupBy.token}"
        }

      val withOrderBy =
        orderBy match {
          case Nil     => withGroupBy
          case orderBy => stmt"$withGroupBy ${tokenOrderBy(orderBy)}"
        }
      (limit, offset) match {
        case (None, None)                => withOrderBy
        case (Some(limit), None)         => stmt"$withOrderBy LIMIT ${limit.token}"
        case (Some(limit), Some(offset)) => stmt"$withOrderBy SKIP ${offset.token} LIMIT ${limit.token}"
        case (None, Some(offset))        => stmt"$withOrderBy SKIP ${offset.token}"
      }
    case SetOperationSqlQuery(a, op, b) =>
      val str = f"SELECT $$c LET $$a = (${a.token}), $$b = (${b.token}), $$c = UNIONALL($$a, $$b)"
      str.token
    case _ =>
      fail("Other operators are not supported yet. Please raise a ticket to support more operations")
  }

  implicit def operationTokenizer(implicit
    propertyTokenizer: Tokenizer[Property],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[Operation] = Tokenizer[Operation] {
    case UnaryOperation(op, ast)                               => stmt"${op.token} (${ast.token})"
    case BinaryOperation(a, EqualityOperator.`_==`, NullValue) => stmt"${scopedTokenizer(a)} IS NULL"
    case BinaryOperation(NullValue, EqualityOperator.`_==`, b) => stmt"${scopedTokenizer(b)} IS NULL"
    case BinaryOperation(a, EqualityOperator.`_!=`, NullValue) => stmt"${scopedTokenizer(a)} IS NOT NULL"
    case BinaryOperation(NullValue, EqualityOperator.`_!=`, b) => stmt"${scopedTokenizer(b)} IS NOT NULL"
    case BinaryOperation(a, op @ SetOperator.`contains`, b)    => SetContainsToken(scopedTokenizer(b), op.token, a.token)
    case BinaryOperation(a, op, b) =>
      stmt"${scopedTokenizer(a)} ${op.token} ${scopedTokenizer(b)}"
    case e: FunctionApply => fail(s"Can't translate the ast to sql: '$e'")
  }

  implicit val setOperationTokenizer: Tokenizer[SetOperation] = Tokenizer[SetOperation] {
    case UnionOperation    => stmt"UNION"
    case UnionAllOperation => stmt"UNION ALL"
  }

  protected def tokenOrderBy(
    criteria: List[OrderByCriteria]
  )(implicit strategy: NamingStrategy, idiomContext: IdiomContext) =
    stmt"ORDER BY ${criteria.token}"

  implicit def sourceTokenizer(implicit strategy: NamingStrategy, idiomContext: IdiomContext): Tokenizer[FromContext] =
    Tokenizer[FromContext] {
      case TableContext(name, alias)  => stmt"${name.token}"
      case QueryContext(query, alias) => stmt"(${query.token})"
      case InfixContext(infix, alias) => stmt"(${(infix: Ast).token})"
      case _                          => fail("OrientDB sql doesn't support joins")
    }

  implicit def orderByCriteriaTokenizer(implicit
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[OrderByCriteria] = Tokenizer[OrderByCriteria] {
    case OrderByCriteria(ast, Asc | AscNullsFirst | AscNullsLast)    => stmt"${scopedTokenizer(ast)} ASC"
    case OrderByCriteria(ast, Desc | DescNullsFirst | DescNullsLast) => stmt"${scopedTokenizer(ast)} DESC"
  }

  implicit val unaryOperatorTokenizer: Tokenizer[UnaryOperator] = Tokenizer[UnaryOperator] {
    case StringOperator.`toUpperCase` => stmt"toUpperCase()"
    case StringOperator.`toLowerCase` => stmt"toLowerCase()"
    case _                            => fail("OrientDB sql doesn't support other unary operators")
  }

  implicit val aggregationOperatorTokenizer: Tokenizer[AggregationOperator] = Tokenizer[AggregationOperator] {
    case AggregationOperator.`min`  => stmt"MIN"
    case AggregationOperator.`max`  => stmt"MAX"
    case AggregationOperator.`avg`  => stmt"AVG"
    case AggregationOperator.`sum`  => stmt"SUM"
    case AggregationOperator.`size` => stmt"COUNT"
  }

  implicit val binaryOperatorTokenizer: Tokenizer[BinaryOperator] = Tokenizer[BinaryOperator] {
    case EqualityOperator.`_==` => stmt"="
    case BooleanOperator.`&&`   => stmt"AND"
    case NumericOperator.`>`    => stmt">"
    case NumericOperator.`>=`   => stmt">="
    case NumericOperator.`<`    => stmt"<"
    case NumericOperator.`<=`   => stmt"<="
    case NumericOperator.`+`    => stmt"+"
    case SetOperator.`contains` => stmt"IN"
    case NumericOperator.`*`    => stmt"*"
    case NumericOperator.`-`    => stmt"-"
    case NumericOperator.`/`    => stmt"/"
    case NumericOperator.`%`    => stmt"%"
    case other                  => fail(s"OrientDB QL doesn't support the '$other' operator.")
  }

  implicit def selectValueTokenizer(implicit
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[SelectValue] = {
    def tokenValue(ast: Ast) =
      ast match {
        case Aggregation(op, Ident(_, _)) => stmt"${op.token}(*)"
        case Aggregation(op, _: Query)    => scopedTokenizer(ast)
        case Aggregation(op, ast)         => stmt"${op.token}(${ast.token})"
        case _                            => ast.token
      }
    Tokenizer[SelectValue] {
      case SelectValue(ast, Some(alias), false)    => stmt"${tokenValue(ast)} ${strategy.column(alias).token}"
      case SelectValue(Ident("?", _), None, false) => "?".token
      case SelectValue(ast: Ident, None, false)    => stmt"*" // stmt"${tokenValue(ast)}.*"
      case SelectValue(ast, None, false)           => tokenValue(ast)
      case SelectValue(_, _, true)                 => fail("OrientDB doesn't support `concatMap`")
    }
  }

  implicit def propertyTokenizer(implicit
    valueTokenizer: Tokenizer[Value],
    identTokenizer: Tokenizer[Ident],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[Property] =
    Tokenizer[Property] {
      case Property(ast, "isEmpty")   => stmt"${ast.token} IS NULL"
      case Property(ast, "nonEmpty")  => stmt"${ast.token} IS NOT NULL"
      case Property(ast, "isDefined") => stmt"${ast.token} IS NOT NULL"
      case Property.Opinionated(ast, name, renameable, _) =>
        renameable.fixedOr(name.token)(strategy.column(name).token)
    }

  implicit def valueTokenizer(implicit strategy: NamingStrategy, idiomContext: IdiomContext): Tokenizer[Value] =
    Tokenizer[Value] {
      case Constant(v: String, _) => stmt"'${v.token}'"
      case Constant((), _)        => stmt"1"
      case Constant(v, _)         => stmt"${v.toString.token}"
      case NullValue              => stmt"null"
      case Tuple(values)          => stmt"${values.token}"
      case CaseClass(_, values)   => stmt"${values.map(_._2).token}"
    }

  implicit def infixTokenizer(implicit
    propertyTokenizer: Tokenizer[Property],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[Infix] = Tokenizer[Infix] { case Infix(parts, params, _, _, _) =>
    val pt = parts.map(_.token)
    val pr = params.map(_.token)
    Statement(Interleave(pt, pr))
  }

  implicit def identTokenizer(implicit strategy: NamingStrategy): Tokenizer[Ident] =
    Tokenizer[Ident](e => strategy.default(e.name).token)

  implicit def externalIdentTokenizer(implicit strategy: NamingStrategy): Tokenizer[ExternalIdent] =
    Tokenizer[ExternalIdent](e => strategy.default(e.name).token)

  implicit def assignmentTokenizer(implicit
    propertyTokenizer: Tokenizer[Property],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[Assignment] = Tokenizer[Assignment] { case Assignment(alias, prop, value) =>
    stmt"${prop.token} = ${scopedTokenizer(value)}"
  }

  implicit def assignmentDualTokenizer(implicit
    propertyTokenizer: Tokenizer[Property],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[AssignmentDual] = Tokenizer[AssignmentDual] { case AssignmentDual(alias1, alias2, prop, value) =>
    stmt"${prop.token} = ${scopedTokenizer(value)}"
  }

  implicit def actionTokenizer(implicit strategy: NamingStrategy, idiomContext: IdiomContext): Tokenizer[Action] = {

    implicit def propertyTokenizer: Tokenizer[Property] = Tokenizer[Property] {
      case Property(Property.Opinionated(_, name, renameable, _), "isEmpty") =>
        stmt"${renameable.fixedOr(name.token)(strategy.column(name).token)} IS NULL"
      case Property(Property.Opinionated(_, name, renameable, _), "isDefined") =>
        stmt"${renameable.fixedOr(name.token)(strategy.column(name).token)} IS NOT NULL"
      case Property(Property.Opinionated(_, name, renameable, _), "nonEmpty") =>
        stmt"${renameable.fixedOr(name.token)(strategy.column(name).token)} IS NOT NULL"
      case Property.Opinionated(_, name, renameable, _) => renameable.fixedOr(name.token)(strategy.column(name).token)
    }

    Tokenizer[Action] {
      case Insert(table: Entity, assignments) =>
        val columns = assignments.map(_.property.token)
        val values  = assignments.map(_.value)
        stmt"INSERT INTO ${table.token} (${columns.mkStmt(", ")}) VALUES(${values.map(scopedTokenizer(_)).mkStmt(", ")})"

      case Update(table: Entity, assignments) =>
        stmt"UPDATE ${table.token} SET ${assignments.token}"

      case Update(Filter(table: Entity, x, where), assignments) =>
        stmt"UPDATE ${table.token} SET ${assignments.token} WHERE ${where.token}"

      case Delete(Filter(table: Entity, x, where)) =>
        stmt"DELETE FROM ${table.token} WHERE ${where.token}"

      case Delete(table: Entity) =>
        stmt"DELETE FROM ${table.token}"

      case other =>
        fail(s"Action ast can't be translated to sql: '$other'")
    }
  }

  implicit def entityTokenizer(implicit strategy: NamingStrategy): Tokenizer[Entity] = Tokenizer[Entity] {
    case Entity.Opinionated(name, _, _, renameable) =>
      renameable.fixedOr(name.token)(strategy.table(name).token)
  }

  protected def scopedTokenizer[A <: Ast](ast: A)(implicit token: Tokenizer[A]) =
    ast match {
      case _: Query           => stmt"(${ast.token})"
      case _: BinaryOperation => stmt"(${ast.token})"
      case _: Tuple           => stmt"(${ast.token})"
      case _                  => ast.token
    }
}
