package io.getquill.sql

import scala.reflect.ClassTag
import language.experimental.macros
import io.getquill.impl.Queryable
import io.getquill.impl.Quoted

abstract class SqlSource[R: ClassTag, S: ClassTag] extends io.getquill.impl.Source[R, S] {

  def query[T](q: Queryable[T]): Any = macro SqlQueryMacro.query[R, S, T]
  def query[P1, T](q: P1 => Queryable[T])(p1: P1): Any = macro SqlQueryMacro.query1[P1, R, S, T]
  def query[P1, P2, T](q: (P1, P2) => Queryable[T])(p1: P1, p2: P2): Any = macro SqlQueryMacro.query2[P1, P2, R, S, T]
  
  
  def insert[T](q: Quoted[Queryable[T]])(values: Iterable[T]): Any = macro SqlInsertMacro.insert[R, S, T]
  
  def delete[T](q: Queryable[T]): Any = macro SqlDeleteMacro.delete[R, S, T]
  def delete[P1, T](q: P1 => Queryable[T])(p1: P1): Any = macro SqlDeleteMacro.delete1[P1, R, S, T]
  def delete[P1, P2, T](q: (P1, P2) => Queryable[T])(p1: P1, p2: P2): Any = macro SqlDeleteMacro.delete2[P1, P2, R, S, T]
}
