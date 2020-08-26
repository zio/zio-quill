package io.getquill.sql.idiom

import io.getquill.NamingStrategy
import io.getquill.ast._
import io.getquill.context.sql.idiom.{ SqlIdiom, VerifySqlQuery }
import io.getquill.context.sql.norm.{ ExpandNestedQueries, SqlNormalize }
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.{ Statement, StringToken }
import io.getquill.quat.Quat
import io.getquill.sql.norm.{ RemoveExtraAlias, RemoveUnusedSelects, VendorizeBooleans }
import io.getquill.util.Messages
import io.getquill.util.Messages.{ fail, trace }

trait BooleanLiteralSupport extends SqlIdiom {

  override def translate(ast: Ast)(implicit naming: NamingStrategy): (Ast, Statement) = {
    val normalizedAst = VendorizeBooleans(SqlNormalize(ast))
    implicit val tokernizer = defaultTokenizer

    val token =
      normalizedAst match {
        case q: Query =>
          val sql = querifyAst(q)
          trace("sql")(sql)
          VerifySqlQuery(sql).map(fail)
          val expanded = ExpandNestedQueries(sql)
          trace("expanded sql")(expanded)
          val refined = if (Messages.pruneColumns) RemoveUnusedSelects(expanded) else expanded
          trace("filtered sql (only used selects)")(refined)
          val cleaned = if (!Messages.alwaysAlias) RemoveExtraAlias(naming)(refined) else refined
          trace("cleaned sql")(cleaned)
          val tokenized = cleaned.token
          trace("tokenized sql")(tokenized)
          tokenized
        case other =>
          other.token
      }

    (normalizedAst, stmt"$token")
  }

  override implicit def valueTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Value] =
    Tokenizer[Value] {
      case Constant(b: Boolean, Quat.BooleanValue) =>
        StringToken(if (b) "1" else "0")
      case Constant(b: Boolean, Quat.BooleanExpression) =>
        StringToken(if (b) "1 = 1" else "1 = 0")
      case other =>
        super.valueTokenizer.token(other)
    }
}
