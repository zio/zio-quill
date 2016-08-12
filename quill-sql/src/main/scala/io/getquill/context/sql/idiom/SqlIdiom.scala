package io.getquill.context.sql.idiom

import io.getquill.ast.Action
import io.getquill.ast.Aggregation
import io.getquill.ast.AggregationOperator
import io.getquill.ast.Asc
import io.getquill.ast.AscNullsFirst
import io.getquill.ast.AscNullsLast
import io.getquill.ast.AssignedAction
import io.getquill.ast.Assignment
import io.getquill.ast.Ast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.BinaryOperator
import io.getquill.ast.Binding
import io.getquill.ast.Block
import io.getquill.ast.BooleanOperator
import io.getquill.ast.Collection
import io.getquill.ast.ConfiguredEntity
import io.getquill.ast.Constant
import io.getquill.ast.Delete
import io.getquill.ast.Desc
import io.getquill.ast.DescNullsFirst
import io.getquill.ast.DescNullsLast
import io.getquill.ast.Dynamic
import io.getquill.ast.Entity
import io.getquill.ast.EqualityOperator
import io.getquill.ast.Filter
import io.getquill.ast.FullJoin
import io.getquill.ast.Function
import io.getquill.ast.FunctionApply
import io.getquill.ast.Ident
import io.getquill.ast.If
import io.getquill.ast.Infix
import io.getquill.ast.InnerJoin
import io.getquill.ast.Insert
import io.getquill.ast.JoinType
import io.getquill.ast.LeftJoin
import io.getquill.ast.NullValue
import io.getquill.ast.NumericOperator
import io.getquill.ast.Operation
import io.getquill.ast.OptionOperation
import io.getquill.ast.Ordering
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.QuotedReference
import io.getquill.ast.RightJoin
import io.getquill.ast.SetOperator
import io.getquill.ast.SimpleEntity
import io.getquill.ast.StringOperator
import io.getquill.ast.Tuple
import io.getquill.ast.UnaryOperation
import io.getquill.ast.UnaryOperator
import io.getquill.ast.Update
import io.getquill.ast.Val
import io.getquill.ast.Value
import io.getquill.context.sql.FlattenSqlQuery
import io.getquill.context.sql.FromContext
import io.getquill.context.sql.InfixContext
import io.getquill.context.sql.JoinContext
import io.getquill.context.sql.OrderByCriteria
import io.getquill.context.sql.QueryContext
import io.getquill.context.sql.SelectValue
import io.getquill.context.sql.SetOperation
import io.getquill.context.sql.SetOperationSqlQuery
import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.TableContext
import io.getquill.context.sql.UnaryOperationSqlQuery
import io.getquill.context.sql.UnionAllOperation
import io.getquill.context.sql.UnionOperation
import io.getquill.NamingStrategy
import io.getquill.util.Messages.fail
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow

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
      case a @ (
        _: Function | _: FunctionApply | _: Dynamic | _: OptionOperation | _: Block |
        _: Val | _: Ordering | _: Binding | _: QuotedReference[_]
        ) =>
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
    case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>

      val distinctShow = if (distinct) " DISTINCT" else ""

      val selectClause =
        select match {
          case Nil => s"SELECT$distinctShow *"
          case _   => s"SELECT$distinctShow ${select.show}"
        }

      val withFrom =
        from match {
          case Nil  => selectClause
          case from => selectClause + s" FROM ${from.show}"
        }

      val withWhere =
        where match {
          case None        => withFrom
          case Some(where) => withFrom + s" WHERE ${where.show}"
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
      s"(${a.show}) ${op.show} (${b.show})"
    case UnaryOperationSqlQuery(op, q) =>
      s"SELECT ${op.show} (${q.show})"
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
    case BinaryOperation(a, op @ SetOperator.`contains`, b)   => s"${scopedShow(b)} ${op.show} (${a.show})"
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

  implicit def sourceShow(implicit strategy: NamingStrategy): Show[FromContext] = Show[FromContext] {
    case TableContext(name, alias)  => s"${name.show} ${strategy.default(alias)}"
    case QueryContext(query, alias) => s"(${query.show}) ${strategy.default(alias)}"
    case InfixContext(infix, alias) => s"(${(infix: Ast).show}) ${strategy.default(alias)}"
    case JoinContext(t, a, b, on)   => s"${a.show} ${t.show} ${b.show} ON ${on.show}"
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
    case StringOperator.`toLong`      => "" // cast is implicit
    case StringOperator.`toInt`       => "" // cast is implicit
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
    case EqualityOperator.`==`  => "="
    case EqualityOperator.`!=`  => "<>"
    case BooleanOperator.`&&`   => "AND"
    case BooleanOperator.`||`   => "OR"
    case StringOperator.`+`     => "||"
    case NumericOperator.`-`    => "-"
    case NumericOperator.`+`    => "+"
    case NumericOperator.`*`    => "*"
    case NumericOperator.`>`    => ">"
    case NumericOperator.`>=`   => ">="
    case NumericOperator.`<`    => "<"
    case NumericOperator.`<=`   => "<="
    case NumericOperator.`/`    => "/"
    case NumericOperator.`%`    => "%"
    case SetOperator.`contains` => "IN"
  }

  implicit def propertyShow(implicit valueShow: Show[Value], identShow: Show[Ident], strategy: NamingStrategy): Show[Property] =
    Show[Property] {
      case Property(ident, "isEmpty")      => s"${ident.show} IS NULL"
      case Property(ident, "nonEmpty")     => s"${ident.show} IS NOT NULL"
      case Property(ident, "isDefined")    => s"${ident.show} IS NOT NULL"
      case Property(Property(ident, a), b) => s"${ident.show}.$a$b"
      case Property(ast, name)             => s"${scopedShow(ast)}.${strategy.column(name)}"
    }

  implicit def valueShow(implicit strategy: NamingStrategy): Show[Value] = Show[Value] {
    case Constant(v: String) => s"'$v'"
    case Constant(())        => s"1"
    case Constant(v)         => s"$v"
    case NullValue           => s"null"
    case Tuple(values)       => s"${values.show}"
    case Collection(values)  => s"${values.show}"
  }

  implicit def identShow(implicit strategy: NamingStrategy): Show[Ident] = Show[Ident] {
    case e => strategy.default(e.name)
  }

  implicit def actionShow(implicit strategy: NamingStrategy): Show[Action] = {

    def set(assignments: List[Assignment]) =
      assignments.map(a => s"${strategy.column(a.property)} = ${scopedShow(a.value)}").mkString(", ")

    implicit def propertyShow: Show[Property] = Show[Property] {
      case Property(Property(_, name), "isEmpty")   => s"${strategy.column(name)} IS NULL"
      case Property(Property(_, name), "isDefined") => s"${strategy.column(name)} IS NOT NULL"
      case Property(Property(_, name), "nonEmpty")  => s"${strategy.column(name)} IS NOT NULL"
      case Property(Property(_, name), prop)        => s"${strategy.column(name)}.$prop"
      case Property(_, name)                        => strategy.column(name)
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
    case SimpleEntity(name)                             => strategy.table(name)
    case ConfiguredEntity(SimpleEntity(name), alias, _) => strategy.table(alias.getOrElse(name))
    case ConfiguredEntity(source, _, _)                 => source.show
  }

  protected def scopedShow[A <: Ast](ast: A)(implicit show: Show[A]) =
    ast match {
      case _: Query           => s"(${ast.show})"
      case _: BinaryOperation => s"(${ast.show})"
      case _: Tuple           => s"(${ast.show})"
      case other              => ast.show
    }
}
