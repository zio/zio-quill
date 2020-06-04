package io.getquill.norm

import io.getquill.ast.{ Ast, Query, StatelessTransformer }
import io.getquill.quat.Quat
import io.getquill.util.Messages

case class CheckQuats(header: String) extends StatelessTransformer {

  override def apply(query: Query) =
    check(query)(super.apply(_))

  override def apply(ast: Ast): Ast =
    check(ast)(super.apply(_))

  protected def check[T <: Ast](ast: T)(continue: T => T): T = {
    ast.quat match {
      case Quat.Error(msg, _) =>
        throw new IllegalStateException(s"Failed Phase ${header}: ${msg}\n" + Messages.qprint(ast) + "\n")
      case _ =>
        continue(ast) //super.apply(ast)
    }
  }
}
