package zio.quill.norm

import zio.quill.ast._

object NormalizeStringConcat extends StatelessTransformer {
  override def apply(ast: Ast): Ast = ast match {
    case BinaryOperation(Constant("", _), StringOperator.`+`, b) => apply(b)
    case _                                                       => super.apply(ast)
  }
}
