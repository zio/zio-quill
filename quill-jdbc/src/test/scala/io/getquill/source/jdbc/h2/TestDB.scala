package io.getquill.source.jdbc.h2

import io.getquill.source.jdbc.JdbcSource
import io.getquill.naming.Literal
import io.getquill.source.sql.idiom.H2Dialect

object testH2DB extends JdbcSource[H2Dialect, Literal]
