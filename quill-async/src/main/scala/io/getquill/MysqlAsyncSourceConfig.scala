package io.getquill

import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import io.getquill.naming.NamingStrategy
import io.getquill.sources.SourceConfig
import io.getquill.sources.async.{ MysqlAsyncSource, AsyncSourceConfig }
import io.getquill.sources.sql.idiom.MySQLDialect

class MysqlAsyncSourceConfig[N <: NamingStrategy](name: String)
  extends AsyncSourceConfig[MySQLDialect, N, MySQLConnection](name, new MySQLConnectionFactory(_))
  with SourceConfig[MysqlAsyncSource[MySQLDialect, N, MySQLConnection]]
