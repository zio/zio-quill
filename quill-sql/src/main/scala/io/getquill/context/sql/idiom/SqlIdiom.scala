package io.getquill.context.sql.idiom

import com.github.vertical_blank.sqlformatter.scala.SqlFormatter
import io.getquill.NamingStrategy
import io.getquill.ast.BooleanOperator._
import io.getquill.ast.Renameable.Fixed
import io.getquill.ast.Visibility.Hidden
import io.getquill.ast._
import io.getquill.context.sql._
import io.getquill.context.sql.norm._
import io.getquill.context.{ OutputClauseSupported, ReturningCapability, ReturningClauseSupported }
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom._
import io.getquill.norm.ConcatBehavior.AnsiConcat
import io.getquill.norm.EqualityBehavior.AnsiEquality
import io.getquill.norm.{ ConcatBehavior, EqualityBehavior, ExpandReturning }
import io.getquill.util.Interleave
import io.getquill.util.Messages.{ fail, trace }

trait SqlIdiom extends Idiom {

  override def prepareForProbing(string: String): String

  protected def concatBehavior: ConcatBehavior = AnsiConcat
  protected def equalityBehavior: EqualityBehavior = AnsiEquality

  protected def actionAlias: Option[Ident] = None

  override def format(queryString: String): String = SqlFormatter.format(queryString)

  def querifyAst(ast: Ast) = SqlQuery(ast)

  override def translate(ast: Ast)(implicit naming: NamingStrategy) = {
    val normalizedAst = SqlNormalize(ast, concatBehavior, equalityBehavior)

    implicit val tokernizer = defaultTokenizer

    val token =
      normalizedAst match {
        case q: Query =>
          val sql = querifyAst(q)
          trace("sql")(sql)
          VerifySqlQuery(sql).map(fail)
          val expanded = new ExpandNestedQueries(naming)(sql, List())
          trace("expanded sql")(expanded)
          val tokenized = expanded.token
          trace("tokenized sql")(tokenized)
          tokenized
        case other =>
          other.token
      }

    (normalizedAst, stmt"$token")
  }

  def defaultTokenizer(implicit naming: NamingStrategy): Tokenizer[Ast] =
    new Tokenizer[Ast] {
      private val stableTokenizer = astTokenizer(this, naming)

      def token(v: Ast) = stableTokenizer.token(v)
    }

