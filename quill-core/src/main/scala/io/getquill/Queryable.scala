package io.getquill

import language.experimental.macros
import io.getquill.ast.Query
import io.getquill.attach.Attachable

trait Queryable[+T] extends Attachable[Query] {

  def map[R](f: T => R): Queryable[R] = macro QueryableMacro.map[T, R]
  def flatMap[R](f: T => Queryable[R]): Queryable[R] = macro QueryableMacro.flatMap[T, R]

  def withFilter(f: T => Any): Queryable[T] = macro QueryableMacro.filter[T]
  def filter(f: T => Any): Queryable[T] = macro QueryableMacro.filter[T]

  override def toString = {
    import util.Show._
    import ast.QueryShow._
    attachment.show
  }
}

object Queryable {
  def apply[T]: Queryable[T] = macro QueryableMacro.apply[T]
}