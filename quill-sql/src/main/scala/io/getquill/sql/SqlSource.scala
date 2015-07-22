package io.getquill.sql

import scala.reflect.ClassTag
import language.experimental.macros
import io.getquill.Queryable

abstract class SqlSource[R: ClassTag] extends io.getquill.Source[R] {

  def run[T](q: Queryable[T]): Any = macro SqlSourceMacro.run[R, T]

  def run[T](sql: String, extractor: R => T): List[T]
}
