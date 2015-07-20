package io.getquill

import language.experimental.macros
import scala.reflect.ClassTag
import io.getquill.ast.Ident
import io.getquill.ast.Property

abstract class Encoder[R: ClassTag, T: ClassTag] {
  def encode(value: T, index: Int, row: R): R
  def decode(index: Int, row: R): T
}

abstract class Source[R: ClassTag] {

  type Encoder[T] = io.getquill.Encoder[R, T]

  protected def entity[T]: Any = macro SourceMacro.entity[T, R]

  def run[T](q: Queryable[T]): Any = ???
}
