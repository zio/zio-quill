package io.getquill

import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import com.typesafe.config.Config

import io.getquill.context.async.AsyncContextConfig

case class MysqlAsyncContextConfig(config: Config)
  extends AsyncContextConfig[MySQLConnection](config, new MySQLConnectionFactory(_))
