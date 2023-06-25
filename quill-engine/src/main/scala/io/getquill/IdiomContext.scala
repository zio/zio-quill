package io.getquill

import io.getquill.ast.{Ast, CollectAst}
import io.getquill.norm.TranspileConfig
import io.getquill.ast

case class IdiomContext(config: TranspileConfig, queryType: IdiomContext.QueryType) {
  def traceConfig = config.traceConfig
}

object IdiomContext {
  def Empty = IdiomContext(TranspileConfig.Empty, QueryType.Insert)
  sealed trait QueryType {
    def isBatch: Boolean
    def batchAlias: Option[String]
  }
  object QueryType {
    case object Select extends Regular
    case object Insert extends Regular
    case object Update extends Regular
    case object Delete extends Regular

    case class BatchInsert(foreachAlias: String) extends Batch { val batchAlias = Some(foreachAlias) }
    case class BatchUpdate(foreachAlias: String) extends Batch { val batchAlias = Some(foreachAlias) }

    sealed trait Regular extends QueryType { val isBatch = false; val batchAlias = None }
    sealed trait Batch   extends QueryType { val isBatch = true                         }

    object Regular {
      def unapply(qt: QueryType): Boolean =
        qt match {
          case r: Regular => true
          case _          => false
        }
    }

    object Batch {
      def unapply(qt: QueryType) =
        qt match {
          case BatchInsert(foreachAlias) => Some(foreachAlias)
          case BatchUpdate(foreachAlias) => Some(foreachAlias)
          case _                         => None
        }
    }

    def discoverFromAst(theAst: Ast, batchAlias: Option[String]): QueryType = {
      val actions =
        CollectAst(theAst) {
          case _: ast.Insert => QueryType.Insert
          case _: ast.Update => QueryType.Update
          case _: ast.Delete => QueryType.Delete
        }
      if (actions.length > 1) println(s"[WARN] Found more then one type of Query: ${actions}. Using 1st one!")
      // if we have not found it to specifically be an action, it must just be a regular select query
      val resultType: QueryType.Regular = actions.headOption.getOrElse(QueryType.Select)
      resultType match {
        case QueryType.Insert => batchAlias.map(QueryType.BatchInsert(_)) getOrElse QueryType.Insert
        case QueryType.Update => batchAlias.map(QueryType.BatchUpdate(_)) getOrElse QueryType.Update
        case QueryType.Delete => Delete
        case QueryType.Select => Select
      }
    }
  }
}
