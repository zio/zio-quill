package io.getquill.context.zio.jasync.postgres

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.context.zio.{ JAsyncContextConfig, PostgresZioJAsyncContext, PostgresJAsyncContextConfig, ZioJAsyncConnection }
import io.getquill.util.LoadConfig
import io.getquill.{ Literal, PostgresDialect, TestEntities }
import zio._

class TestContext extends PostgresZioJAsyncContext(Literal)
  with TestEntities
  with TestEncoders
  with TestDecoders {

  val config: JAsyncContextConfig[PostgreSQLConnection] = PostgresJAsyncContextConfig(LoadConfig("testPostgresDB"))

  val layer: TaskLayer[ZioJAsyncConnection] =
    ZLayer.succeed(config) >>> ZioJAsyncConnection.live[PostgreSQLConnection]

}
