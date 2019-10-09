package io.getquill.context.cassandra

import io.getquill.ast.{ IterableOperation, _ }
import io.getquill.NamingStrategy
import io.getquill.context.CannotReturn
import io.getquill.util.Messages.fail
import io.getquill.idiom.Idiom
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.Statement
import io.getquill.idiom.SetContainsToken
import io.getquill.idiom.Token
import io.getquill.util.Interleave

object CqlIdiom extends CqlIdiom with CannotReturn

trait CqlIdiom extends Idiom {

  override def liftingPlaceholder(idx: Int) = "?"

  override def emptySetContainsToken(field: Token) = stmt"$field IN ()"

  override def prepareForProbing(string: String) = string

  override def translate(ast: Ast)(implicit naming: NamingStrategy) = {
    val normalizedAst = CqlNormalize(ast)
    (normalizedAst, stmt"${normalizedAst.token}")
  }

  implicit def astTokenizer(implicit strategy: NamingStrategy, queryTokenizer: Tokenizer[Query]): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case Aggregation(AggregationOperator.`size`, Constant(1)) =>
        "COUNT(1)".token
      case a: Query             => a.token
      case a: Operation         => a.token
      case a: Action            => a.token
      case a: Ident             => a.token
      case a: ExternalIdent     => a.token
      case a: Property          => a.token
      case a: Value             => a.token
      case a: Function          => a.body.token
      case a: Infix             => a.token
      case a: Lift              => a.token
      case a: Assignment        => a.token
      case a: IterableOperation => a.token
      case a @ (
        _: Function | _: FunctionApply | _: Dynamic | _: OptionOperation | _: Block |
        _: Val | _: Ordering | _: QuotedReference | _: If | _: OnConflict.Excluded | _: OnConflict.Existing
        ) =>
        fail(s"Invalid cql: '$a'")
    }

  implicit def queryTokenizer(implicit strategy: NamingStrategy): Tokenizer[Query] = Tokenizer[Query] {
    case q => CqlQuery(q).token
  }

  implicit def cqlQueryTokenizer(implicit strategy: NamingStrategy): Tokenizer[CqlQuery] = Tokenizer[CqlQuery] {

    case CqlQuery(entity, filter, orderBy, limit, select, distinct) =>

      val distinctToken = if (distinct) " DISTINCT".token else "".token

      val withSelect =
        select match {
          case Nil if distinct => fail(s"Cql only supports DISTINCT with a selection list.'")
          case Nil             => stmt"SELECT *"
          case s               => stmt"SELECT$distinctToken ${s.token}"
        }
      val withEntity =
        stmt"$withSelect FROM ${entity.token}"
      val withFilter =
        filter match {
          case None    => withEntity
          case Some(f) => stmt"$withEntity WHERE ${f.token}"
        }
      val withOrderBy =
        orderBy match {
          case Nil => withFilter
          case o   => stmt"$withFilter ORDER BY ${o.token}"
        }
      limit match {
        case None    => withOrderBy
        case Some(l) => stmt"$withOrderBy LIMIT ${l.token}"
      }
  }

  implicit def orderByCriteriaTokenizer(implicit strategy: NamingStrategy): Tokenizer[OrderByCriteria] = Tokenizer[OrderByCriteria] {
    case OrderByCriteria(prop, Asc | AscNullsFirst | AscNullsLast)    => stmt"${prop.token} ASC"
    case OrderByCriteria(prop, Desc | DescNullsFirst | DescNullsLast) => stmt"${prop.token} DESC"
  }

  implicit def operationTokenizer(implicit strategy: NamingStrategy): Tokenizer[Operation] = Tokenizer[Operation] {
    case BinaryOperation(a, op @ SetOperator.`contains`, b) => SetContainsToken(b.token, op.token, a.token)
    case BinaryOperation(a, op, b)                          => stmt"${a.token} ${op.token} ${b.token}"
    case e: UnaryOperation                                  => fail(s"Cql doesn't support unary operations. Found: '$e'")
    case e: FunctionApply                                   => fail(s"Cql doesn't support functions. Found: '$e'")
  }

  implicit val aggregationOperatorTokenizer: Tokenizer[AggregationOperator] = Tokenizer[AggregationOperator] {
    case AggregationOperator.`size` => stmt"COUNT"
    case o                          => fail(s"Cql doesn't support '$o' aggregations")
  }

  implicit val binaryOperatorTokenizer: Tokenizer[BinaryOperator] = Tokenizer[BinaryOperator] {
    case EqualityOperator.`==`  => stmt"="
    case BooleanOperator.`&&`   => stmt"AND"
    case NumericOperator.`>`    => stmt">"
    case NumericOperator.`>=`   => stmt">="
    case NumericOperator.`<`    => stmt"<"
    case NumericOperator.`<=`   => stmt"<="
    case NumericOperator.`+`    => stmt"+"
    case SetOperator.`contains` => stmt"IN"
    case other                  => fail(s"Cql doesn't support the '$other' operator.")
  }

  // Note: The CqlIdiom does not support joins so there is no need for any complex un-nesting logic like in SqlIdiom
  // if there are Embedded classes, they will result in Property(Property(embedded, embeddedProp), actualProp)
  // and we only need to take the top-level property i.e. `actualProp`.
  implicit def propertyTokenizer(implicit valueTokenizer: Tokenizer[Value], identTokenizer: Tokenizer[Ident], strategy: NamingStrategy): Tokenizer[Property] =
    Tokenizer[Property] {
      case Property.Opinionated(_, name, renameable, _) => renameable.fixedOr(name.token)(strategy.column(name).token)
    }

  implicit def valueTokenizer(implicit strategy: NamingStrategy): Tokenizer[Value] = Tokenizer[Value] {
    case Constant(v: String) => stmt"'${v.token}'"
    case Constant(())        => stmt"1"
    case Constant(v)         => stmt"${v.toString.token}"
    case Tuple(values)       => stmt"${values.token}"
    case CaseClass(values)   => stmt"${values.map(_._2).token}"
    case NullValue           => fail("Cql doesn't support null values.")
  }

  implicit def infixTokenizer(implicit propertyTokenizer: Tokenizer[Property], strategy: NamingStrategy, queryTokenizer: Tokenizer[Query]): Tokenizer[Infix] = Tokenizer[Infix] {
    case Infix(parts, params, _) =>
      val pt = parts.map(_.token)
      val pr = params.map(_.token)
      Statement(Interleave(pt, pr))
  }

  implicit def identTokenizer(implicit strategy: NamingStrategy): Tokenizer[Ident] = Tokenizer[Ident] {
    case e => strategy.default(e.name).token
  }

  implicit def externalIdentTokenizer(implicit strategy: NamingStrategy): Tokenizer[ExternalIdent] = Tokenizer[ExternalIdent] {
    case e => strategy.default(e.name).token
  }

  implicit def assignmentTokenizer(implicit propertyTokenizer: Tokenizer[Property], strategy: NamingStrategy): Tokenizer[Assignment] = Tokenizer[Assignment] {
    case Assignment(alias, prop, value) =>
      stmt"${prop.token} = ${value.token}"
  }

  implicit def actionTokenizer(implicit strategy: NamingStrategy): Tokenizer[Action] = {

    implicit def queryTokenizer(implicit strategy: NamingStrategy): Tokenizer[Query] = Tokenizer[Query] {
      case q: Entity => q.token
      case other     => fail(s"Expected a table, got '$other'")
    }

    Tokenizer[Action] {

      case Insert(table, assignments) =>
        val columns = assignments.map(_.property.token)
        val values = assignments.map(_.value)
        stmt"INSERT INTO ${table.token} (${columns.mkStmt(",")}) VALUES (${values.map(_.token).mkStmt(", ")})"

      case Update(Filter(table, x, where), assignments) =>
        stmt"UPDATE ${table.token} SET ${assignments.token} WHERE ${where.token}"

      case Update(table, assignments) =>
        stmt"UPDATE ${table.token} SET ${assignments.token}"

      case Delete(Map(Filter(table, _, where), _, columns)) =>
        stmt"DELETE ${columns.token} FROM ${table.token} WHERE ${where.token}"

      case Delete(Map(table, _, columns)) =>
        stmt"DELETE ${columns.token} FROM ${table.token}"

      case Delete(Filter(table, x, where)) =>
        stmt"DELETE FROM ${table.token} WHERE ${where.token}"

      case Delete(table) =>
        stmt"TRUNCATE ${table.token}"

      case _: Returning | _: ReturningGenerated =>
        fail(s"Cql doesn't support returning generated during insertion")

      case other =>
        fail(s"Action ast can't be translated to cql: '$other'")
    }
  }

  implicit def entityTokenizer(implicit strategy: NamingStrategy): Tokenizer[Entity] = Tokenizer[Entity] {
    case Entity.Opinionated(name, properties, renameable) =>
      renameable.fixedOr(name.token)(strategy.table(name).token)
  }

  implicit def traversableTokenizer(implicit strategy: NamingStrategy): Tokenizer[IterableOperation] =
    Tokenizer[IterableOperation] {
      case MapContains(ast, body)  => stmt"${ast.token} CONTAINS KEY ${body.token}"
      case SetContains(ast, body)  => stmt"${ast.token} CONTAINS ${body.token}"
      case ListContains(ast, body) => stmt"${ast.token} CONTAINS ${body.token}"
    }
}
