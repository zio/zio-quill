package io.getquill.sql.idiom

import io.getquill.NamingStrategy
import io.getquill.ast._
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.norm.SqlNormalize
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.StringToken
import io.getquill.norm.{ ConcatBehavior, EqualityBehavior }
import io.getquill.quat.Quat
import io.getquill.sql.norm.VendorizeBooleans
import io.getquill.util.Messages

trait BooleanLiteralSupport extends SqlIdiom {

  override def normalizeAst(ast: Ast, concatBehavior: ConcatBehavior, equalityBehavior: EqualityBehavior) = {
    val norm = SqlNormalize(ast, concatBehavior, equalityBehavior)
    if (Messages.smartBooleans)
      VendorizeBooleans(norm)
    else
      norm
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
