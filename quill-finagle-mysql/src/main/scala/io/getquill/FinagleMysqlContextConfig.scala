package io.getquill

import java.util.TimeZone

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Mysql
import com.twitter.finagle.client.DefaultPool
import com.twitter.util.Try
import com.typesafe.config.Config

case class FinagleMysqlContextConfig(config: Config) {

  def injectionTimeZone = Try(TimeZone.getTimeZone(config.getString("timezone.injection"))).getOrElse(TimeZone.getDefault)
  def extractionTimeZone = Try(TimeZone.getTimeZone(config.getString("timezone.extraction"))).getOrElse(TimeZone.getDefault)
  def user = config.getString("user")
  def password = Try(config.getString("password")).getOrElse(null)
  def database = config.getString("database")
  def dest = config.getString("dest")
  def lowWatermark = Try(config.getInt("pool.watermark.low")).getOrElse(0)
  def highWatermark = Try(config.getInt("pool.watermark.high")).getOrElse(10)
  def idleTime = Try(config.getLong("pool.idleTime")).getOrElse(5L)
  def bufferSize = Try(config.getInt("pool.bufferSize")).getOrElse(0)
  def maxWaiters = Try(config.getInt("pool.maxWaiters")).getOrElse(Int.MaxValue)
  def maxPrepareStatements = Try(config.getInt("maxPrepareStatements")).getOrElse(20)
  def connectTimeout = Try(config.getLong("connectTimeout")).getOrElse(1L)
  def noFailFast = Try(config.getBoolean("noFailFast")).getOrElse(false)

  def client = {
    var client = Mysql.client
      .withCredentials(user, password)
      .withDatabase(database)
      .withMaxConcurrentPrepareStatements(maxPrepareStatements)
      .withTransport
      .connectTimeout(connectTimeout.seconds)
      .configured(DefaultPool.Param(
        low = lowWatermark, high = highWatermark,
        idleTime = idleTime.seconds,
        bufferSize = bufferSize,
        maxWaiters = maxWaiters
      ))
    if (noFailFast) {
      client = client.withSessionQualifier.noFailFast
    }
    client.newRichClient(dest)
  }
}
