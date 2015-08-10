package io.getquill.source

import scala.reflect.ClassTag
import com.typesafe.config.ConfigFactory


abstract class Source[R: ClassTag, S: ClassTag] {

  type Decoder[T] = io.getquill.source.Decoder[R, T]
  type Encoder[T] = io.getquill.source.Encoder[S, T]

  protected lazy val config = ConfigFactory.load.getConfig(configPrefix)

  private def configPrefix = getClass.getSimpleName.replaceAllLiterally("$", "")
}
