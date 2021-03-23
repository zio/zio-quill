package io.getquill.context.jasync.qzio.postgres

import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.PostgresJAsyncZioContext.BuildConnection
import io.getquill.util.LoadConfig
import io.getquill.{ PostgresJAsyncContextConfig, Spec }
import zio.{ Has, ZIO, ZLayer }

trait ZioSpec extends Spec {
  import io.getquill.context.jasync.JAsyncZioContext._

  var pool: ConnectionPool[PostgreSQLConnection] = _

  override protected def beforeAll = {
    pool = PostgresJAsyncContextConfig(LoadConfig("testPostgresDB")).pool
  }

  override protected def afterAll = {
    zio.Runtime.default.unsafeRun(pool.disconnect().toZio)
    ()
  }

  def connectionLayer =
    ZLayer.succeed(pool) >>> BuildConnection.fromPool

  def await[T](f: ZIO[Has[PostgreSQLConnection], Throwable, T]): T = zio.Runtime.default.unsafeRun(f.provideSomeLayer(connectionLayer))
}
