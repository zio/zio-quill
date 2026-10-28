package zio.quill.sql.idiom

import zio.quill.NamingStrategy
import zio.quill.ast._
import zio.quill.context.sql.idiom.SqlIdiom
import zio.quill.context.sql.norm.SqlNormalize
import zio.quill.idiom.StatementInterpolator._
import zio.quill.idiom.StringToken
import zio.quill.norm.{ConcatBehavior, EqualityBehavior}
import zio.quill.quat.Quat
import zio.quill.sql.norm.VendorizeBooleans
import zio.quill.util.Messages
import zio.quill.IdiomContext

trait BooleanLiteralSupport extends SqlIdiom {

  override def normalizeAst(
    ast: Ast,
    concatBehavior: ConcatBehavior,
    equalityBehavior: EqualityBehavior,
    idiomContext: IdiomContext
  ) = {
    val norm = SqlNormalize(ast, idiomContext.config, concatBehavior, equalityBehavior)
    if (Messages.smartBooleans)
      VendorizeBooleans(norm)
    else
      norm
  }

  override implicit def valueTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy
  ): Tokenizer[Value] =
    Tokenizer[Value] {
      case Constant(b: Boolean, Quat.BooleanValue) =>
        StringToken(if (b) "1" else "0")
      case Constant(b: Boolean, Quat.BooleanExpression) =>
        StringToken(if (b) "1 = 1" else "1 = 0")
      case other =>
        super.valueTokenizer.token(other)
    }
}
