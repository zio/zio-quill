package io.getquill.source

import scala.reflect.ClassTag
import scala.util.DynamicVariable

import com.typesafe.config.ConfigFactory

abstract class Source[R: ClassTag, S: ClassTag] {

  type Decoder[T] = io.getquill.source.Decoder[R, T]
  type Encoder[T] = io.getquill.source.Encoder[S, T]

  private val configPrefix =
    Source.configPrefix.value.getOrElse {
      getClass.getSimpleName.replaceAllLiterally("$", "")
    }

  protected val config = {
    val factory = ConfigFactory.load(getClass.getClassLoader)
    if (factory.hasPath(configPrefix))
      factory.getConfig(configPrefix)
    else
      ConfigFactory.empty
  }
}

object Source {

  private[getquill] val configPrefix = new DynamicVariable[Option[String]](None)
}
