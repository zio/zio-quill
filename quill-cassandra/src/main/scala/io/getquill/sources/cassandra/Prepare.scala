package io.getquill.sources.cassandra

import io.getquill.ast._
import io.getquill.naming.NamingStrategy
import io.getquill.norm.FlattenOptionOperation
import io.getquill.norm.Normalize
import io.getquill.norm.RenameProperties
import io.getquill.util.Show.Shower

object Prepare {

  def apply(ast: Ast, params: List[Ident])(implicit n: NamingStrategy) = {
    import CqlIdiom._
    val (bindedAst, idents) = BindVariables(normalize(ast), params)
    (bindedAst.show, idents)
  }

  private[this] val normalize =
    (identity[Ast] _)
      .andThen(RenameProperties.apply _)
      .andThen(Normalize.apply _)
      .andThen(ExpandMappedInfix.apply _)
      .andThen(FlattenOptionOperation.apply _)
}
