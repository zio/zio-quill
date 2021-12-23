package io.getquill

import java.util.TimeZone

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Mysql
import com.twitter.finagle.client.DefaultPool
import com.twitter.util.Try
import com.typesafe.config.Config
import com.twitter.finagle.mysql.{ Client, Transactions }

final case class FinagleMysqlContextConfig(config: Config) {

  def injectionTimeZone: TimeZone = Try(TimeZone.getTimeZone(config.getString("timezone.injection"))).getOrElse(TimeZone.getDefault)
  def extractionTimeZone: TimeZone = Try(TimeZone.getTimeZone(config.getString("timezone.extraction"))).getOrElse(TimeZone.getDefault)
  def user: String = config.getString("user")
  def password: String = Try(config.getString("password")).getOrElse(null)
  def database: String = config.getString("database")
  def dest: String = config.getString("dest")
  def lowWatermark: Int = Try(config.getInt("pool.watermark.low")).getOrElse(0)
  def highWatermark: Int = Try(config.getInt("pool.watermark.high")).getOrElse(10)
  def idleTime: Long = Try(config.getLong("pool.idleTime")).getOrElse(5L)
  def bufferSize: Int = Try(config.getInt("pool.bufferSize")).getOrElse(0)
  def maxWaiters: Int = Try(config.getInt("pool.maxWaiters")).getOrElse(Int.MaxValue)
  def maxPrepareStatements: Int = Try(config.getInt("maxPrepareStatements")).getOrElse(20)
  def connectTimeout: Long = Try(config.getLong("connectTimeout")).getOrElse(1L)
  def noFailFast: Boolean = Try(config.getBoolean("noFailFast")).getOrElse(false)

  def client: Client with Transactions = {
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
