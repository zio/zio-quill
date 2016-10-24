package io.getquill

import io.getquill.context.Context
import scala.language.experimental.macros
import io.getquill.context.CompositeContextMacro
import io.getquill.idiom.Idiom
import io.getquill.dsl.CoreDsl

trait CompositeContext {
  this: Context[_, _] =>

  def run[T](quoted: Quoted[T]): Result[RunQuerySingleResult[T]] =  macro CompositeContextMacro.run
  def run[T](quoted: Quoted[Query[T]]): Result[RunQueryResult[T]] = macro CompositeContextMacro.run
  def run(quoted: Quoted[Action[_]]): Result[RunActionResult] = macro CompositeContextMacro.run
  def run[T](quoted: Quoted[ActionReturning[_, T]]): Result[RunActionReturningResult[T]] = macro CompositeContextMacro.run
  def run(quoted: Quoted[BatchAction[Action[_]]]): Result[RunBatchActionResult] = macro CompositeContextMacro.run
  def run[T](quoted: Quoted[BatchAction[ActionReturning[_, T]]]): Result[RunBatchActionReturningResult[T]] = macro CompositeContextMacro.run
}

object CompositeContext {

  case class Element[T <: Context[_, _]](cond: () => Boolean, ctx: T)

  def when[T <: Context[_, _]](cond: => Boolean)(ctx: T): Element[T] = Element(() => cond, ctx)

  def apply[T](elements: Element[T]*): CoreDsl = macro context.CompositeContextMacro.apply
}
