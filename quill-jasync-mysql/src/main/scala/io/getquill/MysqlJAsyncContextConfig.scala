package io.getquill

import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.mysql.util.URLParser
import com.typesafe.config.Config
import io.getquill.context.jasync.JAsyncContextConfig

case class MysqlJAsyncContextConfig(config: Config)
  extends JAsyncContextConfig[MySQLConnection](config, new MySQLConnectionFactory(_), URLParser.INSTANCE)
