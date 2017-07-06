package io.getquill

import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import com.github.mauricio.async.db.mysql.util.URLParser
import com.typesafe.config.Config
import io.getquill.context.async.AsyncContextConfig

case class MysqlAsyncContextConfig(config: Config)
  extends AsyncContextConfig[MySQLConnection](config, new MySQLConnectionFactory(_), URLParser)
