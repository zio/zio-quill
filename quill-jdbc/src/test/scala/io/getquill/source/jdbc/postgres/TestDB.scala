package io.getquill.source.jdbc.postgres

import io.getquill.source.jdbc.JdbcSource
import io.getquill.naming.Literal
import io.getquill.source.sql.idiom.PostgresDialect

object testPostgresDB extends JdbcSource[PostgresDialect, Literal]
