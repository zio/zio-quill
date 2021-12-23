package io.getquill.util

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

object LoadConfig {

  def apply(configPrefix: String): Config = {
    val factory = ConfigFactory.load(getClass.getClassLoader)
    if (factory.hasPath(configPrefix))
      factory.getConfig(configPrefix)
    else
      ConfigFactory.empty
  }
}
