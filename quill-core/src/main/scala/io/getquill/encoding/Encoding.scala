package io.getquill.encoding

import scala.reflect.ClassTag

trait Decoder[R, T] {
  def apply(index: Int, row: R): T
}

trait Encoder[S, T] {
  def apply(index: Int, value: T, row: S): S
}
