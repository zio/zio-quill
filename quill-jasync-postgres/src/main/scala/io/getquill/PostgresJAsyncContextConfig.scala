package io.getquill

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.jasync.sql.db.postgresql.util.URLParser
import com.typesafe.config.Config
import io.getquill.context.jasync.JAsyncContextConfig

case class PostgresJAsyncContextConfig(config: Config)
  extends JAsyncContextConfig[PostgreSQLConnection](config, new PostgreSQLConnectionFactory(_), URLParser.INSTANCE)
