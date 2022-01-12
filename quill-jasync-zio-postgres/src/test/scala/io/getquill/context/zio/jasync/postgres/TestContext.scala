package io.getquill.context.zio.jasync.postgres

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.context.zio.{ JAsyncContextConfig, PostgresJAsyncContext, PostgresJAsyncContextConfig, ZIOConnectedContext, ZIOJAsyncConnection }
import io.getquill.util.LoadConfig
import io.getquill.{ Literal, PostgresDialect, TestEntities }
import zio._

class TestContext extends PostgresJAsyncContext(Literal)
  with TestEntities
  with TestEncoders
  with TestDecoders {

  val config: JAsyncContextConfig[PostgreSQLConnection] = PostgresJAsyncContextConfig(LoadConfig("testPostgresDB"))

  val layer: TaskLayer[Has[ZIOJAsyncConnection]] =
    ZLayer.succeed(config) >>> ZIOJAsyncConnection.live[PostgreSQLConnection]

  val connected: TaskLayer[Has[ZIOConnectedContext[PostgresDialect, Literal.type, PostgreSQLConnection]]] =
    layer >>> ZIOConnectedContext.live(this)

}
