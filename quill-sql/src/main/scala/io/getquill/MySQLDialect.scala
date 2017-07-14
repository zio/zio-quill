package io.getquill

import io.getquill.idiom.StatementInterpolator._
import io.getquill.ast._
import io.getquill.context.sql.idiom.OffsetWithoutLimitWorkaround
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.OrderByCriteria
import io.getquill.context.sql.idiom.QuestionMarkBindVariables

trait MySQLDialect
  extends SqlIdiom
  with OffsetWithoutLimitWorkaround
  with QuestionMarkBindVariables {

  override def prepareForProbing(string: String) = {
    val quoted = string.replace("'", "\\'")
    s"PREPARE p${quoted.hashCode.abs.toString.token} FROM '$quoted'"
  }

  override implicit def operationTokenizer(implicit propertyTokenizer: Tokenizer[Property], strategy: NamingStrategy): Tokenizer[Operation] =
    Tokenizer[Operation] {
      case BinaryOperation(a, StringOperator.`+`, b) => stmt"CONCAT(${a.token}, ${b.token})"
      case other                                     => super.operationTokenizer.token(other)
    }

  override implicit def orderByCriteriaTokenizer(implicit strategy: NamingStrategy): Tokenizer[OrderByCriteria] = Tokenizer[OrderByCriteria] {
    case OrderByCriteria(prop, AscNullsFirst | Asc)  => stmt"${prop.token} ASC"
    case OrderByCriteria(prop, DescNullsFirst)       => stmt"ISNULL(${prop.token}) DESC, ${prop.token} DESC"
    case OrderByCriteria(prop, AscNullsLast)         => stmt"ISNULL(${prop.token}) ASC, ${prop.token} ASC"
    case OrderByCriteria(prop, DescNullsLast | Desc) => stmt"${prop.token} DESC"
  }

  override implicit def actionTokenizer(implicit strategy: NamingStrategy): Tokenizer[Action] = {

    implicit def propertyTokenizer: Tokenizer[Property] = Tokenizer[Property] {
      case Property(Property(_, name), "isEmpty")   => stmt"${strategy.column(name).token} IS NULL"
      case Property(Property(_, name), "isDefined") => stmt"${strategy.column(name).token} IS NOT NULL"
      case Property(Property(_, name), "nonEmpty")  => stmt"${strategy.column(name).token} IS NOT NULL"
      case Property(_, name)                        => strategy.column(name).token
    }

    Tokenizer[Action] {
      case Upsert(table: Entity, assignments) =>
        val columns = assignments.map(_.property.token)
        val values = assignments.map(_.value)
        stmt"INSERT INTO ${table.token} (${columns.mkStmt()}) VALUES (${values.map(scopedTokenizer(_)).mkStmt()})"

      case Conflict(action, _, _)              => stmt"${action.token} ON DUPLICATE KEY UPDATE"

      case ConflictUpdate(action, assignments) => stmt"${action.token} ${assignments.mkStmt()}"

      case action                              => super.actionTokenizer.token(action)
    }
  }

}

object MySQLDialect extends MySQLDialect
