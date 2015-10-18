package io.getquill.source.sql.idiom

import io.getquill.{ Query => _, Action => _, _ }
import io.getquill.ast._
import io.getquill.source.sql._
import io.getquill.util.Messages.fail
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow

trait SqlIdiom {

  def prepare(sql: String): Option[String] =
    prepareKeyword.map(k => s"PREPARE p${sql.hashCode.abs} $k '$sql'")

  def prepareKeyword: Option[String] = None

  implicit def astShow(implicit propertyShow: Show[Property]): Show[Ast] =
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
          case _: Function | _: FunctionApply => fail(s"Malformed query $a.")
        }
    }

  implicit val sqlQueryShow: Show[SqlQuery] = new Show[SqlQuery] {
    def show(e: SqlQuery) =
      e match {
        case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select) =>
          val selectClause = s"SELECT ${select.show} FROM ${from.show}"
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

  implicit val selectValueShow: Show[SelectValue] = new Show[SelectValue] {
    def show(e: SelectValue) =
      e match {
        case SelectValue(Ident(i), _) if i != "*" => fail(s"A select value must not be a nested type. Found: '$i'")
        case SelectValue(ast, Some(alias))        => s"${ast.show} $alias"
        case SelectValue(ast, None)               => ast.show
      }
  }

  implicit def operationShow(implicit propertyShow: Show[Property]): Show[Operation] = new Show[Operation] {
    def show(e: Operation) =
      e match {
        case UnaryOperation(op, ast)                 => s"${op.show} ${scopedShow(ast)}"
        case BinaryOperation(a, ast.`==`, NullValue) => s"${scopedShow(a)} IS NULL"
        case BinaryOperation(NullValue, ast.`==`, b) => s"${scopedShow(b)} IS NULL"
        case BinaryOperation(a, ast.`!=`, NullValue) => s"${scopedShow(a)} IS NOT NULL"
        case BinaryOperation(NullValue, ast.`!=`, b) => s"${scopedShow(b)} IS NOT NULL"
        case BinaryOperation(a, op, b)               => s"${scopedShow(a)} ${op.show} ${scopedShow(b)}"
        case Aggregation(op, Ident(_))               => s"${op.show}(*)"
        case Aggregation(op, ast)                    => s"${op.show}(${ast.show})"
      }
  }

  implicit val setOperationShow: Show[SetOperation] = new Show[SetOperation] {
    def show(e: SetOperation) =
      e match {
        case UnionOperation    => "UNION"
        case UnionAllOperation => "UNION ALL"
      }
  }

  protected def showOffsetWithoutLimit(offset: Ast) =
    s" OFFSET ${offset.show}"

  protected def showOrderBy(criterias: List[OrderByCriteria]) =
    s" ORDER BY ${criterias.show}"

  implicit val sourceShow: Show[Source] = new Show[Source] {
    def show(source: Source) =
      source match {
        case TableSource(name, alias)  => s"$name $alias"
        case QuerySource(query, alias) => s"(${query.show}) $alias"
        case InfixSource(infix, alias) => s"(${(infix: Ast).show}) $alias"
      }
  }

  implicit val orderByCriteriaShow: Show[OrderByCriteria] = new Show[OrderByCriteria] {
    def show(criteria: OrderByCriteria) =
      criteria match {
        case OrderByCriteria(prop, true)  => s"${prop.show} DESC"
        case OrderByCriteria(prop, false) => prop.show
      }
  }

  implicit val unaryOperatorShow: Show[UnaryOperator] = new Show[UnaryOperator] {
    def show(o: UnaryOperator) =
      o match {
        case ast.`!`        => "NOT"
        case ast.`isEmpty`  => "NOT EXISTS"
        case ast.`nonEmpty` => "EXISTS"
      }
  }

  implicit val aggregationOperatorShow: Show[AggregationOperator] = new Show[AggregationOperator] {
    def show(o: AggregationOperator) =
      o match {
        case ast.`min`  => "MIN"
        case ast.`max`  => "MAX"
        case ast.`avg`  => "AVG"
        case ast.`sum`  => "SUM"
        case ast.`size` => "COUNT"
      }
  }

  implicit val binaryOperatorShow: Show[BinaryOperator] = new Show[BinaryOperator] {
    def show(o: BinaryOperator) =
      o match {
        case ast.`-`  => "-"
        case ast.`+`  => "+"
        case ast.`*`  => "*"
        case ast.`==` => "="
        case ast.`!=` => "<>"
        case ast.`&&` => "AND"
        case ast.`||` => "OR"
        case ast.`>`  => ">"
        case ast.`>=` => ">="
        case ast.`<`  => "<"
        case ast.`<=` => "<="
        case ast.`/`  => "/"
        case ast.`%`  => "%"
      }
  }

  implicit def propertyShow(implicit valueShow: Show[Value], identShow: Show[Ident]): Show[Property] =
    new Show[Property] {
      def show(e: Property) =
        e match {
          case Property(ast, name) => s"${scopedShow(ast)}.$name"
        }
    }

  implicit val valueShow: Show[Value] = new Show[Value] {
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

  implicit val actionShow: Show[Action] = {

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

          case Insert(Entity(table), assignments) =>
            val columns = assignments.map(_.property)
            val values = assignments.map(_.value)
            s"INSERT INTO $table (${columns.mkString(",")}) VALUES (${values.show})"

          case Update(Entity(table), assignments) =>
            s"UPDATE $table SET ${set(assignments)}"

          case Update(Filter(Entity(table), x, where), assignments) =>
            s"UPDATE $table SET ${set(assignments)} WHERE ${where.show}"

          case Delete(Filter(Entity(table), x, where)) =>
            s"DELETE FROM $table WHERE ${where.show}"

          case Delete(Entity(table)) =>
            s"DELETE FROM $table"
        }
    }
  }

  private def scopedShow[A <: Ast](ast: A)(implicit show: Show[A]) =
    ast match {
      case _: Query           => s"(${ast.show})"
      case _: BinaryOperation => s"(${ast.show})"
      case other              => ast.show
    }

}
