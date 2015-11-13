package io.getquill.source.async.postgres

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import io.getquill.naming.NamingStrategy
import io.getquill.source.async.AsyncSource
import io.getquill.source.sql.idiom.PostgresDialect

class PostgresAsyncSource[N <: NamingStrategy] extends AsyncSource[PostgresDialect, N, PostgreSQLConnection] {

  override protected def objectFactory(config: Configuration) =
    new PostgreSQLConnectionFactory(config)
}
