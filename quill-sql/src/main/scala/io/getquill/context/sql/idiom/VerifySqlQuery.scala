package io.getquill.context.sql.idiom

import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.quotation.FreeVariables
import io.getquill.context.sql.FlattenSqlQuery
import io.getquill.context.sql.FromContext
import io.getquill.context.sql.InfixContext
import io.getquill.context.sql.JoinContext
import io.getquill.context.sql.QueryContext
import io.getquill.context.sql.SetOperationSqlQuery
import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.TableContext
import io.getquill.context.sql.UnaryOperationSqlQuery
import io.getquill.context.sql.FlatJoinContext

case class Error(free: List[Ident], ast: Ast)
case class InvalidSqlQuery(errors: List[Error]) {
  override def toString =
    s"The monad composition can't be expressed using applicative joins. " +
      errors.map(error => s"Faulty expression: '${error.ast}'. Free variables: '${error.free}'.").mkString(", ")
}

object VerifySqlQuery {

  def apply(query: SqlQuery): Option[String] =
    verify(query).map(_.toString)

  private def verify(query: SqlQuery): Option[InvalidSqlQuery] =
    query match {
      case q: FlattenSqlQuery             => verify(q)
      case SetOperationSqlQuery(a, op, b) => verify(a).orElse(verify(b))
      case UnaryOperationSqlQuery(op, q)  => verify(q)
    }

  private def verify(query: FlattenSqlQuery): Option[InvalidSqlQuery] = {

    val aliases = query.from.map(this.aliases).flatten.map(Ident(_)) :+ Ident("*") :+ Ident("?")

    def verifyFreeVars(ast: Ast) =
      (FreeVariables(ast) -- aliases).toList match {
        case Nil  => None
        case free => Some(Error(free, ast))
      }

    val errors: List[Error] =
      query.where.flatMap(verifyFreeVars).toList ++
        query.orderBy.map(_.ast).flatMap(verifyFreeVars) ++
        query.limit.flatMap(verifyFreeVars) ++
        query.select.map(_.ast).map(verifyFreeVars).flatten

    val nestedErrors =
      query.from.collect {
        case QueryContext(query, alias) => verify(query).map(_.errors)
      }.flatten.flatten

    (errors ++ nestedErrors) match {

      case Nil    => None
      case errors => Some(InvalidSqlQuery(errors))
    }
  }

  private def aliases(s: FromContext): List[String] =
    s match {
      case s: TableContext    => List(s.alias)
      case s: QueryContext    => List(s.alias)
      case s: InfixContext    => List(s.alias)
      case s: JoinContext     => aliases(s.a) ++ aliases(s.b)
      case s: FlatJoinContext => aliases(s.a)
    }
}
