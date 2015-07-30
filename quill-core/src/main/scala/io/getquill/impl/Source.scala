package io.getquill.impl

import scala.reflect.ClassTag
import com.typesafe.config.ConfigFactory

abstract class Decoder[R: ClassTag, T: ClassTag] {
  def apply(index: Int, row: R): T
}

abstract class Encoder[S: ClassTag, T: ClassTag] {
  def apply(index: Int, value: T, row: S): S
}

abstract class Source[R: ClassTag, S: ClassTag] {

  type Decoder[T] = io.getquill.impl.Decoder[R, T]
  type Encoder[T] = io.getquill.impl.Encoder[S, T]

  protected val config = ConfigFactory.load.getConfig(configPrefix)

  private def configPrefix = getClass.getSimpleName.replaceAllLiterally("$", "")
}
