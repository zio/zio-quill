package io.getquill.context.qzio.jasync.postgres

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.qzio.{
  JAsyncContextConfig,
  PostgresJAsyncContextConfig,
  PostgresZioJAsyncContext,
  ZioJAsyncConnection
}
import io.getquill.context.sql.{TestDecoders, TestEncoders}
import io.getquill.util.LoadConfig
import io.getquill.{Literal, TestEntities}
import zio._

class TestContext extends PostgresZioJAsyncContext(Literal) with TestEntities with TestEncoders with TestDecoders {

  val config: JAsyncContextConfig[PostgreSQLConnection] = PostgresJAsyncContextConfig(LoadConfig("testPostgresDB"))

  val layer: TaskLayer[ZioJAsyncConnection] =
    ZLayer.succeed(config) >>> ZioJAsyncConnection.live[PostgreSQLConnection]

}
