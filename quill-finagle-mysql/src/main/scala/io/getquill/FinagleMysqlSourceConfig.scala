package io.getquill

import com.twitter.finagle.client.DefaultPool
import com.twitter.finagle.exp.Mysql
import com.twitter.util.Try
import com.twitter.conversions.time._
import io.getquill.sources.SourceConfig
import io.getquill.naming.NamingStrategy
import java.util.TimeZone
import io.getquill.sources.finagle.mysql.FinagleMysqlSource

class FinagleMysqlSourceConfig[N <: NamingStrategy](val name: String) extends SourceConfig[FinagleMysqlSource[N]] {

  def dateTimezone = TimeZone.getDefault
  def user = config.getString("user")
  def password = Try(config.getString("password")).getOrElse(null)
  def database = config.getString("database")
  def dest = config.getString("dest")
  def lowWatermark = Try(config.getInt("pool.watermark.low")).getOrElse(0)
  def highWatermark = Try(config.getInt("pool.watermark.high")).getOrElse(10)
  def idleTime = Try(config.getInt("pool.idleTime")).getOrElse(5)
  def bufferSize = Try(config.getInt("pool.bufferSize")).getOrElse(0)
  def maxWaiters = Try(config.getInt("pool.maxWaiters")).getOrElse(Int.MaxValue)

  def client =
    Mysql.client
      .withCredentials(user, password)
      .withDatabase(database)
      .configured(DefaultPool.Param(
        low = lowWatermark, high = highWatermark,
        idleTime = idleTime.seconds,
        bufferSize = bufferSize,
        maxWaiters = maxWaiters))
      .newRichClient(dest)
}
