package io.getquill.sources.finagle.mysql

import com.twitter.finagle.exp.mysql.Client
import com.twitter.util.Future
import io.getquill.FinagleMysqlSourceConfig
import io.getquill.naming.NamingStrategy

class TransactionalFinagleMysqlSource[N <: NamingStrategy](config: FinagleMysqlSourceConfig[N], txClient: Client)
  extends FinagleMysqlSource[N](config) {

  def client = txClient

  def transaction[T](f: FinagleMysqlSource[N] => Future[T]) = f(this)
}
