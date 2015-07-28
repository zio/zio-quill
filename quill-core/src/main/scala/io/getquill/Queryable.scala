package io.getquill

import io.getquill.ast.Query
import io.getquill.ast.QueryShow.queryShow
import io.getquill.util.Show.Shower
import language.experimental.macros

trait Queryable[+T] {

  def map[R](f: T => R): Queryable[R] = ???
  def flatMap[R](f: T => Queryable[R]): Queryable[R] = ???

  def withFilter(f: T => Any): Queryable[T] = ???

  def filter(f: T => Any): Queryable[T] = ???
}
