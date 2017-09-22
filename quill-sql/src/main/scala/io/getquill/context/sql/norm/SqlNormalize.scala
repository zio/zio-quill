package io.getquill.context.sql.norm

import io.getquill.norm.FlattenOptionOperation
import io.getquill.norm.Normalize
import io.getquill.ast.Ast
import io.getquill.norm.RenameProperties

object SqlNormalize {

  private val debugEnabled = false

  private val normalize =
    (identity[Ast] _)
      .andThen(debug("original"))
      .andThen(FlattenOptionOperation.apply _)
      .andThen(debug("FlattenOptionOperation"))
      .andThen(Normalize.apply _)
      .andThen(debug("Normalize"))
      .andThen(RenameProperties.apply _)
      .andThen(debug("RenameProperties"))
      .andThen(ExpandJoin.apply _)
      .andThen(debug("ExpandJoin"))
      .andThen(Normalize.apply _)
      .andThen(debug("Normalize"))

  def debug(name: String)(ast: Ast) = {
    if (debugEnabled) println(s"$name:\n 		$ast")
    ast
  }

  def apply(ast: Ast) = normalize(ast)
}
