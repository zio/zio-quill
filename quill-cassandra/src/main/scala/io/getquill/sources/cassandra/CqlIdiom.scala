package io.getquill.sources.cassandra

import io.getquill.ast._
import io.getquill.naming.NamingStrategy
import io.getquill.util.Messages.fail
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow

object CqlIdiom {

  implicit def astShow(implicit strategy: NamingStrategy, queryShow: Show[Query]): Show[Ast] =
    Show[Ast] {
      case Aggregation(AggregationOperator.`size`, Constant(1)) =>
        "COUNT(1)"
      case a: Query     => a.show
      case a: Operation => a.show
      case a: Action    => a.show
      case a: Ident     => a.show
      case a: Property  => a.show
      case a: Value     => a.show
      case a: Function  => a.body.show
      case Infix(parts, params) =>
        StringContext(parts: _*).s(params.map(_.show): _*)
      case a @ (_: Function | _: FunctionApply | _: Dynamic | _: If | _: OptionOperation | _: Query) =>
        fail(s"Invalid cql: '$a'")
    }

  implicit def queryShow(implicit strategy: NamingStrategy): Show[Query] = Show[Query] {
    case q => CqlQuery(q).show
  }

  implicit def cqlQueryShow(implicit strategy: NamingStrategy): Show[CqlQuery] = Show[CqlQuery] {

    case CqlQuery(entity, filter, orderBy, limit, select) =>
      val withSelect =
        select match {
          case Nil => "SELECT *"
          case s   => s"SELECT ${s.show}"
        }
      val withEntity =
        s"$withSelect FROM ${entity.show}"
      val withFilter =
        filter match {
          case None    => withEntity
          case Some(f) => s"$withEntity WHERE ${f.show}"
        }
      val withOrderBy =
        orderBy match {
          case Nil => withFilter
          case o   => s"$withFilter ORDER BY ${o.show}"
        }
      limit match {
        case None    => withOrderBy
        case Some(l) => s"$withOrderBy LIMIT ${l.show}"
      }
  }

  implicit def orderByCriteriaShow(implicit strategy: NamingStrategy): Show[OrderByCriteria] = Show[OrderByCriteria] {
    case OrderByCriteria(prop, Asc | AscNullsFirst | AscNullsLast)    => s"${prop.show} ASC"
    case OrderByCriteria(prop, Desc | DescNullsFirst | DescNullsLast) => s"${prop.show} DESC"
  }

  implicit def operationShow(implicit strategy: NamingStrategy): Show[Operation] = Show[Operation] {
    case BinaryOperation(a, op, b) => s"${a.show} ${op.show} ${b.show}"
    case e: UnaryOperation         => fail(s"Cql doesn't support unary operations. Found: '$e'")
    case e: FunctionApply          => fail(s"Cql doesn't support functions. Found: '$e'")
  }

  implicit val aggregationOperatorShow: Show[AggregationOperator] = Show[AggregationOperator] {
    case AggregationOperator.`size` => "COUNT"
    case o                          => fail(s"Cql doesn't support '$o' aggregations")
  }

  implicit val binaryOperatorShow: Show[BinaryOperator] = Show[BinaryOperator] {
    case EqualityOperator.`==` => "="
    case BooleanOperator.`&&`  => "AND"
    case NumericOperator.`>`   => ">"
    case NumericOperator.`>=`  => ">="
    case NumericOperator.`<`   => "<"
    case NumericOperator.`<=`  => "<="
    case other                 => fail(s"Cql doesn't support the '$other' operator.")
  }

  implicit def propertyShow(implicit valueShow: Show[Value], identShow: Show[Ident], strategy: NamingStrategy): Show[Property] =
    Show[Property] {
      case Property(_, name) => strategy.column(name)
    }

  implicit def valueShow(implicit strategy: NamingStrategy): Show[Value] = Show[Value] {
    case Constant(v: String) => s"'$v'"
    case Constant(())        => s"1"
    case Constant(v)         => s"$v"
    case Tuple(values)       => s"${values.show}"
    case NullValue           => fail("Cql doesn't support null values.")
  }

  implicit def identShow(implicit strategy: NamingStrategy): Show[Ident] = Show[Ident] {
    case e => strategy.default(e.name)
  }

  implicit def actionShow(implicit strategy: NamingStrategy): Show[Action] = {

    def set(assignments: List[Assignment]) =
      assignments.map(a => s"${strategy.column(a.property)} = ${a.value.show}").mkString(", ")

    implicit def queryShow(implicit strategy: NamingStrategy): Show[Query] = Show[Query] {
      case q: Entity => q.show
      case other     => fail(s"Expected a table, got '$other'")
    }

    Show[Action] {

      case AssignedAction(Insert(table), assignments) =>
        val columns = assignments.map(_.property).map(strategy.column(_))
        val values = assignments.map(_.value)
        s"INSERT INTO ${table.show} (${columns.mkString(",")}) VALUES (${values.show})"

      case AssignedAction(Update(Filter(table, x, where)), assignments) =>
        s"UPDATE ${table.show} SET ${set(assignments)} WHERE ${where.show}"

      case AssignedAction(Update(table), assignments) =>
        s"UPDATE ${table.show} SET ${set(assignments)}"

      case Delete(Map(Filter(table, _, where), _, columns)) =>
        s"DELETE ${columns.show} FROM ${table.show} WHERE ${where.show}"

      case Delete(Map(table, _, columns)) =>
        s"DELETE ${columns.show} FROM ${table.show}"

      case Delete(Filter(table, x, where)) =>
        s"DELETE FROM ${table.show} WHERE ${where.show}"

      case Delete(table) =>
        s"TRUNCATE ${table.show}"

      case other =>
        fail(s"Action ast can't be translated to sql: '$other'")
    }
  }

  implicit def entityShow(implicit strategy: NamingStrategy): Show[Entity] = Show[Entity] {
    case e => e.alias.map(strategy.table(_)).getOrElse(strategy.table(e.name))
  }
}
