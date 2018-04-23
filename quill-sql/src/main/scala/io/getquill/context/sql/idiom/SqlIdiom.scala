package io.getquill.context.sql.idiom

import io.getquill.ast._
import io.getquill.ast.BooleanOperator._
import io.getquill.ast.Lift
import io.getquill.context.sql._
import io.getquill.context.sql.norm._
import io.getquill.idiom.{ Idiom, SetContainsToken, Statement }
import io.getquill.idiom.StatementInterpolator._
import io.getquill.NamingStrategy
import io.getquill.util.Interleave
import io.getquill.util.Messages.{ fail, trace }

trait SqlIdiom extends Idiom {

  override def prepareForProbing(string: String): String

  override def translate(ast: Ast)(implicit naming: NamingStrategy) = {
    val normalizedAst = SqlNormalize(ast)

    implicit val tokernizer = defaultTokenizer

    val token =
      normalizedAst match {
        case q: Query =>
          val sql = SqlQuery(q)
          trace("sql")(sql)
          VerifySqlQuery(sql).map(fail)
          val expanded = ExpandNestedQueries(sql, collection.Set.empty).token
          trace("expanded sql")(expanded)
          expanded
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
      case a: Property        => a.token
      case a: Value           => a.token
      case a: If              => a.token
      case a: Lift            => a.token
      case a: Assignment      => a.token
      case a: OptionOperation => a.token
      case a @ (
        _: Function | _: FunctionApply | _: Dynamic | _: OptionOperation | _: Block |
        _: Val | _: Ordering | _: QuotedReference | _: TraversableOperation | _: OnConflict.Excluded | _: OnConflict.Existing
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

  implicit def sqlQueryTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[SqlQuery] = Tokenizer[SqlQuery] {
    case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>

      val distinctTokenizer = (if (distinct) "DISTINCT " else "").token

      val withDistinct =
        select match {
          case Nil => stmt"$distinctTokenizer*"
          case _   => stmt"$distinctTokenizer${select.token}"
        }

      val withFrom =
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

      val withWhere =
        where match {
          case None        => withFrom
          case Some(where) => stmt"$withFrom WHERE ${where.token}"
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
      val withLimitOffset = limitOffsetToken(withOrderBy).token((limit, offset))
      stmt"SELECT $withLimitOffset"
    case SetOperationSqlQuery(a, op, b) =>
      stmt"(${a.token}) ${op.token} (${b.token})"
    case UnaryOperationSqlQuery(op, q) =>
      stmt"SELECT ${op.token} (${q.token})"
  }

  implicit def selectValueTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[SelectValue] = {

    def tokenizer(implicit astTokenizer: Tokenizer[Ast]) =
      Tokenizer[SelectValue] {
        case SelectValue(ast, Some(alias), false) => stmt"${ast.token} ${strategy.column(alias).token}"
        case SelectValue(ast, Some(alias), true)  => stmt"${concatFunction.token}(${ast.token}) ${strategy.column(alias).token}"
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
    case TableContext(name, alias)  => stmt"${name.token} ${strategy.default(alias).token}"
    case QueryContext(query, alias) => stmt"(${query.token}) ${strategy.default(alias).token}"
    case InfixContext(infix, alias) => stmt"(${(infix: Ast).token}) ${strategy.default(alias).token}"
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
    def unnest(ast: Ast): (Ast, String) =
      ast match {
        case Property(a, name) if (name.matches("_[0-9]*")) =>
          unnest(a) match {
            case (ast, nestedName) =>
              (ast, s"$nestedName$name")
          }
        case Property(a, name) =>
          unnest(a) match {
            case (a, _) => (a, name)
          }
        case a => (a, "")
      }
    Tokenizer[Property] {
      case ast =>
        unnest(ast) match {
          case (ast, name) =>
            stmt"${scopedTokenizer(ast)}.${strategy.column(name).token}"
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
    case Infix(parts, params) =>
      val pt = parts.map(_.token)
      val pr = params.map(_.token)
      Statement(Interleave(pt, pr))
  }

  implicit def identTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ident] =
    Tokenizer[Ident](e => strategy.default(e.name).token)

  implicit def assignmentTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Assignment] = Tokenizer[Assignment] {
    case Assignment(alias, prop, value) =>
      stmt"${prop.token} = ${scopedTokenizer(value)}"
  }

  implicit def defaultAstTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Action] = {
    val insertEntityTokenizer = Tokenizer[Entity] {
      case Entity(name, _) => stmt"INTO ${strategy.table(name).token}"
    }
    actionTokenizer(insertEntityTokenizer)(actionAstTokenizer, strategy)
  }

  protected def actionAstTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy) =
    Tokenizer.withFallback[Ast](SqlIdiom.this.astTokenizer(_, strategy)) {
      case q: Query                                 => astTokenizer.token(q)
      case Property(Property(_, name), "isEmpty")   => stmt"${strategy.column(name).token} IS NULL"
      case Property(Property(_, name), "isDefined") => stmt"${strategy.column(name).token} IS NOT NULL"
      case Property(Property(_, name), "nonEmpty")  => stmt"${strategy.column(name).token} IS NOT NULL"
      case Property(_, name)                        => strategy.column(name).token
    }

  protected def actionTokenizer(insertEntityTokenizer: Tokenizer[Entity])(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Action] =
    Tokenizer[Action] {

      case Insert(entity: Entity, assignments) =>
        val table = insertEntityTokenizer.token(entity)
        val columns = assignments.map(_.property.token)
        val values = assignments.map(_.value)
        stmt"INSERT $table (${columns.mkStmt(",")}) VALUES (${values.map(scopedTokenizer(_)).mkStmt(", ")})"

      case Update(table: Entity, assignments) =>
        stmt"UPDATE ${table.token} SET ${assignments.token}"

      case Update(Filter(table: Entity, x, where), assignments) =>
        stmt"UPDATE ${table.token} SET ${assignments.token} WHERE ${where.token}"

      case Delete(Filter(table: Entity, x, where)) =>
        stmt"DELETE FROM ${table.token} WHERE ${where.token}"

      case Delete(table: Entity) =>
        stmt"DELETE FROM ${table.token}"

      case Returning(Insert(table: Entity, Nil), alias, prop) =>
        stmt"INSERT INTO ${table.token} ${defaultAutoGeneratedToken(prop.token)}"

      case Returning(action, alias, prop) =>
        action.token

      case other =>
        fail(s"Action ast can't be translated to sql: '$other'")
    }

  implicit def entityTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Entity] = Tokenizer[Entity] {
    case Entity(name, _) => strategy.table(name).token
  }

  protected def scopedTokenizer(ast: Ast)(implicit tokenizer: Tokenizer[Ast]) =
    ast match {
      case _: Query           => stmt"(${ast.token})"
      case _: BinaryOperation => stmt"(${ast.token})"
      case _: Tuple           => stmt"(${ast.token})"
      case _                  => ast.token
    }
}
