package io.getquill.source.finagle.mysql

import com.twitter.finagle.client.DefaultPool
import com.twitter.finagle.exp.Mysql
import com.twitter.util.Try
import com.twitter.conversions.time._
import com.typesafe.config.Config

object FinagleMysqlClient {

  def apply(config: Config) = {
    val user = config.getString("user")
    val password = Try(config.getString("password")).getOrElse(null)
    val database = config.getString("database")
    val dest = config.getString("dest")
    val lowWatermark = Try(config.getInt("pool.watermark.low")).getOrElse(0)
    val highWatermark = Try(config.getInt("pool.watermark.high")).getOrElse(10)
    val idleTime = Try(config.getInt("pool.idleTime")).getOrElse(5)
    val bufferSize = Try(config.getInt("pool.bufferSize")).getOrElse(0)
    val maxWaiters = Try(config.getInt("pool.maxWaiters")).getOrElse(Int.MaxValue)

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
}
