package io.getquill

import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.typesafe.config.Config

import io.getquill.context.async.AsyncContextConfig

case class PostgresAsyncContextConfig(config: Config)
  extends AsyncContextConfig[PostgreSQLConnection](config, new PostgreSQLConnectionFactory(_))
