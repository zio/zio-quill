package io.getquill

import scala.reflect.ClassTag

import com.typesafe.config.ConfigFactory

abstract class Encoder[R: ClassTag, T: ClassTag] {
  def encode(value: T, index: Int, row: R): R
  def decode(index: Int, row: R): T
}

abstract class Source[R: ClassTag] {

  type Encoder[T] = io.getquill.Encoder[R, T]

  protected val config =
    ConfigFactory.load.getConfig(getClass.getSimpleName.replaceAllLiterally("$", ""))
}
