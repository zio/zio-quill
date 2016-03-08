package io.getquill.sources.cassandra

import io.getquill.ast._
import io.getquill.naming.NamingStrategy
import io.getquill.norm.{ RenameAssignments, FlattenOptionOperation, Normalize, RenameProperties }
import io.getquill.util.Show.Shower

object Prepare {

  def apply(ast: Ast, params: List[Ident])(implicit n: NamingStrategy) = {
    import CqlIdiom._
    val (bindedAst, idents) = BindVariables(normalize(ast), params)
    (bindedAst.show, idents, None)
  }

  private[this] val normalize =
    (identity[Ast] _)
      .andThen(RenameProperties.apply _)
      .andThen(Normalize.apply _)
      .andThen(ExpandMappedInfix.apply _)
      .andThen(FlattenOptionOperation.apply _)
      .andThen(RenameAssignments.apply _)
}
