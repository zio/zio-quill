package io.getquill

import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import io.getquill.naming.NamingStrategy
import io.getquill.sources.async.AsyncSourceConfig
import io.getquill.sources.sql.idiom.PostgresDialect

class PostgresAsyncSourceConfig[N <: NamingStrategy](name: String)
  extends AsyncSourceConfig[PostgresDialect, N, PostgreSQLConnection](name, new PostgreSQLConnectionFactory(_))
