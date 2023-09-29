package io.getquill.context.qzio

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.jasync.sql.db.postgresql.util.URLParser
import com.typesafe.config.Config

case class PostgresJAsyncContextConfig(config: Config)
    extends JAsyncContextConfig[PostgreSQLConnection](config, new PostgreSQLConnectionFactory(_), URLParser.INSTANCE)
