package io.getquill.source.jdbc

import io.getquill.naming.Literal
import io.getquill.source.sql.idiom.MySQLDialect

object testDB extends JdbcSource[MySQLDialect.type, Literal]
