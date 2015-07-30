package io.getquill.sql

import scala.reflect.ClassTag
import language.experimental.macros
import io.getquill.impl.Queryable

abstract class SqlSource[R: ClassTag] extends io.getquill.impl.Source[R] {

  def run[T](q: Queryable[T]): Any = macro SqlSourceMacro.run[R, T]
}