  def astTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case a: Query           => SqlQuery(a).token
      case a: Operation       => a.token
      case a: Infix           => a.token
      case a: Action          => a.token
      case a: Ident           => a.token
      case a: ExternalIdent   => a.token
      case a: Property        => a.token
      case a: Value           => a.token
      case a: If              => a.token
      case a: Lift            => a.token
      case a: Assignment      => a.token
      case a: OptionOperation => a.token
      case a @ (
        _: Function | _: FunctionApply | _: Dynamic | _: OptionOperation | _: Block |
        _: Val | _: Ordering | _: QuotedReference | _: IterableOperation | _: OnConflict.Excluded | _: OnConflict.Existing
        ) =>
        fail(s"Malformed or unsupported construct: $a.")
    }

  implicit def ifTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[If] = Tokenizer[If] {
    case ast: If =>
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
          stmt"WHEN ${cond.token} THEN ${body.token}"
        }
      stmt"CASE ${conditions.mkStmt(" ")} ELSE ${e.token} END"
  }

  def concatFunction: String

  protected def tokenizeGroupBy(values: Ast)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Token =
    values.token

  protected class FlattenSqlQueryTokenizerHelper(q: FlattenSqlQuery)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy) {
    import q._

    def distinctTokenizer = (if (distinct) "DISTINCT " else "").token

    def withDistinct =
      select match {
        case Nil => stmt"$distinctTokenizer*"
        case _   => stmt"$distinctTokenizer${select.token}"
      }

    def withFrom =
      from match {
        case Nil => withDistinct
        case head :: tail =>
          val t = tail.foldLeft(stmt"${head.token}") {
            case (a, b: FlatJoinContext) =>
              stmt"$a ${(b: FromContext).token}"
            case (a, b) =>
              stmt"$a, ${b.token}"
          }

          stmt"$withDistinct FROM $t"
      }

    def withWhere =
      where match {
        case None        => withFrom
        case Some(where) => stmt"$withFrom WHERE ${where.token}"
      }
    def withGroupBy =
      groupBy match {
        case None          => withWhere
        case Some(groupBy) => stmt"$withWhere GROUP BY ${tokenizeGroupBy(groupBy)}"
      }
    def withOrderBy =
      orderBy match {
        case Nil     => withGroupBy
        case orderBy => stmt"$withGroupBy ${tokenOrderBy(orderBy)}"
      }
    def withLimitOffset = limitOffsetToken(withOrderBy).token((limit, offset))

    def apply = stmt"SELECT $withLimitOffset"
  }

  implicit def sqlQueryTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[SqlQuery] = Tokenizer[SqlQuery] {
    case q: FlattenSqlQuery =>
      new FlattenSqlQueryTokenizerHelper(q).apply
    case SetOperationSqlQuery(a, op, b) =>
      stmt"(${a.token}) ${op.token} (${b.token})"
    case UnaryOperationSqlQuery(op, q) =>
      stmt"SELECT ${op.token} (${q.token})"
  }

  protected def tokenizeColumn(strategy: NamingStrategy, column: String, renameable: Renameable) =
    renameable match {
      case Fixed => column
      case _     => strategy.column(column)
    }

  protected def tokenizeTable(strategy: NamingStrategy, table: String, renameable: Renameable) =
    renameable match {
      case Fixed => table
      case _     => strategy.table(table)
    }

  protected def tokenizeAlias(strategy: NamingStrategy, table: String) =
    strategy.default(table)

  implicit def selectValueTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[SelectValue] = {

    def tokenizer(implicit astTokenizer: Tokenizer[Ast]) =
      Tokenizer[SelectValue] {
        case SelectValue(ast, Some(alias), false) => {
          stmt"${ast.token} AS ${alias.token}"
        }
        case SelectValue(ast, Some(alias), true) => stmt"${concatFunction.token}(${ast.token}) AS ${alias.token}"
        case selectValue =>
          val value =
            selectValue match {
              case SelectValue(Ident("?"), _, _)  => "?".token
              case SelectValue(Ident(name), _, _) => stmt"${strategy.default(name).token}.*"
              case SelectValue(ast, _, _)         => ast.token
            }
          selectValue.concat match {
            case true  => stmt"${concatFunction.token}(${value.token})"
            case false => value
          }
      }

    val customAstTokenizer =
      Tokenizer.withFallback[Ast](SqlIdiom.this.astTokenizer(_, strategy)) {
        case Aggregation(op, Ident(_) | Tuple(_)) => stmt"${op.token}(*)"
        case Aggregation(op, Distinct(ast))       => stmt"${op.token}(DISTINCT ${ast.token})"
        case ast @ Aggregation(op, _: Query)      => scopedTokenizer(ast)
        case Aggregation(op, ast)                 => stmt"${op.token}(${ast.token})"
      }

    tokenizer(customAstTokenizer)
  }

  implicit def operationTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Operation] = Tokenizer[Operation] {
    case UnaryOperation(op, ast)                              => stmt"${op.token} (${ast.token})"
    case BinaryOperation(a, EqualityOperator.`==`, NullValue) => stmt"${scopedTokenizer(a)} IS NULL"
    case BinaryOperation(NullValue, EqualityOperator.`==`, b) => stmt"${scopedTokenizer(b)} IS NULL"
    case BinaryOperation(a, EqualityOperator.`!=`, NullValue) => stmt"${scopedTokenizer(a)} IS NOT NULL"
    case BinaryOperation(NullValue, EqualityOperator.`!=`, b) => stmt"${scopedTokenizer(b)} IS NOT NULL"
    case BinaryOperation(a, StringOperator.`startsWith`, b)   => stmt"${scopedTokenizer(a)} LIKE (${(BinaryOperation(b, StringOperator.`+`, Constant("%")): Ast).token})"
    case BinaryOperation(a, op @ StringOperator.`split`, b)   => stmt"${op.token}(${scopedTokenizer(a)}, ${scopedTokenizer(b)})"
    case BinaryOperation(a, op @ SetOperator.`contains`, b)   => SetContainsToken(scopedTokenizer(b), op.token, a.token)
    case BinaryOperation(a, op @ `&&`, b) => (a, b) match {
      case (BinaryOperation(_, `||`, _), BinaryOperation(_, `||`, _)) => stmt"${scopedTokenizer(a)} ${op.token} ${scopedTokenizer(b)}"
      case (BinaryOperation(_, `||`, _), _) => stmt"${scopedTokenizer(a)} ${op.token} ${b.token}"
      case (_, BinaryOperation(_, `||`, _)) => stmt"${a.token} ${op.token} ${scopedTokenizer(b)}"
      case _ => stmt"${a.token} ${op.token} ${b.token}"
    }
    case BinaryOperation(a, op @ `||`, b) => stmt"${a.token} ${op.token} ${b.token}"
    case BinaryOperation(a, op, b)        => stmt"${scopedTokenizer(a)} ${op.token} ${scopedTokenizer(b)}"
    case e: FunctionApply                 => fail(s"Can't translate the ast to sql: '$e'")
  }

  implicit def optionOperationTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[OptionOperation] = Tokenizer[OptionOperation] {
    case OptionIsEmpty(ast)   => stmt"${ast.token} IS NULL"
    case OptionNonEmpty(ast)  => stmt"${ast.token} IS NOT NULL"
    case OptionIsDefined(ast) => stmt"${ast.token} IS NOT NULL"
    case other                => fail(s"Malformed or unsupported construct: $other.")
  }

  implicit val setOperationTokenizer: Tokenizer[SetOperation] = Tokenizer[SetOperation] {
    case UnionOperation    => stmt"UNION"
    case UnionAllOperation => stmt"UNION ALL"
  }

  protected def limitOffsetToken(query: Statement)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy) =
    Tokenizer[(Option[Ast], Option[Ast])] {
      case (None, None)                => query
      case (Some(limit), None)         => stmt"$query LIMIT ${limit.token}"
      case (Some(limit), Some(offset)) => stmt"$query LIMIT ${limit.token} OFFSET ${offset.token}"
      case (None, Some(offset))        => stmt"$query OFFSET ${offset.token}"
    }

  protected def tokenOrderBy(criterias: List[OrderByCriteria])(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy) =
    stmt"ORDER BY ${criterias.token}"

  implicit def sourceTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[FromContext] = Tokenizer[FromContext] {
    case TableContext(name, alias)  => stmt"${name.token} ${tokenizeAlias(strategy, alias).token}"
    case QueryContext(query, alias) => stmt"(${query.token}) AS ${tokenizeAlias(strategy, alias).token}"
    case InfixContext(infix, alias) => stmt"(${(infix: Ast).token}) AS ${strategy.default(alias).token}"
    case JoinContext(t, a, b, on)   => stmt"${a.token} ${t.token} ${b.token} ON ${on.token}"
    case FlatJoinContext(t, a, on)  => stmt"${t.token} ${a.token} ON ${on.token}"
  }

  implicit val joinTypeTokenizer: Tokenizer[JoinType] = Tokenizer[JoinType] {
    case InnerJoin => stmt"INNER JOIN"
    case LeftJoin  => stmt"LEFT JOIN"
    case RightJoin => stmt"RIGHT JOIN"
    case FullJoin  => stmt"FULL JOIN"
  }

  implicit def orderByCriteriaTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[OrderByCriteria] = Tokenizer[OrderByCriteria] {
    case OrderByCriteria(ast, Asc)            => stmt"${scopedTokenizer(ast)} ASC"
    case OrderByCriteria(ast, Desc)           => stmt"${scopedTokenizer(ast)} DESC"
    case OrderByCriteria(ast, AscNullsFirst)  => stmt"${scopedTokenizer(ast)} ASC NULLS FIRST"
    case OrderByCriteria(ast, DescNullsFirst) => stmt"${scopedTokenizer(ast)} DESC NULLS FIRST"
    case OrderByCriteria(ast, AscNullsLast)   => stmt"${scopedTokenizer(ast)} ASC NULLS LAST"
    case OrderByCriteria(ast, DescNullsLast)  => stmt"${scopedTokenizer(ast)} DESC NULLS LAST"
  }

  implicit val unaryOperatorTokenizer: Tokenizer[UnaryOperator] = Tokenizer[UnaryOperator] {
    case NumericOperator.`-`          => stmt"-"
    case BooleanOperator.`!`          => stmt"NOT"
    case StringOperator.`toUpperCase` => stmt"UPPER"
    case StringOperator.`toLowerCase` => stmt"LOWER"
    case StringOperator.`toLong`      => stmt"" // cast is implicit
    case StringOperator.`toInt`       => stmt"" // cast is implicit
    case SetOperator.`isEmpty`        => stmt"NOT EXISTS"
    case SetOperator.`nonEmpty`       => stmt"EXISTS"
  }

  implicit val aggregationOperatorTokenizer: Tokenizer[AggregationOperator] = Tokenizer[AggregationOperator] {
    case AggregationOperator.`min`  => stmt"MIN"
    case AggregationOperator.`max`  => stmt"MAX"
    case AggregationOperator.`avg`  => stmt"AVG"
    case AggregationOperator.`sum`  => stmt"SUM"
    case AggregationOperator.`size` => stmt"COUNT"
  }

  implicit val binaryOperatorTokenizer: Tokenizer[BinaryOperator] = Tokenizer[BinaryOperator] {
    case EqualityOperator.`==`       => stmt"="
    case EqualityOperator.`!=`       => stmt"<>"
    case BooleanOperator.`&&`        => stmt"AND"
    case BooleanOperator.`||`        => stmt"OR"
    case StringOperator.`+`          => stmt"||"
    case StringOperator.`startsWith` => fail("bug: this code should be unreachable")
    case StringOperator.`split`      => stmt"SPLIT"
    case NumericOperator.`-`         => stmt"-"
    case NumericOperator.`+`         => stmt"+"
    case NumericOperator.`*`         => stmt"*"
    case NumericOperator.`>`         => stmt">"
    case NumericOperator.`>=`        => stmt">="
    case NumericOperator.`<`         => stmt"<"
    case NumericOperator.`<=`        => stmt"<="
    case NumericOperator.`/`         => stmt"/"
    case NumericOperator.`%`         => stmt"%"
    case SetOperator.`contains`      => stmt"IN"
  }

  implicit def propertyTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Property] = {

    def unnest(ast: Ast): (Ast, List[String]) =
      ast match {
        case Property.Opinionated(a, _, _, Hidden) =>
          unnest(a) match {
            case (a, nestedName) => (a, nestedName)
          }
        // Append the property name. This includes tuple indexes.
        case Property(a, name) =>
          unnest(a) match {
            case (ast, nestedName) =>
              (ast, nestedName :+ name)
          }
        case a => (a, Nil)
      }

    def tokenizePrefixedProperty(name: String, prefix: List[String], strategy: NamingStrategy, renameable: Renameable) =
      renameable.fixedOr(
        (prefix.mkString + name).token
      )(tokenizeColumn(strategy, prefix.mkString + name, renameable).token)

    Tokenizer[Property] {
      case Property.Opinionated(ast, name, renameable, _ /* Top level property cannot be invisible */ ) =>
        // When we have things like Embedded tables, properties inside of one another needs to be un-nested.
        // E.g. in `Property(Property(Ident("realTable"), embeddedTableAlias), realPropertyAlias)` the inner
        // property needs to be unwrapped and the result of this should only be `realTable.realPropertyAlias`
        // as opposed to `realTable.embeddedTableAlias.realPropertyAlias`.
        unnest(ast) match {
          // When using ExternalIdent such as .returning(eid => eid.idColumn) clauses drop the 'eid' since SQL
          // returning clauses have no alias for the original table. I.e. INSERT [...] RETURNING idColumn there's no
          // alias you can assign to the INSERT [...] clause that can be used as a prefix to 'idColumn'.
          // In this case, `Property(Property(Ident("realTable"), embeddedTableAlias), realPropertyAlias)`
          // should just be `realPropertyAlias` as opposed to `realTable.realPropertyAlias`.
          // The exception to this is when a Query inside of a RETURNING clause is used. In that case, assume
          // that there is an alias for the inserted table (i.e. `INSERT ... as theAlias values ... RETURNING`)
          // and the instances of ExternalIdent use it.
          case (ExternalIdent(_), prefix) =>
            stmt"${
              actionAlias.map(alias => stmt"${scopedTokenizer(alias)}.").getOrElse(stmt"")
            }${tokenizePrefixedProperty(name, prefix, strategy, renameable)}"

          // In the rare case that the Ident is invisible, do not show it. See the Ident documentation for more info.
          case (Ident.Opinionated(_, Hidden), prefix) =>
            stmt"${tokenizePrefixedProperty(name, prefix, strategy, renameable)}"

          // The normal case where `Property(Property(Ident("realTable"), embeddedTableAlias), realPropertyAlias)`
          // becomes `realTable.realPropertyAlias`.
          case (ast, prefix) =>
            stmt"${scopedTokenizer(ast)}.${tokenizePrefixedProperty(name, prefix, strategy, renameable)}"
        }
    }
  }

  implicit def valueTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Value] = Tokenizer[Value] {
    case Constant(v: String) => stmt"'${v.token}'"
    case Constant(())        => stmt"1"
    case Constant(v)         => stmt"${v.toString.token}"
    case NullValue           => stmt"null"
    case Tuple(values)       => stmt"${values.token}"
    case CaseClass(values)   => stmt"${values.map(_._2).token}"
  }

  implicit def infixTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Infix] = Tokenizer[Infix] {
    case Infix(parts, params, _) =>
      val pt = parts.map(_.token)
      val pr = params.map(_.token)
      Statement(Interleave(pt, pr))
  }

  implicit def identTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ident] =
    Tokenizer[Ident](e => strategy.default(e.name).token)

  implicit def externalIdentTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[ExternalIdent] =
    Tokenizer[ExternalIdent](e => strategy.default(e.name).token)

  implicit def assignmentTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Assignment] = Tokenizer[Assignment] {
    case Assignment(alias, prop, value) =>
      stmt"${prop.token} = ${scopedTokenizer(value)}"
  }

  implicit def defaultAstTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Action] = {
    val insertEntityTokenizer = Tokenizer[Entity] {
      case Entity.Opinionated(name, _, renameable) => stmt"INTO ${tokenizeTable(strategy, name, renameable).token}"
    }
    actionTokenizer(insertEntityTokenizer)(actionAstTokenizer, strategy)
  }

  protected def actionAstTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy) =
    Tokenizer.withFallback[Ast](SqlIdiom.this.astTokenizer(_, strategy)) {
      case q: Query => astTokenizer.token(q)
      case Property(Property.Opinionated(_, name, renameable, _), "isEmpty") => stmt"${renameable.fixedOr(name)(tokenizeColumn(strategy, name, renameable)).token} IS NULL"
      case Property(Property.Opinionated(_, name, renameable, _), "isDefined") => stmt"${renameable.fixedOr(name)(tokenizeColumn(strategy, name, renameable)).token} IS NOT NULL"
      case Property(Property.Opinionated(_, name, renameable, _), "nonEmpty") => stmt"${renameable.fixedOr(name)(tokenizeColumn(strategy, name, renameable)).token} IS NOT NULL"
      case Property.Opinionated(_, name, renameable, _) => renameable.fixedOr(name.token)(tokenizeColumn(strategy, name, renameable).token)
    }

  def returnListTokenizer(implicit tokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[List[Ast]] = {
    val customAstTokenizer =
      Tokenizer.withFallback[Ast](SqlIdiom.this.astTokenizer(_, strategy)) {
        case sq: Query =>
          stmt"(${tokenizer.token(sq)})"
      }

    Tokenizer[List[Ast]] {
      case list =>
        list.mkStmt(", ")(customAstTokenizer)
    }
  }

  protected def actionTokenizer(insertEntityTokenizer: Tokenizer[Entity])(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Action] =
    Tokenizer[Action] {

      case Insert(entity: Entity, assignments) =>
        val (table, columns, values) = insertInfo(insertEntityTokenizer, entity, assignments)
        stmt"INSERT $table${actionAlias.map(alias => stmt" AS ${alias.token}").getOrElse(stmt"")} (${columns.mkStmt(",")}) VALUES (${values.map(scopedTokenizer(_)).mkStmt(", ")})"

      case Update(table: Entity, assignments) =>
        stmt"UPDATE ${table.token}${actionAlias.map(alias => stmt" AS ${alias.token}").getOrElse(stmt"")} SET ${assignments.token}"

      case Update(Filter(table: Entity, x, where), assignments) =>
        stmt"UPDATE ${table.token}${actionAlias.map(alias => stmt" AS ${alias.token}").getOrElse(stmt"")} SET ${assignments.token} WHERE ${where.token}"

      case Delete(Filter(table: Entity, x, where)) =>
        stmt"DELETE FROM ${table.token} WHERE ${where.token}"

      case Delete(table: Entity) =>
        stmt"DELETE FROM ${table.token}"

      case r @ ReturningAction(Insert(table: Entity, Nil), alias, prop) =>
        idiomReturningCapability match {
          // If there are queries inside of the returning clause we are forced to alias the inserted table (see #1509). Only do this as
          // a last resort since it is not even supported in all Postgres versions (i.e. only after 9.5)
          case ReturningClauseSupported if (CollectAst.byType[Entity](prop).nonEmpty) =>
            SqlIdiom.withActionAlias(this, r)
          case ReturningClauseSupported =>
            stmt"INSERT INTO ${table.token} ${defaultAutoGeneratedToken(prop.token)} RETURNING ${returnListTokenizer.token(ExpandReturning(r)(this, strategy).map(_._1))}"
          case OutputClauseSupported =>
            stmt"INSERT INTO ${table.token} OUTPUT ${returnListTokenizer.token(ExpandReturning(r, Some("INSERTED"))(this, strategy).map(_._1))} ${defaultAutoGeneratedToken(prop.token)}"
          case other =>
            stmt"INSERT INTO ${table.token} ${defaultAutoGeneratedToken(prop.token)}"
        }

      case r @ ReturningAction(action, alias, prop) =>
        idiomReturningCapability match {
          // If there are queries inside of the returning clause we are forced to alias the inserted table (see #1509). Only do this as
          // a last resort since it is not even supported in all Postgres versions (i.e. only after 9.5)
          case ReturningClauseSupported if (CollectAst.byType[Entity](prop).nonEmpty) =>
            SqlIdiom.withActionAlias(this, r)
          case ReturningClauseSupported =>
            stmt"${action.token} RETURNING ${returnListTokenizer.token(ExpandReturning(r)(this, strategy).map(_._1))}"
          case OutputClauseSupported => action match {
            case Insert(entity: Entity, assignments) =>
              val (table, columns, values) = insertInfo(insertEntityTokenizer, entity, assignments)
              stmt"INSERT $table${actionAlias.map(alias => stmt" AS ${alias.token}").getOrElse(stmt"")} (${columns.mkStmt(",")}) OUTPUT ${returnListTokenizer.token(ExpandReturning(r, Some("INSERTED"))(this, strategy).map(_._1))} VALUES (${values.map(scopedTokenizer(_)).mkStmt(", ")})"
            case Update(entity: Entity, assignments) =>
              stmt"UPDATE ${entity.token}${actionAlias.map(alias => stmt" AS ${alias.token}").getOrElse(stmt"")} SET ${assignments.token} OUTPUT ${returnListTokenizer.token(ExpandReturning(r, Some("INSERTED"))(this, strategy).map(_._1))}"
            case other =>
              fail(s"Action ast can't be translated to sql: '$other'")
          }
          case _ =>
            stmt"${action.token}"
        }

      case other =>
        fail(s"Action ast can't be translated to sql: '$other'")
    }

  private def insertInfo(insertEntityTokenizer: Tokenizer[Entity], entity: Entity, assignments: List[Assignment])(implicit astTokenizer: Tokenizer[Ast]) = {
    val table = insertEntityTokenizer.token(entity)
    val columns = assignments.map(_.property.token)
    val values = assignments.map(_.value)
    (table, columns, values)
  }

  implicit def entityTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Entity] = Tokenizer[Entity] {
    case Entity.Opinionated(name, _, renameable) => tokenizeTable(strategy, name, renameable).token
  }

  protected def scopedTokenizer(ast: Ast)(implicit tokenizer: Tokenizer[Ast]) =
    ast match {
      case _: Query           => stmt"(${ast.token})"
      case _: BinaryOperation => stmt"(${ast.token})"
      case _: Tuple           => stmt"(${ast.token})"
      case _                  => ast.token
    }
}

