package io.getquill.sources.sql.idiom

import io.getquill.ast._
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql._
import io.getquill.util.Messages.fail
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow
import io.getquill.norm.BetaReduction

trait SqlIdiom {

  def prepare(sql: String): String

  implicit def astShow(implicit propertyShow: Show[Property], strategy: NamingStrategy): Show[Ast] =
    Show[Ast] {
      case a: Query             => s"${SqlQuery(a).show}"
      case a: Operation         => a.show
      case Infix(parts, params) => StringContext(parts: _*).s(params.map(_.show): _*)
      case a: Action            => a.show
      case a: Ident             => a.show
      case a: Property          => a.show
      case a: Value             => a.show
      case a: If                => a.show
      case a @ (_: Function | _: FunctionApply | _: Dynamic | _: OptionOperation) =>
        fail(s"Malformed query $a.")
    }

  implicit def ifShow(implicit strategy: NamingStrategy): Show[If] = Show[If] {
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
          s"WHEN ${cond.show} THEN ${body.show}"
        }
      s"CASE ${conditions.mkString(" ")} ELSE ${e.show} END"
  }

  implicit def sqlQueryShow(implicit strategy: NamingStrategy): Show[SqlQuery] = Show[SqlQuery] {
    case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select) =>
      val selectClause =
        select match {
          case Nil => s"SELECT * FROM ${from.show}"
          case _   => s"SELECT ${select.show} FROM ${from.show}"
        }

      val withWhere =
        where match {
          case None        => selectClause
          case Some(where) => selectClause + s" WHERE ${where.show}"
        }
      val withGroupBy =
        groupBy match {
          case Nil     => withWhere
          case groupBy => withWhere + s" GROUP BY ${groupBy.show}"
        }
      val withOrderBy =
        orderBy match {
          case Nil     => withGroupBy
          case orderBy => withGroupBy + showOrderBy(orderBy)
        }
      (limit, offset) match {
        case (None, None)                => withOrderBy
        case (Some(limit), None)         => withOrderBy + s" LIMIT ${limit.show}"
        case (Some(limit), Some(offset)) => withOrderBy + s" LIMIT ${limit.show} OFFSET ${offset.show}"
        case (None, Some(offset))        => withOrderBy + showOffsetWithoutLimit(offset)
      }
    case SetOperationSqlQuery(a, op, b) =>
      s"${a.show} ${op.show} ${b.show}"
  }

  implicit def selectValueShow(implicit strategy: NamingStrategy): Show[SelectValue] = {
    def showValue(ast: Ast) =
      ast match {
        case Aggregation(op, Ident(_)) => s"${op.show}(*)"
        case Aggregation(op, _: Query) => scopedShow(ast)
        case Aggregation(op, ast)      => s"${op.show}(${ast.show})"
        case other                     => ast.show
      }
    Show[SelectValue] {
      case SelectValue(ast, Some(alias)) => s"${showValue(ast)} $alias"
      case SelectValue(Ident("?"), None) => "?"
      case SelectValue(ast: Ident, None) => s"${showValue(ast)}.*"
      case SelectValue(ast, None)        => showValue(ast)
    }
  }

  implicit def operationShow(implicit propertyShow: Show[Property], strategy: NamingStrategy): Show[Operation] = Show[Operation] {
    case UnaryOperation(op, ast)                              => s"${op.show} (${ast.show})"
    case BinaryOperation(a, EqualityOperator.`==`, NullValue) => s"${scopedShow(a)} IS NULL"
    case BinaryOperation(NullValue, EqualityOperator.`==`, b) => s"${scopedShow(b)} IS NULL"
    case BinaryOperation(a, EqualityOperator.`!=`, NullValue) => s"${scopedShow(a)} IS NOT NULL"
    case BinaryOperation(NullValue, EqualityOperator.`!=`, b) => s"${scopedShow(b)} IS NOT NULL"
    case BinaryOperation(a, op, b)                            => s"${scopedShow(a)} ${op.show} ${scopedShow(b)}"
    case e: FunctionApply                                     => fail(s"Can't translate the ast to sql: '$e'")
  }

  implicit val setOperationShow: Show[SetOperation] = Show[SetOperation] {
    case UnionOperation    => "UNION"
    case UnionAllOperation => "UNION ALL"
  }

  protected def showOffsetWithoutLimit(offset: Ast)(implicit strategy: NamingStrategy) =
    s" OFFSET ${offset.show}"

  protected def showOrderBy(criterias: List[OrderByCriteria])(implicit strategy: NamingStrategy) =
    s" ORDER BY ${criterias.show}"

  implicit def sourceShow(implicit strategy: NamingStrategy): Show[Source] = Show[Source] {
    case TableSource(name, alias)  => s"${name.show} ${strategy.default(alias)}"
    case QuerySource(query, alias) => s"(${query.show}) ${strategy.default(alias)}"
    case InfixSource(infix, alias) => s"(${(infix: Ast).show}) ${strategy.default(alias)}"
    case JoinSource(t, a, b, on)   => s"${a.show} ${t.show} ${b.show} ON ${on.show}"
  }

  implicit val joinTypeShow: Show[JoinType] = Show[JoinType] {
    case InnerJoin => "INNER JOIN"
    case LeftJoin  => "LEFT JOIN"
    case RightJoin => "RIGHT JOIN"
    case FullJoin  => "FULL JOIN"
  }

  implicit def orderByCriteriaShow(implicit strategy: NamingStrategy): Show[OrderByCriteria] = Show[OrderByCriteria] {
    case OrderByCriteria(ast, Asc)            => s"${scopedShow(ast)} ASC"
    case OrderByCriteria(ast, Desc)           => s"${scopedShow(ast)} DESC"
    case OrderByCriteria(ast, AscNullsFirst)  => s"${scopedShow(ast)} ASC NULLS FIRST"
    case OrderByCriteria(ast, DescNullsFirst) => s"${scopedShow(ast)} DESC NULLS FIRST"
    case OrderByCriteria(ast, AscNullsLast)   => s"${scopedShow(ast)} ASC NULLS LAST"
    case OrderByCriteria(ast, DescNullsLast)  => s"${scopedShow(ast)} DESC NULLS LAST"
  }

  implicit val unaryOperatorShow: Show[UnaryOperator] = Show[UnaryOperator] {
    case NumericOperator.`-`          => "-"
    case BooleanOperator.`!`          => "NOT"
    case StringOperator.`toUpperCase` => "UPPER"
    case StringOperator.`toLowerCase` => "LOWER"
    case SetOperator.`isEmpty`        => "NOT EXISTS"
    case SetOperator.`nonEmpty`       => "EXISTS"
  }

  implicit val aggregationOperatorShow: Show[AggregationOperator] = Show[AggregationOperator] {
    case AggregationOperator.`min`  => "MIN"
    case AggregationOperator.`max`  => "MAX"
    case AggregationOperator.`avg`  => "AVG"
    case AggregationOperator.`sum`  => "SUM"
    case AggregationOperator.`size` => "COUNT"
  }

  implicit val binaryOperatorShow: Show[BinaryOperator] = Show[BinaryOperator] {
    case EqualityOperator.`==` => "="
    case EqualityOperator.`!=` => "<>"
    case BooleanOperator.`&&`  => "AND"
    case BooleanOperator.`||`  => "OR"
    case StringOperator.`+`    => "||"
    case NumericOperator.`-`   => "-"
    case NumericOperator.`+`   => "+"
    case NumericOperator.`*`   => "*"
    case NumericOperator.`>`   => ">"
    case NumericOperator.`>=`  => ">="
    case NumericOperator.`<`   => "<"
    case NumericOperator.`<=`  => "<="
    case NumericOperator.`/`   => "/"
    case NumericOperator.`%`   => "%"
  }

  implicit def propertyShow(implicit valueShow: Show[Value], identShow: Show[Ident], strategy: NamingStrategy): Show[Property] =
    Show[Property] {
      case Property(Property(ident, a), b) => s"$ident.$a$b"
      case Property(ast, name)             => s"${scopedShow(ast)}.${strategy.column(name)}"
    }

  implicit def valueShow(implicit strategy: NamingStrategy): Show[Value] = Show[Value] {
    case Constant(v: String) => s"'$v'"
    case Constant(())        => s"1"
    case Constant(v)         => s"$v"
    case NullValue           => s"null"
    case Tuple(values)       => s"${values.show}"
  }

  implicit def identShow(implicit strategy: NamingStrategy): Show[Ident] = Show[Ident] {
    case e => strategy.default(e.name)
  }

  implicit def actionShow(implicit strategy: NamingStrategy): Show[Action] = {

    def set(assignments: List[Assignment]) =
      assignments.map(a => s"${strategy.column(a.property)} = ${scopedShow(a.value)}").mkString(", ")

    implicit def propertyShow: Show[Property] = Show[Property] {
      case Property(_, name) => strategy.column(name)
    }

    Show[Action] {

      case AssignedAction(Insert(table: Entity), assignments) =>
        val columns = assignments.map(_.property).map(strategy.column(_))
        val values = assignments.map(_.value)
        s"INSERT INTO ${table.show} (${columns.mkString(",")}) VALUES (${values.map(scopedShow(_)).mkString(", ")})"

      case AssignedAction(Update(table: Entity), assignments) =>
        s"UPDATE ${table.show} SET ${set(assignments)}"

      case AssignedAction(Update(Filter(table: Entity, x, where)), assignments) =>
        s"UPDATE ${table.show} SET ${set(assignments)} WHERE ${where.show}"

      case Delete(Filter(table: Entity, x, where)) =>
        s"DELETE FROM ${table.show} WHERE ${where.show}"

      case Delete(table: Entity) =>
        s"DELETE FROM ${table.show}"

      case other =>
        fail(s"Action ast can't be translated to sql: '$other'")
    }
  }

  implicit def entityShow(implicit strategy: NamingStrategy): Show[Entity] = Show[Entity] {
    case e => e.alias.map(strategy.table(_)).getOrElse(strategy.table(e.name))
  }

  private def scopedShow[A <: Ast](ast: A)(implicit show: Show[A]) =
    ast match {
      case _: Query           => s"(${ast.show})"
      case _: BinaryOperation => s"(${ast.show})"
      case _: Tuple           => s"(${ast.show})"
      case other              => ast.show
    }
}
