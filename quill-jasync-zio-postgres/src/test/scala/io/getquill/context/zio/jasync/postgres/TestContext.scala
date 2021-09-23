package io.getquill.context.zio.jasync.postgres

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.context.zio.{ JAsyncContextConfig, PostgresJAsyncContext, PostgresJAsyncContextConfig, ZConnection }
import io.getquill.util.LoadConfig
import io.getquill.{ Literal, TestEntities }
import zio.{ Has, TaskLayer, ULayer, ZLayer }

class TestContext extends PostgresJAsyncContext(Literal)
  with TestEntities
  with TestEncoders
  with TestDecoders {

  val config: JAsyncContextConfig[PostgreSQLConnection] = PostgresJAsyncContextConfig(LoadConfig("testPostgresDB"))

  val layer: TaskLayer[Has[ZConnection.Service]] =
    ZLayer.succeed(config) >>> ZConnection.live[PostgreSQLConnection]

}
