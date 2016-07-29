package io.getquill

import io.getquill.idiom.StatementInterpolator._
import io.getquill.ast.Asc
import io.getquill.ast.AscNullsFirst
import io.getquill.ast.AscNullsLast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Desc
import io.getquill.ast.DescNullsFirst
import io.getquill.ast.DescNullsLast
import io.getquill.ast.Operation
import io.getquill.ast.Property
import io.getquill.ast.StringOperator
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
}

object MySQLDialect extends MySQLDialect
