package io.getquill.source.sql.idiom

import io.getquill.ast._
import io.getquill.naming.NamingStrategy
import io.getquill.source.sql._
import io.getquill.util.Messages.fail
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow
import io.getquill.norm.BetaReduction

trait SqlIdiom {

  def prepare(sql: String): String

  implicit def astShow(implicit propertyShow: Show[Property], strategy: NamingStrategy): Show[Ast] =
    new Show[Ast] {
      def show(a: Ast) =
        a match {
          case a: Query                       => s"${SqlQuery(a).show}"
          case a: Operation                   => a.show
          case Infix(parts, params)           => StringContext(parts: _*).s(params.map(_.show): _*)
          case a: Action                      => a.show
          case a: Ident                       => a.show
          case a: Property                    => a.show
          case a: Value                       => a.show
          case a: OptionOperation             => a.show
          case _: Function | _: FunctionApply => fail(s"Malformed query $a.")
        }
    }

  implicit def optionOperationShow(implicit strategy: NamingStrategy): Show[OptionOperation] = new Show[OptionOperation] {
    def show(e: OptionOperation) =
      e match {
        case OptionOperation(t, ast, alias, body) =>
          BetaReduction(body, alias -> ast).show
      }
  }

  implicit def sqlQueryShow(implicit strategy: NamingStrategy): Show[SqlQuery] = new Show[SqlQuery] {
    def show(e: SqlQuery) =
      e match {
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
  }

  implicit def selectValueShow(implicit strategy: NamingStrategy): Show[SelectValue] = new Show[SelectValue] {
    def show(e: SelectValue) =
      e match {
        case SelectValue(ast, Some(alias)) => s"${showValue(ast)} $alias"
        case SelectValue(ast, None)        => showValue(ast)
      }

    private def showValue(ast: Ast) =
      ast match {
        case Aggregation(op, Ident(_)) => s"${op.show}(*)"
        case Aggregation(op, _: Query) => scopedShow(ast)
        case Aggregation(op, ast)      => s"${op.show}(${ast.show})"
        case other                     => ast.show
      }
  }

  implicit def operationShow(implicit propertyShow: Show[Property], strategy: NamingStrategy): Show[Operation] = new Show[Operation] {
    def show(e: Operation) =
      e match {
        case UnaryOperation(op, ast)                              => s"${op.show} (${ast.show})"
        case BinaryOperation(a, EqualityOperator.`==`, NullValue) => s"${scopedShow(a)} IS NULL"
        case BinaryOperation(NullValue, EqualityOperator.`==`, b) => s"${scopedShow(b)} IS NULL"
        case BinaryOperation(a, EqualityOperator.`!=`, NullValue) => s"${scopedShow(a)} IS NOT NULL"
        case BinaryOperation(NullValue, EqualityOperator.`!=`, b) => s"${scopedShow(b)} IS NOT NULL"
        case BinaryOperation(a, op, b)                            => s"${scopedShow(a)} ${op.show} ${scopedShow(b)}"
        case FunctionApply(_, _)                                  => fail(s"Can't translate the ast to sql: '$e'")
      }
  }

  implicit val setOperationShow: Show[SetOperation] = new Show[SetOperation] {
    def show(e: SetOperation) =
      e match {
        case UnionOperation    => "UNION"
        case UnionAllOperation => "UNION ALL"
      }
  }

  protected def showOffsetWithoutLimit(offset: Ast)(implicit strategy: NamingStrategy) =
    s" OFFSET ${offset.show}"

  protected def showOrderBy(criterias: List[OrderByCriteria])(implicit strategy: NamingStrategy) =
    s" ORDER BY ${criterias.show}"

  implicit def sourceShow(implicit strategy: NamingStrategy): Show[Source] = new Show[Source] {
    def show(source: Source) =
      source match {
        case TableSource(name, alias)     => s"${name.show} $alias"
        case QuerySource(query, alias)    => s"(${query.show}) $alias"
        case InfixSource(infix, alias)    => s"(${(infix: Ast).show}) $alias"
        case OuterJoinSource(t, a, b, on) => s"${a.show} ${t.show} ${b.show} ON ${on.show}"
      }
  }

  implicit val outerJoinTypeShow: Show[OuterJoinType] = new Show[OuterJoinType] {
    def show(q: OuterJoinType) =
      q match {
        case LeftJoin  => "LEFT JOIN"
        case RightJoin => "RIGHT JOIN"
        case FullJoin  => "FULL JOIN"
      }
  }

  implicit def orderByCriteriaShow(implicit strategy: NamingStrategy): Show[OrderByCriteria] = new Show[OrderByCriteria] {
    def show(criteria: OrderByCriteria) =
      criteria match {
        case OrderByCriteria(prop, true)  => s"${prop.show} DESC"
        case OrderByCriteria(prop, false) => prop.show
      }
  }

  implicit val unaryOperatorShow: Show[UnaryOperator] = new Show[UnaryOperator] {
    def show(o: UnaryOperator) =
      o match {
        case NumericOperator.`-`          => "-"
        case BooleanOperator.`!`          => "NOT"
        case StringOperator.`toUpperCase` => "UPPER"
        case StringOperator.`toLowerCase` => "LOWER"
        case SetOperator.`isEmpty`        => "NOT EXISTS"
        case SetOperator.`nonEmpty`       => "EXISTS"
      }
  }

  implicit val aggregationOperatorShow: Show[AggregationOperator] = new Show[AggregationOperator] {
    def show(o: AggregationOperator) =
      o match {
        case AggregationOperator.`min`  => "MIN"
        case AggregationOperator.`max`  => "MAX"
        case AggregationOperator.`avg`  => "AVG"
        case AggregationOperator.`sum`  => "SUM"
        case AggregationOperator.`size` => "COUNT"
      }
  }

  implicit val binaryOperatorShow: Show[BinaryOperator] = new Show[BinaryOperator] {
    def show(o: BinaryOperator) =
      o match {
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
  }

  implicit def propertyShow(implicit valueShow: Show[Value], identShow: Show[Ident], strategy: NamingStrategy): Show[Property] =
    new Show[Property] {
      def show(e: Property) =
        e match {
          case Property(ast, name) => s"${scopedShow(ast)}.${strategy(name)}"
        }
    }

  implicit def valueShow(implicit strategy: NamingStrategy): Show[Value] = new Show[Value] {
    def show(e: Value) =
      e match {
        case Constant(v: String) => s"'$v'"
        case Constant(())        => s"1"
        case Constant(v)         => s"$v"
        case NullValue           => s"null"
        case Tuple(values)       => s"${values.show}"
      }
  }

  implicit val identShow: Show[Ident] = new Show[Ident] {
    def show(e: Ident) = e.name
  }

  implicit def actionShow(implicit strategy: NamingStrategy): Show[Action] = {

    def set(assignments: List[Assignment]) =
      assignments.map(a => s"${a.property} = ${a.value.show}").mkString(", ")

    implicit def propertyShow: Show[Property] = new Show[Property] {
      def show(e: Property) =
        e match {
          case Property(_, name) => name
        }
    }

    new Show[Action] {
      def show(a: Action) =
        (a: @unchecked) match {

          case Insert(table: Entity, assignments) =>
            val columns = assignments.map(_.property)
            val values = assignments.map(_.value)
            s"INSERT INTO ${table.show} (${columns.mkString(",")}) VALUES (${values.show})"

          case Update(table: Entity, assignments) =>
            s"UPDATE ${table.show} SET ${set(assignments)}"

          case Update(Filter(table: Entity, x, where), assignments) =>
            s"UPDATE ${table.show} SET ${set(assignments)} WHERE ${where.show}"

          case Delete(Filter(table: Entity, x, where)) =>
            s"DELETE FROM ${table.show} WHERE ${where.show}"

          case Delete(table: Entity) =>
            s"DELETE FROM ${table.show}"
        }
    }
  }

  implicit def entityShow(implicit strategy: NamingStrategy): Show[Entity] = new Show[Entity] {
    def show(e: Entity) =
      e.alias.getOrElse(strategy(e.name))
  }

  private def scopedShow[A <: Ast](ast: A)(implicit show: Show[A]) =
    ast match {
      case _: Query           => s"(${ast.show})"
      case _: BinaryOperation => s"(${ast.show})"
      case _: Tuple           => s"(${ast.show})"
      case other              => ast.show
    }

}
