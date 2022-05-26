package io.getquill.context.jdbc

import io.getquill._
import io.getquill.context.ContextVerbPrepareLambda
import io.getquill.context.sql.idiom.SqlIdiom

import java.sql._

trait JdbcContextBase[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends JdbcContextVerbExecute[Dialect, Naming]
  with JdbcContextVerbPrepare[Dialect, Naming]
  with ContextVerbPrepareLambda {

  // Need to re-define these here or they conflict with staged-prepare imported types
  override type PrepareQueryResult = Connection => Result[PreparedStatement]
  override type PrepareActionResult = Connection => Result[PreparedStatement]
  override type PrepareBatchActionResult = Connection => Result[List[PreparedStatement]]

  def constructPrepareQuery(f: Connection => Result[PreparedStatement]): Connection => Result[PreparedStatement] = f
  def constructPrepareAction(f: Connection => Result[PreparedStatement]): Connection => Result[PreparedStatement] = f
  def constructPrepareBatchAction(f: Connection => Result[List[PreparedStatement]]): Connection => Result[List[PreparedStatement]] = f
}
