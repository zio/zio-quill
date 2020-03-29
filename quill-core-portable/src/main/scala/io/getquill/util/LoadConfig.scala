package io.getquill.util

import com.typesafe.config.ConfigFactory

object LoadConfig {

  def apply(configPrefix: String) = {
    val factory = ConfigFactory.load(getClass.getClassLoader)
    if (factory.hasPath(configPrefix))
      factory.getConfig(configPrefix)
    else
      ConfigFactory.empty
  }
}
