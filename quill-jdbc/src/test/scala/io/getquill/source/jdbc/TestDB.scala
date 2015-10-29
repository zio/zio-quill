package io.getquill.source.jdbc

import io.getquill.source.sql.idiom.MySQLDialect
import io.getquill.source.sql.naming.Literal

object testDB extends JdbcSource[MySQLDialect.type, Literal]
