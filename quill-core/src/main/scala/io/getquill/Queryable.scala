package io.getquill

import io.getquill.ast.Query
import io.getquill.ast.QueryShow.queryShow
import io.getquill.util.Show.Shower
import language.experimental.macros
import io.getquill.attach.Attachable

trait Queryable[+T] extends Attachable {

  def map[R](f: T => R): Queryable[R] = macro QueryableMacro.map[T, R]
  def flatMap[R](f: T => Queryable[R]): Queryable[R] = macro QueryableMacro.flatMap[T, R]

  def withFilter(f: T => Any): Queryable[T] = macro QueryableMacro.filter[T]
  def withFilter(f: Partial1[T, Boolean]): Queryable[T] = macro QueryableMacro.filterPartial[T]

  def filter(f: T => Any): Queryable[T] = macro QueryableMacro.filter[T]
  def filter(f: Partial1[T, Boolean]): Queryable[T] = macro QueryableMacro.filterPartial[T]

  def run: Any = macro QueryableMacro.run[T]

  def data: Source[_]
  def metadata: Query

  override def toString = {
    import ast.QueryShow._
    metadata.show
  }
}