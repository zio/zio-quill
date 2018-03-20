package io.getquill.context

import io.getquill.NamingStrategy
import io.getquill.idiom.Idiom
import scala.annotation.tailrec
import scala.language.experimental.macros

trait TranslateContext {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  def translate[T](quoted: Quoted[T]): String = macro QueryMacro.translateQuery[T]
  def translate[T](quoted: Quoted[Query[T]]): String = macro QueryMacro.translateQuery[T]
  def translate(quoted: Quoted[Action[_]]): String = macro ActionMacro.translateQuery
  def translate(quoted: Quoted[BatchAction[Action[_]]]): List[String] = macro ActionMacro.translateBatchQuery

  def translateQuery[T](statement: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): String = {
    val params = prepareParams(statement, prepare)
    if (params.nonEmpty) {
      params.foldLeft(statement) {
        case (expanded, param) => expanded.replaceFirst("\\?", param)
      }
    } else {
      statement
    }
  }

  def translateBatchQuery(groups: List[BatchGroup]): List[String] =
    groups.flatMap { group =>
      group.prepare.map { prepare =>
        translateQuery(group.string, prepare)
      }
    }

  protected def prepareParams(statement: String, prepare: Prepare): Seq[String]

  @tailrec
  final protected def prepareParam(param: Any): String = param match {
    case None | null => "null"
    case Some(x)     => prepareParam(x)
    case str: String => s"'$str'"
    case _           => param.toString
  }
}
