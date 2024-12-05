package io.getquill.context

import io.getquill.ast.ScalarLift
import io.getquill.{Action, BatchAction, NamingStrategy, Query, Quoted}
import io.getquill.idiom.Idiom

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.language.higherKinds

trait ContextVerbTranslate extends ContextTranslateMacro {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  override type TranslateResult[T] = T
  override def wrap[T](t: => T): T                 = t
  override def push[A, B](result: A)(f: A => B): B = f(result)
  override def seq[A](list: List[A]): List[A]      = list
}

case class TranslateOptions(
  prettyPrint: Boolean = false,
  plugLifts: Boolean = true,
  demarcateLifts: Boolean = false
)

trait ContextTranslateMacro extends ContextTranslateProto {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  def translate[T](quoted: Quoted[T]): String = macro QueryMacro.translateQuery[T]
  def translate[T](quoted: Quoted[Query[T]]): String = macro QueryMacro.translateQuery[T]
  def translate(quoted: Quoted[Action[_]]): String = macro ActionMacro.translateQuery
  def translate(quoted: Quoted[BatchAction[Action[_]]]): List[String] =
    macro ActionMacro.translateBatchQuery

  def translate[T](quoted: Quoted[T], options: TranslateOptions): TranslateResult[String] =
    macro QueryMacro.translateQueryPrettyPrint[T]
  def translate[T](quoted: Quoted[Query[T]], options: TranslateOptions): TranslateResult[String] =
    macro QueryMacro.translateQueryPrettyPrint[T]
  def translate(quoted: Quoted[Action[_]], options: TranslateOptions): TranslateResult[String] =
    macro ActionMacro.translateQueryPrettyPrint
  def translate(quoted: Quoted[BatchAction[Action[_]]], options: TranslateOptions): TranslateResult[List[String]] =
    macro ActionMacro.translateBatchQueryPrettyPrint

  def translateQuery[T](
    statement: String,
    lifts: List[ScalarLift] = List(),
    options: TranslateOptions
  )(executionInfo: ExecutionInfo, dc: Runner): String

  def translateBatchQuery(groups: List[BatchGroup], options: TranslateOptions = TranslateOptions())(
    executionInfo: ExecutionInfo,
    dc: Runner
  ): List[String]
}

trait ContextTranslateProto {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  type TranslateResult[T]
  type Runner

  def wrap[T](t: => T): TranslateResult[T]
  def push[A, B](result: TranslateResult[A])(f: A => B): TranslateResult[B]
  def seq[A](list: List[TranslateResult[A]]): TranslateResult[List[A]]

  def translateQuery[T](
    statement: String,
    liftings: List[ScalarLift] = List(),
    options: TranslateOptions = TranslateOptions()
  )(executionInfo: ExecutionInfo, dc: Runner): String =
    (liftings.nonEmpty, options.plugLifts) match {
      case (true, true) =>
        liftings.foldLeft(statement) { case (expanded, lift) =>
          expanded.replaceFirst("\\?", if (options.demarcateLifts) s"prep(${lift.value})" else s"${lift.value}")
        }
      case (true, false) =>
        var varNum: Int = 0
        val dol         = '$'
        val numberedQuery =
          liftings.foldLeft(statement) { case (expanded, lift) =>
            val res = expanded.replaceFirst("\\?", s"${dol}${varNum}")
            varNum += 1
            res
          }
        numberedQuery + "\n" + liftings.map(lift => s"${dol} = ${lift.value}").mkString("\n")
      case _ =>
        statement
    }

  def translateBatchQuery(
    // TODO these groups need to have liftings lists
    groups: List[BatchGroup],
    options: TranslateOptions = TranslateOptions()
  )(executionInfo: ExecutionInfo, dc: Runner): List[String] =
    groups.flatMap { group =>
      group.prepare.map { _ =>
        translateQuery(group.string, options = options)(executionInfo, dc)
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
