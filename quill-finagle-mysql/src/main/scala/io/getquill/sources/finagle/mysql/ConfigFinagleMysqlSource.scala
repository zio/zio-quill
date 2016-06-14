package io.getquill.sources.finagle.mysql

import com.twitter.util.Future
import io.getquill.FinagleMysqlSourceConfig
import io.getquill.naming.NamingStrategy

class ConfigFinagleMysqlSource[N <: NamingStrategy](config: FinagleMysqlSourceConfig[N])
  extends FinagleMysqlSource[N](config) {

  def client = config.client

  def transaction[T](f: FinagleMysqlSource[N] => Future[T]) =
    client.transaction { tx =>
      f(new TransactionalFinagleMysqlSource(config, tx))
    }
}
