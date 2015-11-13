package io.getquill.source.async.mysql

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import io.getquill.naming.NamingStrategy
import io.getquill.source.async.AsyncSource
import io.getquill.source.sql.idiom.MySQLDialect

class MysqlAsyncSource[N <: NamingStrategy] extends AsyncSource[MySQLDialect.type, N, MySQLConnection] {

  override protected def objectFactory(config: Configuration) =
    new MySQLConnectionFactory(config)
}
