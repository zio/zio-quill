package io.getquill.source.sql

import io.getquill.ast._
import io.getquill.quotation.FreeVariables
import scala.reflect.macros.whitebox.Context

object VerifySqlQuery {

  case class Error(free: List[Ident], ast: Ast)
  case class InvalidSqlQuery(errors: List[Error]) {
    override def toString =
      s"The monad composition can't be expressed using applicative joins. " +
        errors.map(error => s"Faulty expression: '${error.ast}'. Free variables: '${error.free}'.)").mkString(", ")
  }

  def apply(query: SqlQuery): Option[InvalidSqlQuery] = {

    val aliases = query.from.map(_.alias).map(Ident(_)) :+ Ident("*") :+ Ident("?")

    def verifyFreeVars(ast: Ast) =
      (FreeVariables(ast) -- aliases).toList match {
        case Nil  => None
        case free => Some(Error(free, ast))
      }

    val errors: List[Error] =
      query.where.flatMap(verifyFreeVars).toList ++
        query.orderBy.map(_.property).flatMap(verifyFreeVars) ++
        query.limit.flatMap(verifyFreeVars) ++
        verifyFreeVars(query.select)

    val nestedErrors =
      query.from.collect {
        case QuerySource(query, alias) => apply(query).map(_.errors)
      }.flatten.flatten

    (errors ++ nestedErrors) match {
      case Nil    => None
      case errors => Some(InvalidSqlQuery(errors))
    }
  }
}
