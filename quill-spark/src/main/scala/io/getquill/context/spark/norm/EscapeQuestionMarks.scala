package io.getquill.context.spark.norm

import io.getquill.ast._
import QuestionMarkEscaper._

object EscapeQuestionMarks extends StatelessTransformer {

  override def apply(ast: Ast): Ast =
    ast match {
      case Constant(value, _) =>
        Constant.auto(if (value.isInstanceOf[String]) escape(value.asInstanceOf[String]) else value)
      case Infix(parts, params, pure, quat) =>
        Infix(parts.map(escape(_)), params, pure, quat)
      case other =>
        super.apply(other)
    }
}
