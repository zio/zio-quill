package io.getquill.sql

import scala.reflect.ClassTag
import language.experimental.macros
import io.getquill.impl.Queryable

abstract class SqlSource[R: ClassTag, S: ClassTag] extends io.getquill.impl.Source[R, S] {

  def query[T](q: Queryable[T]): Any = macro SqlSourceMacro.query[R, S, T]
  def query[P1, T](q: P1 => Queryable[T])(p1: P1): Any = macro SqlSourceMacro.query1[P1, R, S, T]
  def query[P1, P2, T](q: (P1, P2) => Queryable[T])(p1: P1, p2: P2): Any = macro SqlSourceMacro.query2[P1, P2, R, S, T]
}
