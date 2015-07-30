package io.getquill.impl

import scala.reflect.ClassTag
import com.typesafe.config.ConfigFactory

abstract class Decoder[R: ClassTag, T: ClassTag] {
  def apply(index: Int, row: R): T
}

abstract class Source[R: ClassTag] {

  type Decoder[T] = io.getquill.impl.Decoder[R, T]

  protected val config = ConfigFactory.load.getConfig(configPrefix)

  private def configPrefix = getClass.getSimpleName.replaceAllLiterally("$", "")
}