object SqlIdiom {
  private[getquill] def copyIdiom(parent: SqlIdiom, newActionAlias: Option[Ident]) =
    new SqlIdiom {
      override protected def actionAlias: Option[Ident] = newActionAlias
      override def prepareForProbing(string: String): String = parent.prepareForProbing(string)
      override def concatFunction: String = parent.concatFunction
      override def liftingPlaceholder(index: Int): String = parent.liftingPlaceholder(index)
      override def idiomReturningCapability: ReturningCapability = parent.idiomReturningCapability
    }

  /**
   * Construct a new instance of the specified idiom with `newActionAlias` variable specified so that actions
   * (i.e. insert, and update) will be rendered with the specified alias. This is needed for RETURNING clauses that have
   * queries inside. See #1509 for details.
   */
  private[getquill] def withActionAlias(parentIdiom: SqlIdiom, query: ReturningAction)(implicit strategy: NamingStrategy) = {
    val idiom = copyIdiom(parentIdiom, Some(query.alias))
    import idiom._

    implicit val stableTokenizer = idiom.astTokenizer(new Tokenizer[Ast] {
      override def token(v: Ast): Token = astTokenizer(this, strategy).token(v)
    }, strategy)

    query match {
      case r @ ReturningAction(Insert(table: Entity, Nil), alias, prop) =>
        stmt"INSERT INTO ${table.token} AS ${alias.name.token} ${defaultAutoGeneratedToken(prop.token)} RETURNING ${returnListTokenizer.token(ExpandReturning(r)(idiom, strategy).map(_._1))}"
      case r @ ReturningAction(action, alias, prop) =>
        stmt"${action.token} RETURNING ${returnListTokenizer.token(ExpandReturning(r)(idiom, strategy).map(_._1))}"
    }
  }
}
