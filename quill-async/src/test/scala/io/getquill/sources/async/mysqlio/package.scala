package io.getquill.sources.async

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.mysql.MySQLConnection
import io.getquill.naming.Literal
import io.getquill.sources.sql.idiom.MySQLDialect
import scala.concurrent._
import scala.concurrent.duration._

package object mysqlio {

  def connectionFactory(cfg: DefaultAsyncPoolConfig): Connection = {
    val conCfg = Configuration(
      host = cfg.host,
      username = cfg.user,
      database = Some(cfg.database),
      port = cfg.port
    )
    Await.result(new MySQLConnection(conCfg).connect, 10.seconds)
  }

  implicit val testAsyncPool = new DefaultAsyncPool(
    config = new DefaultAsyncPoolConfig(
    host = "127.0.0.1",
    port = 3306,
    user = "root",
    password = "",
    poolSize = 16,
    database = "quill_test",
    maxQueueSize = 1024,
    queryTimeout = 10.seconds,
    connectionFactory = connectionFactory
  )
  )

  val testMysqlIO = new MysqlAsyncIOSource[MySQLDialect, Literal, MySQLConnection](testAsyncPool)

}
