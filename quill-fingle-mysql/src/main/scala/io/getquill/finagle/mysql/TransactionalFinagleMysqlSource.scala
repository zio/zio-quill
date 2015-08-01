package io.getquill.finagle.mysql

import com.twitter.finagle.exp.mysql.Client
import com.twitter.finagle.exp.mysql.Transactions
import com.twitter.util.Future

trait TransactionalFinagleMysqlSource extends FinagleMysqlSource {

  override protected val client: Client with Transactions = FinagleMysqlClient(config)

  def transaction[T](f: FinagleMysqlSource => Future[T]) =
    client.transaction { transactional =>
      val transactionalSource = new FinagleMysqlSource {
        override protected val client = transactional
      }
      f(transactionalSource)
    }
}
