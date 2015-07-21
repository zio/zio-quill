package io.getquill

import scala.collection.JavaConversions._
import java.util.Properties

class Config(prefix: String) {

  lazy val properties = {
    val p = new Properties
    val configs =
      System.getProperties.collect {
        case (key, value) if (key.startsWith(prefix)) =>
          p.setProperty(key, value.drop(prefix.size))
      }
    p
  }

  def get(config: String) = Option(System.getProperty(s"$prefix.$config"))
  def apply(config: String) =
    get(config)
      .getOrElse(throw new IllegalArgumentException(s"Required property $config is not configured."))
}
