package io.getquill.sources.finagle.mysql

import io.getquill.naming.NamingStrategy
import io.getquill.FinagleMysqlSourceConfig
import java.util.TimeZone
import com.twitter.finagle.exp.mysql.Client
import com.twitter.util.Future
import com.typesafe.config.Config
import io.getquill.util.LoadConfig
import com.twitter.util.Await

class FinagleMysqlSource[N <: NamingStrategy](config: FinagleMysqlSourceConfig)
  extends FinagleMysqlSourceBase[N] {

  def this(config: Config) = this(FinagleMysqlSourceConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override protected val client = config.client
  override protected[mysql] def dateTimezone = config.dateTimezone

  Await.result(client.ping)

  def transaction[T](f: TransactionFinagleMysql[N] => Future[T]): Future[T] =
    client.transaction { t =>
      f(new TransactionFinagleMysql(t, dateTimezone))
    }

}

class TransactionFinagleMysql[N <: NamingStrategy](
  override protected val client:              Client,
  override protected[mysql] val dateTimezone: TimeZone
)
  extends FinagleMysqlSourceBase[N]
