package io.getquill.context

import io.getquill.{ Action, BatchAction, NamingStrategy, Query, Quoted }
import io.getquill.idiom.Idiom

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.language.higherKinds

trait TranslateContext extends TranslateContextBase {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  override type TranslateResult[T] = T
  override def wrap[T](t: => T): T = t
  override def push[A, B](result: A)(f: A => B): B = f(result)
  override def seq[A](list: List[A]): List[A] = list
}

trait TranslateContextBase extends TranslateContextMacro {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  type TranslateResult[T]
  type Runner

  def wrap[T](t: => T): TranslateResult[T]
  def push[A, B](result: TranslateResult[A])(f: A => B): TranslateResult[B]
  def seq[A](list: List[TranslateResult[A]]): TranslateResult[List[A]]

  def translateQuery[T](statement: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor, prettyPrint: Boolean = false)(executionInfo: ExecutionInfo, dc: Runner): TranslateResult[String] =
    push(prepareParams(statement, prepare)) { params =>
      val query =
        if (params.nonEmpty) {
          params.foldLeft(statement) {
            case (expanded, param) => expanded.replaceFirst("\\?", param)
          }
        } else {
          statement
        }

      if (prettyPrint)
        idiom.format(query)
      else
        query
    }

  def translateBatchQuery(groups: List[BatchGroup], prettyPrint: Boolean = false)(executionInfo: ExecutionInfo, dc: Runner): TranslateResult[List[String]] =
    seq {
      groups.flatMap { group =>
        group.prepare.map { prepare =>
          translateQuery(group.string, prepare, prettyPrint = prettyPrint)(executionInfo, dc)
        }
      }
    }

  private[getquill] def prepareParams(statement: String, prepare: Prepare): TranslateResult[Seq[String]]

  @tailrec
  final protected def prepareParam(param: Any): String = param match {
    case None | null => "null"
    case Some(x)     => prepareParam(x)
    case str: String => s"'$str'"
    case _           => param.toString
  }
}

trait TranslateContextMacro {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  type TranslateResult[T]
  type Runner

  def translate[T](quoted: Quoted[T]): TranslateResult[String] = macro QueryMacro.translateQuery[T]
  def translate[T](quoted: Quoted[Query[T]]): TranslateResult[String] = macro QueryMacro.translateQuery[T]
  def translate(quoted: Quoted[Action[_]]): TranslateResult[String] = macro ActionMacro.translateQuery
  def translate(quoted: Quoted[BatchAction[Action[_]]]): TranslateResult[List[String]] = macro ActionMacro.translateBatchQuery

  def translate[T](quoted: Quoted[T], prettyPrint: Boolean): TranslateResult[String] = macro QueryMacro.translateQueryPrettyPrint[T]
  def translate[T](quoted: Quoted[Query[T]], prettyPrint: Boolean): TranslateResult[String] = macro QueryMacro.translateQueryPrettyPrint[T]
  def translate(quoted: Quoted[Action[_]], prettyPrint: Boolean): TranslateResult[String] = macro ActionMacro.translateQueryPrettyPrint
  def translate(quoted: Quoted[BatchAction[Action[_]]], prettyPrint: Boolean): TranslateResult[List[String]] = macro ActionMacro.translateBatchQueryPrettyPrint

  def translateQuery[T](statement: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor, prettyPrint: Boolean = false)(executionInfo: ExecutionInfo, dc: Runner): TranslateResult[String]
  def translateBatchQuery(groups: List[BatchGroup], prettyPrint: Boolean = false)(executionInfo: ExecutionInfo, dc: Runner): TranslateResult[List[String]]
}
