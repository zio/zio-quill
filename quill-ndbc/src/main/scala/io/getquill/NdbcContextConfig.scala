package io.getquill

import java.util.Properties

import scala.util.control.NonFatal

import com.typesafe.config.Config

import io.trane.ndbc.DataSource

case class NdbcContextConfig(config: Config) {

  private def configProperties = {
    import scala.collection.JavaConverters._
    val p = new Properties
    for (entry <- config.entrySet.asScala)
      p.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
    p
  }

  def dataSource =
    try
      DataSource.fromProperties("ndbc", configProperties)
    catch {
      case NonFatal(ex) =>
        throw new IllegalStateException(s"Failed to load data source for config: '$config'", ex)
    }
}
